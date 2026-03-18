# Terminal Text Buffer

A pure Java 17 implementation of `TerminalBuffer` — the core data structure used by terminal
emulators to store and manipulate displayed text.

## Build & Run

```bash
gradle wrapper --gradle-version 8.5
./gradlew test
```

Requires Java 17+. No external libraries except JUnit 5 (test scope only).

## Architecture

### Data Model

```
TextColor (enum)       — DEFAULT + 16 standard ANSI colors
StyleFlags (class)     — bold, italic, underline
TextStyle (class)      — fg + bg + StyleFlags; immutable, equals/hashCode
TerminalCell (class)   — char + style + isWidePlaceholder; immutable
TerminalLine (class)   — TerminalCell[] of fixed width; mutable cells
TerminalBuffer (class) — screen + scrollback + cursor state
```

All model classes are `final` with proper `equals`/`hashCode`. `TerminalCell` and `TextStyle`
are immutable — every write creates a new instance. This makes state reasoning easier and
avoids aliasing bugs at the cost of minor allocation overhead (acceptable for a buffer of this
size).

### Screen & Scrollback

`screen` and `scrollback` are both `ArrayDeque<TerminalLine>`:

- `screen`: index 0 = top row, index `height-1` = bottom row
- `scrollback`: index 0 = **oldest** line (industry convention: xterm.js, WezTerm, alacritty)

`ArrayDeque` was chosen over `LinkedList` for cache locality and amortized O(1) on both ends.
`screenLine(row)` iterates the deque — O(height), which is fine for typical terminal sizes
(24–50 rows).

### Cursor & LCF (Deferred Wrap)

The implementation follows the **Last Column Flag (LCF)** algorithm used by xterm, kitty,
WezTerm, and other modern terminals:

> When a character is written to the rightmost column with autowrap on, it is drawn normally
> but the cursor **does not advance**. Instead, `wrapPending = true` is set. The actual wrap
> (cursor to col 0, row+1) is deferred until the next printable character is written.

This differs from "immediate wrap" and is critical for correct display of TUI applications
(vim, less, shell prompts). Failing to defer the wrap causes double-printing bugs at line ends.

`wrapPending` is reset by: any explicit cursor movement (`setCursor`, `moveCursor`),
`clearScreen`, and `clearScreenAndScrollback`.

`autoWrap` is configurable (default `true`). With `autoWrap=false`: cursor stops at last
column, subsequent characters overwrite it, `wrapPending` is never set.

### Wide Character Support

Wide characters (CJK ideographs, some emoji) occupy 2 terminal columns. The implementation
uses an **anchor/placeholder model**:

- Leading cell: stores the actual character, `isWidePlaceholder=false`
- Trailing cell: `ch=null`, `isWidePlaceholder=true`

**Invariant**: no placeholder cell exists without an anchor directly to its left.

This invariant is maintained by `clearWideCharAt()`, called before every write. It handles:
- Overwriting the anchor → placeholder cleared atomically
- Overwriting the placeholder → anchor cleared atomically

Cursor movement `LEFT` into a placeholder automatically jumps to the anchor cell.

Wide char at the last column (insufficient room): a space is written instead,
`wrapPending` is set.

### BCE (Background Color Erase)

`clearScreen()` and `fillLine()` fill cells with `currentStyle` rather than a default-style
empty cell. This matches the VT102 Background Color Erase behavior where cleared cells
inherit the current SGR background color.

## Design Decisions & Trade-offs

| Decision | Choice | Alternative considered | Reason |
|---|---|---|---|
| Cell representation | `final class TerminalCell` | `@JvmInline value class` over `long` | Value classes have boxing issues in arrays; `data class`-style is TDD-friendly and correct at this scale |
| Wrap mode | LCF deferred (xterm default) | Immediate wrap | Immediate wrap breaks vim/less/TUIs at line boundaries |
| `getLineAsString` | Overloaded: `trim=true` default | Always trim | Preserves raw access for debug/render diagnostics |
| Scrollback row 0 | Oldest line | Newest line | Industry consensus: xterm.js, WezTerm, alacritty all use oldest=0 |
| `insertText` overflow on last row | `autoWrap=ON` → scroll; `autoWrap=OFF` → lost | Always lost | ECMA-48 IRM: overflow behavior follows wrap mode |
| Color model | `enum TextColor` (17 values) | `byte` + render-time mapping | Clarity over micro-optimization; no performance requirement here |

## Improvements Not Implemented (Future Work)

- **Resize** (`resize(newWidth, newHeight)`): content handling strategy (clip vs. reflow) is
  non-trivial and was out of scope. The buffer currently has fixed dimensions.

- **Scroll region** (DECSTBM): VT100 supports restricting scroll operations to a sub-region
  of the screen. Not implemented; `insertEmptyLineAtBottom` always uses the full screen.

- **CSI 3J** (clear scrollback via escape sequence): the buffer supports
  `clearScreenAndScrollback()` programmatically but does not parse escape sequences.

- **Packed cell model**: packing `TerminalCell` into a `LongArray` (char: 21 bits, fg: 5 bits,
  bg: 5 bits, flags: 3 bits, placeholder: 1 bit = 35 bits total, fits in a `long`) would
  reduce GC pressure on large buffers. The current `data class`-style approach is the
  reference implementation. Optimization should be guided by profiling.

- **Deferred wrap (LCF) for `insertText`**: currently `insertText` does not check
  `wrapPending` before inserting. A complete VT implementation would resolve pending wrap
  before any printable output including insert-mode characters.

- **Snapshot testing**: a `TerminalBufferHarness` with JUnit 5 snapshot assertions
  (e.g., Selfie) would catch subtle layout regressions across multi-step scenarios.

## Testing

Tests follow **TDD** (RED → GREEN → REFACTOR). Every class has a corresponding test file.
Tests are written before production code in each phase.

Test coverage areas:

- Init invariants (screen size, cursor position, wrapPending state)
- Cursor: `setCursor` clamping, `moveCursor` boundary conditions, wrapPending reset
- Attributes: style applied to subsequent writes only
- `writeText`: LCF deferred wrap, autoWrap=off path, scroll-on-last-row
- `insertText`: shift-right, overflow wrap, overflow lost (autoWrap=off)
- `fillLine`: BCE style, OOB no-op
- Screen ops: scrollback ordering, max size cap, clearScreen BCE, cursor preservation
- Content access: trim vs raw, scrollback oldest-first ordering
- Wide chars: two-cell model, cursor adjustment, atomic clear on overwrite

## Project Structure

```
src/
├── main/java/terminal/
│   ├── TextColor.java        enum: DEFAULT + 16 ANSI colors
│   ├── StyleFlags.java       bold/italic/underline flags
│   ├── TextStyle.java        fg + bg + StyleFlags
│   ├── TerminalCell.java     single grid cell; immutable
│   ├── TerminalLine.java     array of cells with write/insert ops
│   └── TerminalBuffer.java   full buffer with screen, scrollback, cursor
└── test/java/terminal/
    ├── TextColorTest.java
    ├── StyleFlagsTest.java
    ├── TextStyleTest.java
    ├── TerminalCellTest.java
    ├── TerminalLineTest.java
    └── TerminalBufferTest.java
```

## First-Time Setup (Gradle Wrapper)

The `gradle/wrapper/gradle-wrapper.jar` is not included in this archive (binary file).
To generate it, run **one** of the following:

**Option A — if Gradle is installed globally:**
```bash
gradle wrapper --gradle-version 8.5
./gradlew test
```

**Option B — if you have IntelliJ IDEA:**
Open the project root. IDEA detects `build.gradle.kts` and sets up the wrapper automatically.

**Option C — compile and run tests manually (no Gradle needed):**

Download JUnit Platform Console Standalone:
```
https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
```
Save it as `junit-standalone.jar` in the project root.

**macOS / Linux** (classpath separator `:`)
```bash
mkdir -p out/production out/test

javac -encoding UTF-8 -d out/production src/main/java/terminal/*.java

javac -encoding UTF-8 -cp junit-standalone.jar:out/production \
  -d out/test src/test/java/terminal/*.java

java -jar junit-standalone.jar \
  --class-path out/production:out/test \
  --scan-class-path
```

**Windows** (classpath separator `;`)
```bat
mkdir out\production
mkdir out\test

javac -encoding UTF-8 -d out/production src/main/java/terminal/*.java

javac -encoding UTF-8 -cp junit-standalone.jar;out/production ^
  -d out/test src/test/java/terminal/*.java

java -jar junit-standalone.jar ^
  --class-path out/production;out/test ^
  --scan-class-path
```

package terminal;

import java.util.ArrayDeque;

public final class TerminalBuffer {

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final int width;
    private final int height;
    private final int scrollbackMaxSize;
    private final boolean autoWrap;

    // screen: index 0 = top row, index height-1 = bottom row
    private final ArrayDeque<TerminalLine> screen;
    // scrollback: index 0 = oldest line (via peekFirst/addLast convention — we use List view)
    private final ArrayDeque<TerminalLine> scrollback;

    private int cursorCol;
    private int cursorRow;
    private boolean wrapPending;
    private TextStyle currentStyle;

    public TerminalBuffer(int width, int height, int scrollbackMaxSize) {
        this(width, height, scrollbackMaxSize, true);
    }

    public TerminalBuffer(int width, int height, int scrollbackMaxSize, boolean autoWrap) {
        this.width = width;
        this.height = height;
        this.scrollbackMaxSize = scrollbackMaxSize;
        this.autoWrap = autoWrap;
        this.screen = new ArrayDeque<>();
        this.scrollback = new ArrayDeque<>();
        this.cursorCol = 0;
        this.cursorRow = 0;
        this.wrapPending = false;
        this.currentStyle = new TextStyle();

        for (int i = 0; i < height; i++) {
            screen.addLast(new TerminalLine(width));
        }
    }

    public void setAttributes(TextColor fg, TextColor bg, StyleFlags flags) {
        currentStyle = new TextStyle(fg, bg, flags);
    }

    public int getCursorCol() { return cursorCol; }
    public int getCursorRow() { return cursorRow; }
    public boolean isWrapPending() { return wrapPending; }
    public boolean isAutoWrap() { return autoWrap; }

    public void setCursor(int col, int row) {
        cursorCol = clamp(col, 0, width - 1);
        cursorRow = clamp(row, 0, height - 1);
        wrapPending = false;
    }

    public void moveCursor(Direction dir) {
        moveCursor(dir, 1);
    }

    public void moveCursor(Direction dir, int n) {
        if (n <= 0) return;
        switch (dir) {
            case RIGHT -> cursorCol = clamp(cursorCol + n, 0, width - 1);
            case LEFT  -> cursorCol = clamp(cursorCol - n, 0, width - 1);
            case DOWN  -> cursorRow = clamp(cursorRow + n, 0, height - 1);
            case UP    -> cursorRow = clamp(cursorRow - n, 0, height - 1);
        }
        // moving cursor via explicit command resets LCF flag
        wrapPending = false;
        // if landed on a wide placeholder, jump to anchor
        adjustCursorOffPlaceholder(dir);
    }

    public void writeText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            resolveWrapIfPending();
            // overwrite — clear wide pair if needed
            clearWideCharAt(cursorCol, cursorRow);
            screenLine(cursorRow).writeAt(cursorCol, ch, currentStyle);

            if (cursorCol == width - 1) {
                // LCF: char written at last col, defer wrap
                if (autoWrap) wrapPending = true;
                // autoWrap=false: cursor stays, next char overwrites last col
            } else {
                cursorCol++;
            }
        }
    }

    public void insertText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            TerminalCell overflow = screenLine(cursorRow).insertAt(cursorCol, ch, currentStyle);
            if (cursorCol < width - 1) cursorCol++;

            if (overflow != null && overflow.ch != null && autoWrap) {
                // overflow wraps to next line
                if (cursorRow < height - 1) {
                    screenLine(cursorRow + 1).insertAt(0, overflow.ch, overflow.style);
                } else {
                    // last row with autoWrap: scroll then place overflow
                    insertEmptyLineAtBottom();
                    screenLine(height - 1).insertAt(0, overflow.ch, overflow.style);
                }
            }
            // autoWrap=false: overflow silently lost
        }
    }

    public void fillLine(int row, Character ch) {
        fillLine(row, ch, currentStyle);
    }

    public void fillLine(int row, Character ch, TextStyle style) {
        if (row < 0 || row >= height) return;
        screenLine(row).fill(ch, style);
    }

    public void insertEmptyLineAtBottom() {
        // top screen line → scrollback
        TerminalLine topLine = screen.removeFirst();
        if (scrollback.size() >= scrollbackMaxSize) {
            scrollback.removeFirst(); // drop oldest
        }
        scrollback.addLast(new TerminalLine(topLine)); // copy
        screen.addLast(new TerminalLine(width));
    }

    public void clearScreen() {
        for (TerminalLine line : screen) {
            line.fill(null, currentStyle);
        }
        wrapPending = false;
        // cursor preserved
    }

    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    public Character getCharAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return null;
        return screenLine(row).cells[col].ch;
    }

    public TextStyle getAttributesAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return new TextStyle();
        return screenLine(row).cells[col].style;
    }

    public TerminalCell getCellAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return TerminalCell.empty();
        return screenLine(row).cells[col];
    }

    public Character getCharAtScrollback(int col, int scrollbackRow) {
        if (col < 0 || col >= width) return null;
        TerminalLine line = scrollbackLine(scrollbackRow);
        if (line == null) return null;
        return line.cells[col].ch;
    }

    public TextStyle getAttributesAtScrollback(int col, int scrollbackRow) {
        if (col < 0 || col >= width) return new TextStyle();
        TerminalLine line = scrollbackLine(scrollbackRow);
        if (line == null) return new TextStyle();
        return line.cells[col].style;
    }

    public int getScreenHeight() { return height; }
    public int getScrollbackSize() { return scrollback.size(); }

    // resolve LCF wrap before writing next char
    private void resolveWrapIfPending() {
        if (!wrapPending) return;
        wrapPending = false;
        if (cursorRow < height - 1) {
            cursorRow++;
            cursorCol = 0;
        } else {
            // bottom row: scroll
            insertEmptyLineAtBottom();
            cursorCol = 0;
            // cursorRow stays at height-1 (new empty line is now the last)
        }
    }

    // when writing over a cell, clear any wide pair it belongs to
    private void clearWideCharAt(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return;
        TerminalCell cell = screenLine(row).cells[col];

        if (cell.isWidePlaceholder && col > 0) {
            // clear anchor to the left
            screenLine(row).cells[col - 1] = TerminalCell.empty();
            screenLine(row).cells[col] = TerminalCell.empty();
        } else if (cell.ch != null && col + 1 < width
                && screenLine(row).cells[col + 1].isWidePlaceholder) {
            // this is a wide anchor — clear placeholder to the right
            screenLine(row).cells[col + 1] = TerminalCell.empty();
        }
    }


    // after moveCursor LEFT, if we land on placeholder jump one more left
    private void adjustCursorOffPlaceholder(Direction dir) {
        if (dir != Direction.LEFT) return;
        if (cursorCol > 0 && screenLine(cursorRow).cells[cursorCol].isWidePlaceholder) {
            cursorCol--;
        }
    }

    private TerminalLine screenLine(int row) {
        // warn: ArrayDeque has no O(1) index access; iterate directly
        int i = 0;
        for (TerminalLine line : screen) {
            if (i++ == row) return line;
        }
        throw new IndexOutOfBoundsException("screen row " + row);
    }

    // scrollback: index 0 = oldest (first inserted via addLast)
    private TerminalLine scrollbackLine(int index) {
        if (index < 0 || index >= scrollback.size()) return null;
        int i = 0;
        for (TerminalLine line : scrollback) {
            if (i++ == index) return line;
        }
        return null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
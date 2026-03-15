package terminal;

import java.util.Objects;

public final class TerminalCell {

    public final Character ch;           // null = empty cell
    public final TextStyle style;
    public final boolean isWidePlaceholder;

    public TerminalCell() {
        this(null, new TextStyle(), false);
    }

    public TerminalCell(Character ch, TextStyle style) {
        this(ch, style, false);
    }

    public TerminalCell(Character ch, TextStyle style, boolean isWidePlaceholder) {
        this.ch = ch;
        this.style = style;
        this.isWidePlaceholder = isWidePlaceholder;
    }

    // wide placeholder occupying the right half of a wide char
    public static TerminalCell widePlaceholder(TextStyle style) {
        return new TerminalCell(null, style, true);
    }

    public static TerminalCell empty() {
        return new TerminalCell();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalCell)) return false;
        TerminalCell c = (TerminalCell) o;
        return isWidePlaceholder == c.isWidePlaceholder
            && Objects.equals(ch, c.ch)
            && Objects.equals(style, c.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ch, style, isWidePlaceholder);
    }

    @Override
    public String toString() {
        return "TerminalCell{ch=" + ch + ", style=" + style + ", placeholder=" + isWidePlaceholder + "}";
    }
}

package terminal;

import java.util.Objects;

public final class TextStyle {

    public final TextColor fg;
    public final TextColor bg;
    public final StyleFlags flags;

    public TextStyle() {
        this(TextColor.DEFAULT, TextColor.DEFAULT, new StyleFlags());
    }

    public TextStyle(TextColor fg, TextColor bg, StyleFlags flags) {
        this.fg = fg;
        this.bg = bg;
        this.flags = flags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextStyle)) return false;
        TextStyle s = (TextStyle) o;
        return fg == s.fg && bg == s.bg && Objects.equals(flags, s.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fg, bg, flags);
    }

    @Override
    public String toString() {
        return "TextStyle{fg=" + fg + ", bg=" + bg + ", flags=" + flags + "}";
    }
}

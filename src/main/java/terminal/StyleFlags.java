package terminal;

import java.util.Objects;

public final class StyleFlags {

    public final boolean bold;
    public final boolean italic;
    public final boolean underline;

    public StyleFlags() {
        this(false, false, false);
    }

    public StyleFlags(boolean bold, boolean italic, boolean underline) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StyleFlags)) return false;
        StyleFlags s = (StyleFlags) o;
        return bold == s.bold && italic == s.italic && underline == s.underline;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, underline);
    }

    @Override
    public String toString() {
        return "StyleFlags{bold=" + bold + ", italic=" + italic + ", underline=" + underline + "}";
    }
}

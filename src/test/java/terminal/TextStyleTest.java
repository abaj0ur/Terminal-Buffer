package terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextStyleTest {

    @Test
    void defaultFgBgAreDefault() {
        TextStyle s = new TextStyle();
        assertEquals(TextColor.DEFAULT, s.fg);
        assertEquals(TextColor.DEFAULT, s.bg);
    }

    @Test
    void defaultFlagsAreAllFalse() {
        TextStyle s = new TextStyle();
        assertEquals(new StyleFlags(), s.flags);
    }

    @Test
    void equalityWhenSame() {
        assertEquals(new TextStyle(), new TextStyle());
    }

    @Test
    void equalityWithExplicitValues() {
        TextStyle a = new TextStyle(TextColor.RED, TextColor.BLUE, new StyleFlags(true, false, false));
        TextStyle b = new TextStyle(TextColor.RED, TextColor.BLUE, new StyleFlags(true, false, false));
        assertEquals(a, b);
    }

    @Test
    void inequalityWhenDifferentFg() {
        assertNotEquals(
            new TextStyle(TextColor.RED, TextColor.DEFAULT, new StyleFlags()),
            new TextStyle(TextColor.GREEN, TextColor.DEFAULT, new StyleFlags())
        );
    }
}

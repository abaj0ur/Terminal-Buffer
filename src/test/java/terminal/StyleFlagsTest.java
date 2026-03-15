package terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StyleFlagsTest {

    @Test
    void defaultsAllFalse() {
        StyleFlags f = new StyleFlags();
        assertFalse(f.bold);
        assertFalse(f.italic);
        assertFalse(f.underline);
    }

    @Test
    void equalityWhenSame() {
        assertEquals(new StyleFlags(), new StyleFlags());
        assertEquals(new StyleFlags(true, false, true), new StyleFlags(true, false, true));
    }

    @Test
    void inequalityWhenDifferent() {
        assertNotEquals(new StyleFlags(true, false, false), new StyleFlags(false, false, false));
    }

    @Test
    void constructorSetsFields() {
        StyleFlags f = new StyleFlags(true, true, false);
        assertTrue(f.bold);
        assertTrue(f.italic);
        assertFalse(f.underline);
    }
}

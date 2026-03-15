package terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextColorTest {

    @Test
    void defaultExists() {
        assertNotNull(TextColor.DEFAULT);
    }

    @Test
    void has17Values() {
        assertEquals(17, TextColor.values().length);
    }

    @Test
    void contains16StandardColors() {
        TextColor[] colors = TextColor.values();
        // spot-check standard 8
        assertNotNull(TextColor.BLACK);
        assertNotNull(TextColor.RED);
        assertNotNull(TextColor.GREEN);
        assertNotNull(TextColor.YELLOW);
        assertNotNull(TextColor.BLUE);
        assertNotNull(TextColor.MAGENTA);
        assertNotNull(TextColor.CYAN);
        assertNotNull(TextColor.WHITE);
        // bright variants
        assertNotNull(TextColor.BRIGHT_BLACK);
        assertNotNull(TextColor.BRIGHT_RED);
        assertNotNull(TextColor.BRIGHT_GREEN);
        assertNotNull(TextColor.BRIGHT_YELLOW);
        assertNotNull(TextColor.BRIGHT_BLUE);
        assertNotNull(TextColor.BRIGHT_MAGENTA);
        assertNotNull(TextColor.BRIGHT_CYAN);
        assertNotNull(TextColor.BRIGHT_WHITE);
    }
}

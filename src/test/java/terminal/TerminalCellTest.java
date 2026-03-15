package terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalCellTest {

    @Test
    void emptyCellHasNullChar() {
        TerminalCell cell = new TerminalCell();
        assertNull(cell.ch);
    }

    @Test
    void emptyCellHasDefaultStyle() {
        assertEquals(new TextStyle(), new TerminalCell().style);
    }

    @Test
    void emptyCellIsNotWidePlaceholder() {
        assertFalse(new TerminalCell().isWidePlaceholder);
    }

    @Test
    void cellWithChar() {
        TerminalCell cell = new TerminalCell('A', new TextStyle());
        assertEquals('A', cell.ch);
    }

    @Test
    void widePlaceholderFlag() {
        TerminalCell cell = new TerminalCell(null, new TextStyle(), true);
        assertTrue(cell.isWidePlaceholder);
    }
}

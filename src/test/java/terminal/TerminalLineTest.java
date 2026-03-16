package terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalLineTest {

    private static final TextStyle DEFAULT_STYLE = new TextStyle();
    private static final TextStyle RED_STYLE =
        new TextStyle(TextColor.RED, TextColor.DEFAULT, new StyleFlags());

    @Test
    void constructorCreatesEmptyCells() {
        TerminalLine line = new TerminalLine(10);
        for (int i = 0; i < 10; i++) {
            assertNull(line.cells[i].ch, "cell " + i + " should be empty");
        }
    }

    @Test
    void widthStoredCorrectly() {
        TerminalLine line = new TerminalLine(80);
        assertEquals(80, line.cells.length);
    }

    @Test
    void fillWithChar() {
        TerminalLine line = new TerminalLine(5);
        line.fill('X', RED_STYLE);
        for (int i = 0; i < 5; i++) {
            assertEquals('X', line.cells[i].ch);
            assertEquals(RED_STYLE, line.cells[i].style);
        }
    }

    @Test
    void fillWithNullClearsAllCells() {
        TerminalLine line = new TerminalLine(5);
        line.fill('X', DEFAULT_STYLE);
        line.fill(null, DEFAULT_STYLE);
        for (int i = 0; i < 5; i++) {
            assertNull(line.cells[i].ch);
        }
    }

    @Test
    void writeAtUpdatesCell() {
        TerminalLine line = new TerminalLine(10);
        line.writeAt(3, 'Z', RED_STYLE);
        assertEquals('Z', line.cells[3].ch);
        assertEquals(RED_STYLE, line.cells[3].style);
    }

    @Test
    void writeAtOutOfBoundsIsNoOp() {
        TerminalLine line = new TerminalLine(5);
        assertDoesNotThrow(() -> line.writeAt(-1, 'X', DEFAULT_STYLE));
        assertDoesNotThrow(() -> line.writeAt(5, 'X', DEFAULT_STYLE));
        for (int i = 0; i < 5; i++) assertNull(line.cells[i].ch);
    }

    @Test
    void insertAtShiftsRight() {
        TerminalLine line = new TerminalLine(5);
        line.writeAt(0, 'A', DEFAULT_STYLE);
        line.writeAt(1, 'B', DEFAULT_STYLE);
        line.writeAt(2, 'C', DEFAULT_STYLE);

        TerminalCell overflow = line.insertAt(1, 'X', DEFAULT_STYLE);

        // X inserted at 1; A stays at 0; B,C shifted right; C falls off
        assertEquals('A', line.cells[0].ch);
        assertEquals('X', line.cells[1].ch);
        assertEquals('B', line.cells[2].ch);
        assertEquals('C', line.cells[3].ch);
        assertNull(line.cells[4].ch);  // shifted empty
        // overflow = original last non-null? no — overflow = what was pushed past end
        // C was at index 2, shifted to 3; nothing at 4 was pushed out
        // Let's be precise: line width=5, cells [A,B,C,_,_], insert X at 1
        // result: [A,X,B,C,_], overflow = null (nothing pushed off)
        assertNull(overflow);
    }

    @Test
    void insertAtReturnsOverflowWhenLineFull() {
        TerminalLine line = new TerminalLine(3);
        line.writeAt(0, 'A', DEFAULT_STYLE);
        line.writeAt(1, 'B', DEFAULT_STYLE);
        line.writeAt(2, 'C', DEFAULT_STYLE);

        // insert X at 0: [A,B,C] → [X,A,B], C overflows
        TerminalCell overflow = line.insertAt(0, 'X', DEFAULT_STYLE);

        assertEquals('X', line.cells[0].ch);
        assertEquals('A', line.cells[1].ch);
        assertEquals('B', line.cells[2].ch);
        assertNotNull(overflow);
        assertEquals('C', overflow.ch);
    }
}

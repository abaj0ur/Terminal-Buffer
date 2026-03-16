package terminal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    // 5x3 buffer (width=5, height=3) used in most tests
    private TerminalBuffer buf;

    @BeforeEach
    void setUp() {
        buf = new TerminalBuffer(5, 3, 10);
    }

    @Nested
    class Init {
        @Test
        void screenHasCorrectHeight() {
            assertEquals(3, buf.getScreenHeight());
        }

        @Test
        void allCellsEmptyOnInit() {
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 5; col++)
                    assertNull(buf.getCharAt(col, row));
        }

        @Test
        void cursorAtOrigin() {
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void wrapPendingFalseOnInit() {
            assertFalse(buf.isWrapPending());
        }

        @Test
        void autoWrapTrueByDefault() {
            assertTrue(buf.isAutoWrap());
        }

        @Test
        void scrollbackEmptyOnInit() {
            assertEquals(0, buf.getScrollbackSize());
        }
    }

    @Nested
    class CursorManagement {
        @Test
        void setCursorValid() {
            buf.setCursor(2, 1);
            assertEquals(2, buf.getCursorCol());
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void setCursorResetsWrapPending() {
            buf.writeText("AAAAA"); // fills row, sets wrapPending
            assertTrue(buf.isWrapPending());
            buf.setCursor(0, 0);
            assertFalse(buf.isWrapPending());
        }

        @Test
        void setCursorClampsNegativeCol() {
            buf.setCursor(-1, 0);
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void setCursorClampsNegativeRow() {
            buf.setCursor(0, -5);
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void setCursorClampsColBeyondWidth() {
            buf.setCursor(99, 0);
            assertEquals(4, buf.getCursorCol()); // width-1
        }

        @Test
        void setCursorClampsRowBeyondHeight() {
            buf.setCursor(0, 99);
            assertEquals(2, buf.getCursorRow()); // height-1
        }

        @Test
        void moveCursorRight() {
            buf.moveCursor(TerminalBuffer.Direction.RIGHT, 2);
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void moveCursorLeft() {
            buf.setCursor(3, 0);
            buf.moveCursor(TerminalBuffer.Direction.LEFT, 2);
            assertEquals(1, buf.getCursorCol());
        }

        @Test
        void moveCursorDown() {
            buf.moveCursor(TerminalBuffer.Direction.DOWN, 2);
            assertEquals(2, buf.getCursorRow());
        }

        @Test
        void moveCursorUp() {
            buf.setCursor(0, 2);
            buf.moveCursor(TerminalBuffer.Direction.UP, 1);
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void moveCursorClampsAtRightEdge() {
            buf.moveCursor(TerminalBuffer.Direction.RIGHT, 99);
            assertEquals(4, buf.getCursorCol());
        }

        @Test
        void moveCursorClampsAtLeftEdge() {
            buf.moveCursor(TerminalBuffer.Direction.LEFT, 99);
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void moveCursorClampsAtBottomEdge() {
            buf.moveCursor(TerminalBuffer.Direction.DOWN, 99);
            assertEquals(2, buf.getCursorRow());
        }

        @Test
        void moveCursorClampsAtTopEdge() {
            buf.moveCursor(TerminalBuffer.Direction.UP, 99);
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void moveCursorZeroIsNoOp() {
            buf.setCursor(2, 1);
            buf.moveCursor(TerminalBuffer.Direction.RIGHT, 0);
            assertEquals(2, buf.getCursorCol());
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void moveCursorResetsWrapPending() {
            buf.writeText("AAAAA");
            assertTrue(buf.isWrapPending());
            buf.moveCursor(TerminalBuffer.Direction.LEFT, 1);
            assertFalse(buf.isWrapPending());
        }
    }

    @Nested
    class Attributes {
        @Test
        void setAttributesUpdatesCurrentStyle() {
            buf.setAttributes(TextColor.RED, TextColor.BLUE, new StyleFlags(true, false, false));
            buf.writeText("A");
            assertEquals(TextColor.RED, buf.getAttributesAt(0, 0).fg);
            assertEquals(TextColor.BLUE, buf.getAttributesAt(0, 0).bg);
            assertTrue(buf.getAttributesAt(0, 0).flags.bold);
        }

        @Test
        void styleAppliedToSubsequentWritesOnly() {
            buf.writeText("A");
            buf.setAttributes(TextColor.RED, TextColor.DEFAULT, new StyleFlags());
            buf.writeText("B");
            assertEquals(TextColor.DEFAULT, buf.getAttributesAt(0, 0).fg); // A: default
            assertEquals(TextColor.RED, buf.getAttributesAt(1, 0).fg);     // B: red
        }
    }

    @Nested
    class WriteText {
        @Test
        void writeSingleChar() {
            buf.writeText("A");
            assertEquals('A', buf.getCharAt(0, 0));
            assertEquals(1, buf.getCursorCol());
        }

        @Test
        void writeMultipleChars() {
            buf.writeText("ABC");
            assertEquals('A', buf.getCharAt(0, 0));
            assertEquals('B', buf.getCharAt(1, 0));
            assertEquals('C', buf.getCharAt(2, 0));
            assertEquals(3, buf.getCursorCol());
        }

        @Test
        void writeOverridesExistingContent() {
            buf.writeText("ABC");
            buf.setCursor(1, 0);
            buf.writeText("X");
            assertEquals('X', buf.getCharAt(1, 0));
        }

        @Test
        void writeAtLastColSetsWrapPending() {
            // write 5 chars on a width-5 buffer
            buf.writeText("AAAAA");
            // cursor stays at col 4 (LCF deferred wrap)
            assertEquals(4, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
            assertTrue(buf.isWrapPending());
        }

        @Test
        void writeWithWrapPendingResolvesWrapFirst() {
            buf.writeText("AAAAA"); // fills row 0, wrapPending=true
            buf.writeText("B");    // should wrap to row 1 col 0 first
            assertEquals('B', buf.getCharAt(0, 1));
            assertEquals(1, buf.getCursorCol());
            assertEquals(1, buf.getCursorRow());
            assertFalse(buf.isWrapPending());
        }

        @Test
        void writeWrapsOnLastRowTriggersScroll() {
            // fill all 3 rows, then one more char should scroll
            buf.writeText("AAAAA"); // row 0 full, wrapPending=true, no scroll yet
            buf.writeText("BBBBB"); // resolveWrap→row1; row 1 full, wrapPending=true
            buf.writeText("CCCCC"); // resolveWrap→row2; row 2 full, wrapPending=true
            // no scroll has happened yet — all 3 rows used without scrolling
            assertEquals(0, buf.getScrollbackSize());
            buf.writeText("D");    // resolveWrap: at bottom row → insertEmptyLineAtBottom → scrollback=1
            assertEquals(1, buf.getScrollbackSize());
            assertEquals('D', buf.getCharAt(0, 2)); // D at new last row col 0
        }

        @Test
        void autoWrapOffCursorStaysAtLastCol() {
            TerminalBuffer noWrap = new TerminalBuffer(5, 3, 10, false);
            noWrap.writeText("AAAAAX"); // X should overwrite last cell
            assertEquals('X', noWrap.getCharAt(4, 0));
            assertEquals(4, noWrap.getCursorCol());
            assertFalse(noWrap.isWrapPending());
        }
    }

    @Nested
    class InsertText {
        @Test
        void insertShiftsRight() {
            buf.writeText("ABCD");
            buf.setCursor(1, 0);
            buf.insertText("X");
            assertEquals('A', buf.getCharAt(0, 0));
            assertEquals('X', buf.getCharAt(1, 0));
            assertEquals('B', buf.getCharAt(2, 0));
            assertEquals('C', buf.getCharAt(3, 0));
            assertEquals('D', buf.getCharAt(4, 0));
        }

        @Test
        void insertOverflowWrapsToNextLineWhenAutoWrapOn() {
            // fill row 0: ABCDE
            buf.writeText("ABCDE");
            buf.setCursor(0, 0);
            // insert X at col 0: shifts all right, E overflows → col 0 of row 1
            buf.insertText("X");
            assertEquals('E', buf.getCharAt(0, 1));
        }

        @Test
        void insertOverflowLostOnLastRowWhenAutoWrapOff() {
            TerminalBuffer noWrap = new TerminalBuffer(5, 3, 10, false);
            noWrap.writeText("ABCDE");
            // move cursor to last row and fill it
            noWrap.setCursor(0, 2);
            noWrap.writeText("VWXYZ");
            noWrap.setCursor(0, 2);
            noWrap.insertText("1"); // Z should be lost
            assertNull(noWrap.getCharAt(0, 3)); // no row 3
            assertEquals('1', noWrap.getCharAt(0, 2));
        }
    }

    @Nested
    class FillLine {
        @Test
        void fillsAllCells() {
            buf.fillLine(1, 'X');
            for (int col = 0; col < 5; col++)
                assertEquals('X', buf.getCharAt(col, 1));
        }

        @Test
        void fillWithNullClearsLine() {
            buf.writeText("ABCDE");
            buf.fillLine(0, null);
            for (int col = 0; col < 5; col++)
                assertNull(buf.getCharAt(col, 0));
        }

        @Test
        void fillOutOfBoundsIsNoOp() {
            assertDoesNotThrow(() -> buf.fillLine(-1, 'X'));
            assertDoesNotThrow(() -> buf.fillLine(99, 'X'));
        }

        @Test
        void fillUsesCurrentStyle() {
            buf.setAttributes(TextColor.RED, TextColor.DEFAULT, new StyleFlags());
            buf.fillLine(0, 'A');
            assertEquals(TextColor.RED, buf.getAttributesAt(0, 0).fg);
        }
    }
}

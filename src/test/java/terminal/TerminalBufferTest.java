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
}

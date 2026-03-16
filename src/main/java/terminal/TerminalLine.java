package terminal;

public final class TerminalLine {

    public final TerminalCell[] cells;

    public TerminalLine(int width) {
        this.cells = new TerminalCell[width];
        for (int i = 0; i < width; i++) {
            cells[i] = TerminalCell.empty();
        }
    }

    // deep copy constructor — used when pushing line into scrollback
    public TerminalLine(TerminalLine source) {
        this.cells = new TerminalCell[source.cells.length];
        System.arraycopy(source.cells, 0, this.cells, 0, source.cells.length);
    }

    public void fill(Character ch, TextStyle style) {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new TerminalCell(ch, style);
        }
    }

    // no-op if out of bounds
    public void writeAt(int col, Character ch, TextStyle style) {
        if (col < 0 || col >= cells.length) return;
        cells[col] = new TerminalCell(ch, style);
    }

    // shifts cells right from col; returns overflow cell (last cell pushed out) or null
    public TerminalCell insertAt(int col, Character ch, TextStyle style) {
        if (col < 0 || col >= cells.length) return null;
        TerminalCell overflow = cells[cells.length - 1];
        // shift right
        System.arraycopy(cells, col, cells, col + 1, cells.length - col - 1);
        cells[col] = new TerminalCell(ch, style);
        // only return overflow if it had content
        return overflow.ch != null || overflow.isWidePlaceholder ? overflow : null;
    }
}

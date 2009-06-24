package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

public class Grid {

    private ValueEval[][] grid;

    public void setGrid(ValueEval[][] grid) {
        this.grid = grid;
    }

    public int getWidth() {
        return grid.length;
    }

    public int getHeight() {
        return grid[0].length;
    }

    public ValueEval getValue(int x, int y) {
        return grid[x][y];
    }

}

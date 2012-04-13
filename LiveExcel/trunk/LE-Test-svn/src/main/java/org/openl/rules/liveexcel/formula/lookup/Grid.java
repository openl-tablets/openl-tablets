package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
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

    public void setValue(int x, int y, ValueEval newValue) {
        grid[x][y] = newValue;
    }

    public boolean isBlank(int x, int y) {
        ValueEval value = getValue(x, y);
        if (value instanceof BlankEval
                || (value instanceof StringEval && "".equals(((StringEval) value).getStringValue()))) {
            return true;
        } else {
            return false;
        }
    }
}

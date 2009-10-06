package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * 
 * Join multiple tables in single one. Tables should have the same dimensions.
 * 
 * @author spetrakovsky
 */
public class CompositeGrid extends Grid {

    private Grid[] grids;

    private int width = 0;

    private int height = 0;

    public CompositeGrid(Grid... grids) {
        this.grids = grids;
        initialize();
    }

    private void initialize() {
        for (int i = 0; i < grids.length; i++) {
            height += grids[i].getHeight();
        }
        width = grids[0].getWidth();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public ValueEval getValue(int x, int y) {
        return getGrid(y).getValue(x, getY(y));
    }

    private int getY(int y) {
        int currentGridHeight = 0;
        for (int i = 0; i < grids.length; i++) {
            currentGridHeight += grids[i].getHeight();
            if (y < currentGridHeight) {
                return y - currentGridHeight + grids[i].getHeight();
            }
        }
        return 0;
    }

    private Grid getGrid(int y) {
        int currentGridHeight = 0;
        for (int i = 0; i < grids.length; i++) {
            currentGridHeight += grids[i].getHeight();
            if (y < currentGridHeight) {
                return grids[i];
            }
        }
        return null;
    }

}

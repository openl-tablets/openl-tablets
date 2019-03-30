/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.syntax;

import org.openl.rules.table.IGrid;
import org.openl.util.text.IPosition;
import org.openl.util.text.TextInfo;

/**
 * @author snshor
 *
 */
public class GridPosition implements IPosition {

    private int x, y;

    private IGrid grid;

    public GridPosition(int x, int y, IGrid grid) {
        this.x = x;
        this.y = y;
        this.grid = grid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.IPosition#getAbsolutePosition(org.openl.util.text.TextInfo)
     */
    @Override
    public int getAbsolutePosition(TextInfo info) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.IPosition#getColumn(org.openl.util.text.TextInfo,
     *      int)
     */
    @Override
    public int getColumn(TextInfo info, int tabSize) {
        return x;
    }

    /**
     * @return
     */
    public IGrid getGrid() {
        return grid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.util.text.IPosition#getLine(org.openl.util.text.TextInfo)
     */
    @Override
    public int getLine(TextInfo info) {
        return y;
    }

    @Override
    public String toString() {
        return grid.getCell(x, y).getUri();
    }

}

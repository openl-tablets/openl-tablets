/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table;

/**
 * @author snshor
 *
 */

public class GridDelegator implements IGrid {

    protected IGrid delegate;

    public GridDelegator(IGrid delegate) {
        this.delegate = delegate;
    }

    public ICell getCell(int column, int row) {
        return delegate.getCell(column, row);
    }

    public int getColumnWidth(int col) {
        return delegate.getColumnWidth(col);
    }

    public int getMaxColumnIndex(int row) {
        return delegate.getMaxColumnIndex(row);
    }

    public int getMaxRowIndex() {
        return delegate.getMaxRowIndex();
    }

    public IGridRegion getMergedRegion(int i) {
        return delegate.getMergedRegion(i);
    }

    public int getMinColumnIndex(int row) {
        return delegate.getMinColumnIndex(row);
    }

    public int getMinRowIndex() {
        return delegate.getMinRowIndex();
    }

    public int getNumberOfMergedRegions() {
        return delegate.getNumberOfMergedRegions();
    }

    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
        return delegate.getRangeUri(colStart, rowStart, colEnd, rowEnd);
    }

    public IGridRegion getRegionStartingAt(int colFrom, int rowFrom) {
        return delegate.getRegionStartingAt(colFrom, rowFrom);
    }

    public String getUri() {
        return delegate.getUri();
    }

    public boolean isEmpty(int col, int row) {
        return delegate.isEmpty(col, row);
    }

    public boolean isPartOfTheMergedRegion(int col, int row) {
        return delegate.isPartOfTheMergedRegion(col, row);
    }

    public IGridRegion getRegionContaining(int column, int row) {
        return delegate.getRegionContaining(column, row);
    }

}

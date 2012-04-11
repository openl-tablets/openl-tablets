/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 */

public class GridDelegator implements IGrid {

    protected IGrid delegate;

    public GridDelegator(IGrid delegate) {
        this.delegate = delegate;
    }

    public int getCellHeight(int column, int row) {
        return delegate.getCellHeight(column, row);
    }

    public ICellInfo getCellInfo(int column, int row) {
        return delegate.getCellInfo(column, row);
    }

    public ICellStyle getCellStyle(int column, int row) {
        return delegate.getCellStyle(column, row);
    }

    public int getCellType(int column, int row) {
        return delegate.getCellType(column, row);
    }

    public String getCellUri(int column, int row) {
        return delegate.getCellUri(column, row);
    }

    public int getCellWidth(int column, int row) {
        return delegate.getCellWidth(column, row);
    }

    public int getColumnWidth(int col) {
        return delegate.getColumnWidth(col);
    }

    public Date getDateCellValue(int column, int row) {
        return delegate.getDateCellValue(column, row);
    }

    public double getDoubleCellValue(int column, int row) {
        return delegate.getDoubleCellValue(column, row);
    }

    public FormattedCell getFormattedCell(int column, int row) {
        return delegate.getFormattedCell(column, row);
    }

    public String getFormattedCellValue(int column, int row) {
        return delegate.getFormattedCellValue(column, row);
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

    public Object getObjectCellValue(int column, int row) {
        return delegate.getObjectCellValue(column, row);
    }

    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
        return delegate.getRangeUri(colStart, rowStart, colEnd, rowEnd);
    }

    public IGridRegion getRegionStartingAt(int colFrom, int rowFrom) {
        return delegate.getRegionStartingAt(colFrom, rowFrom);
    }

    public String getStringCellValue(int column, int row) {
        return delegate.getStringCellValue(column, row);
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

}

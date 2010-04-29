/*
 * Created on Oct 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public abstract class ALogicalTable implements ILogicalTable {

    public ILogicalTable columns(int from) {
        return columns(from, getLogicalWidth() - 1);
    }

    public ILogicalTable columns(int from, int to) {
        if (getLogicalWidth() == to - from + 1) {
            return this;
        }

        return columnsInternal(from, to);
    }    

    public ILogicalTable getLogicalRegion(int column, int row, int width, int height) {
        if (column == 0 && width == getLogicalWidth()) {
            return rows(row, row + height - 1);
        }

        if (row == 0 && height == getLogicalHeight()) {
            return columns(column, column + width - 1);
        }

        return getLogicalRegionInternal(column, row, width, height);
    }
    
    public ILogicalTable rows(int from) {
        return rows(from, getLogicalHeight() - 1);
    }

    public ILogicalTable rows(int from, int to) {
        if (getLogicalHeight() == to - from + 1) {
            return this;
        }

        return rowsInternal(from, to);
    }
    
    @Override
    public String toString(){
        return "T(" + getLogicalWidth() + " x " + getLogicalHeight() + ")";
    }   
    
    protected abstract ILogicalTable columnsInternal(int from, int to);
    
    protected abstract ILogicalTable getLogicalRegionInternal(int column, int row, int width, int height);
    
    protected abstract ILogicalTable rowsInternal(int from, int to);
}

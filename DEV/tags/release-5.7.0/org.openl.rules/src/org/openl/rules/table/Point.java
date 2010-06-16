package org.openl.rules.table;

import java.io.Serializable;

/**
 * Handles two coordinates: column number and row number.
 *
 */
public class Point implements Serializable {
    
    private static final long serialVersionUID = 5186952375131099814L;
    
    private int column;
    private int row;

    public Point(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setRow(int row) {
        this.row = row;
    }
    
}
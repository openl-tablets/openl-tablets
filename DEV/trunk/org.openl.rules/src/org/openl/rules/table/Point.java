package org.openl.rules.table;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
    
    @Override
    public boolean equals(Object obj) {
        EqualsBuilder builder = new EqualsBuilder();
        if (!(obj instanceof Point)) {;
            return false;
        }
        Point another = (Point)obj;
        builder.append(another.column, column);
        builder.append(another.row, row);
        
        return builder.isEquals();
    }
    
    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder()
            .append(column)
            .append(row)
            .toHashCode();
        
        return hashCode;
    }
    
    @Override
    public String toString() {
        return String.format("column index: %s\nrow index: %s", column, row);
    }
    
    
}
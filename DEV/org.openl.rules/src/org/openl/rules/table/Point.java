package org.openl.rules.table;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.table.Point.AdapterPoint;

/**
 * Handles two coordinates: column number and row number.
 *
 */
@XmlRootElement
@XmlJavaTypeAdapter(PointXmlAdapter.class)
public final class Point implements Serializable {
    
    private static final long serialVersionUID = 5186952375131099814L;
    
    public static class AdapterPoint {
        private int column;
        private int row;
        
        public int getColumn() {
            return column;
        }
        
        public void setColumn(int column) {
            this.column = column;
        }

        public int getRow() {
            return row;
        }
        
        public void setRow(int row) {
            this.row = row;
        }
     
    }
    
    private int column;
    private int row;

    public Point(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public Point() {}

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    /**
     * For converters
     */
    public Point moveRight() {
        return new Point(column + 1, row);
    }

    /**
     * For converters
     */
    public Point moveDown() {
        return new Point(column, row + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point)) {
            return false;
        }
        Point another = (Point)obj;
        return another.column == column && another.row == row;
    }
    
    @Override
    public int hashCode() {
        return column + row * 31;
    }

    @Override
    public String toString() {
        return String.format("column index: %s\nrow index: %s", column, row);
    }
    
}

class PointXmlAdapter extends XmlAdapter<AdapterPoint,Point> {
    public Point unmarshal(AdapterPoint val) throws Exception {
        return new Point(val.getColumn(), val.getRow());
    }
    public AdapterPoint marshal(Point val) throws Exception {
        AdapterPoint pointAdapter = new AdapterPoint();
        pointAdapter.setColumn(val.getColumn());
        pointAdapter.setRow(val.getRow());
        return pointAdapter;
    }
}
/* ====================================================================
   Copyright 2002-2004   Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.util;

import org.apache.poi.hssf.record.MergeCellsRecord.MergedRegion;

/**
 * Represents a from/to row/col square. This is a object primitive that can be
 * used to represent row,col - row,col just as one would use String to represent
 * a string of characters. Its really only useful for HSSF though.
 *
 * @author Andrew C. Oliver acoliver at apache dot org
 */

public class Region implements Comparable {
    private int rowFrom;
    private short colFrom;
    private int rowTo;
    private short colTo;

    /**
     * Creates a new instance of Region (0,0 - 0,0)
     */

    public Region() {
    }

    public Region(int rowFrom, short colFrom, int rowTo, short colTo) {
        this.rowFrom = rowFrom;
        this.rowTo = rowTo;
        this.colFrom = colFrom;
        this.colTo = colTo;
    }

    /**
     * special constructor (I know this is bad but it is so wrong that its right
     * okay) that makes a region from a mergedcells's region subrecord.
     */

    public Region(MergedRegion region) {
        this(region.row_from, region.col_from, region.row_to, region.col_to);
    }

    public int compareTo(Object o) {
        return compareTo((Region) o);
    }

    /**
     * Compares that the given region is the same less than or greater than this
     * region. If any regional coordiant passed in is less than this regions
     * coordinants then a positive integer is returned. Otherwise a negative
     * integer is returned.
     *
     * @param r region
     * @see #compareTo(Object)
     */

    public int compareTo(Region r) {
        if ((getRowFrom() == r.getRowFrom()) && (getColumnFrom() == r.getColumnFrom())
                && (getRowTo() == r.getRowTo()) && (getColumnTo() == r.getColumnTo())) {
            return 0;
        }
        if ((getRowFrom() < r.getRowFrom()) || (getColumnFrom() < r.getColumnFrom())
                || (getRowTo() < r.getRowTo()) || (getColumnTo() < r.getColumnTo())) {
            return 1;
        }
        return -1;
    }

    /**
     * Answers: "is the row/column inside this range?"
     *
     * @returns boolean - true if the cell is in the range and false if it is
     *          not
     */

    public boolean contains(int row, short col) {
        if ((rowFrom <= row) && (rowTo >= row) && (colFrom <= col) && (colTo >= col)) {

            // System.out.println("Region ("+rowFrom+","+colFrom+","+rowTo+","+
            // colTo+") does contain "+row+","+col);
            return true;
        }
        return false;
    }

    public boolean equals(Region r) {
        return (compareTo(r) == 0);
    }

    /**
     * @returns the area contained by this region (number of cells)
     */

    public int getArea() {
        return ((1 + (getRowTo() - getRowFrom())) * (1 + (getColumnTo() - getColumnFrom())));
    }

    /**
     * get the upper left hand corner column number
     *
     * @return column number for the upper left hand corner
     */

    public short getColumnFrom() {
        return colFrom;
    }

    /**
     * get the lower right hand corner column number
     *
     * @return column number for the lower right hand corner
     */

    public short getColumnTo() {
        return colTo;
    }

    /**
     * get the upper left hand corner row number
     *
     * @return row number for the upper left hand corner
     */

    public int getRowFrom() {
        return rowFrom;
    }

    /**
     * get the lower right hand corner row number
     *
     * @return row number for the lower right hand corner
     */

    public int getRowTo() {
        return rowTo;
    }

    /**
     * set the upper left hand corner column number
     *
     * @param colFrom column number for the upper left hand corner
     */

    public void setColumnFrom(short colFrom) {
        this.colFrom = colFrom;
    }

    /**
     * set the lower right hand corner column number
     *
     * @param colTo column number for the lower right hand corner
     */

    public void setColumnTo(short colTo) {
        this.colTo = colTo;
    }

    /**
     * set the upper left hand corner row number
     *
     * @param rowFrom row number for the upper left hand corner
     */

    public void setRowFrom(int rowFrom) {
        this.rowFrom = rowFrom;
    }

    /**
     * get the lower right hand corner row number
     *
     * @param rowTo row number for the lower right hand corner
     */

    public void setRowTo(int rowTo) {
        this.rowTo = rowTo;
    }
}

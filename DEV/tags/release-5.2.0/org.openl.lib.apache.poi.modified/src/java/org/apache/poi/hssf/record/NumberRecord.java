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

/*
 * NumberRecord.java
 *
 * Created on October 1, 2001, 8:01 PM
 */
package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.hssf.record.Record;

/**
 * Contains a numeric cell value.
 * <P>
 * REFERENCE: PG 334 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 * <P>
 *
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */

public class NumberRecord extends Record implements CellValueRecordInterface, Comparable {
    public static final short sid = 0x203;
    // private short field_1_row;
    private int field_1_row;
    private short field_2_col;
    private short field_3_xf;
    private double field_4_value;

    /** Creates new NumberRecord */
    public NumberRecord() {
    }

    /**
     * Constructs a Number record and sets its fields appropriately.
     *
     * @param id id must be 0x203 or an exception will be throw upon validation
     * @param size the size of the data area of the record
     * @param data data of the record (should not contain sid/len)
     */

    public NumberRecord(short id, short size, byte[] data) {
        super(id, size, data);
    }

    /**
     * Constructs a Number record and sets its fields appropriately.
     *
     * @param id id must be 0x203 or an exception will be throw upon validation
     * @param size the size of the data area of the record
     * @param data data of the record (should not contain sid/len)
     * @param offset of the data
     */

    public NumberRecord(short id, short size, byte[] data, int offset) {
        super(id, size, data, offset);
    }

    @Override
    public Object clone() {
        NumberRecord rec = new NumberRecord();
        rec.field_1_row = field_1_row;
        rec.field_2_col = field_2_col;
        rec.field_3_xf = field_3_xf;
        rec.field_4_value = field_4_value;
        return rec;
    }

    public int compareTo(Object obj) {
        CellValueRecordInterface loc = (CellValueRecordInterface) obj;

        if ((getRow() == loc.getRow()) && (getColumn() == loc.getColumn())) {
            return 0;
        }
        if (getRow() < loc.getRow()) {
            return -1;
        }
        if (getRow() > loc.getRow()) {
            return 1;
        }
        if (getColumn() < loc.getColumn()) {
            return -1;
        }
        if (getColumn() > loc.getColumn()) {
            return 1;
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CellValueRecordInterface)) {
            return false;
        }
        CellValueRecordInterface loc = (CellValueRecordInterface) obj;

        if ((getRow() == loc.getRow()) && (getColumn() == loc.getColumn())) {
            return true;
        }
        return false;
    }

    /**
     * called by the constructor, should set class level fields. Should throw
     * runtime exception for bad/icomplete data.
     *
     * @param data raw data
     * @param size size of data
     */

    @Override
    protected void fillFields(byte[] data, short size, int offset) {
        // field_1_row = LittleEndian.getShort(data, 0 + offset);
        field_1_row = LittleEndian.getUShort(data, 0 + offset);
        field_2_col = LittleEndian.getShort(data, 2 + offset);
        field_3_xf = LittleEndian.getShort(data, 4 + offset);
        field_4_value = LittleEndian.getDouble(data, 6 + offset);
    }

    public short getColumn() {
        return field_2_col;
    }

    @Override
    public int getRecordSize() {
        return 18;
    }

    // public short getRow()
    public int getRow() {
        return field_1_row;
    }

    @Override
    public short getSid() {
        return sid;
    }

    /**
     * get the value for the cell
     *
     * @return double representing the value
     */

    public double getValue() {
        return field_4_value;
    }

    /**
     * get the index to the ExtendedFormat
     *
     * @see org.apache.poi.hssf.record.ExtendedFormatRecord
     * @return index to the XF record
     */

    public short getXFIndex() {
        return field_3_xf;
    }

    public boolean isAfter(CellValueRecordInterface i) {
        if (getRow() < i.getRow()) {
            return false;
        }
        if ((getRow() == i.getRow()) && (getColumn() < i.getColumn())) {
            return false;
        }
        if ((getRow() == i.getRow()) && (getColumn() == i.getColumn())) {
            return false;
        }
        return true;
    }

    public boolean isBefore(CellValueRecordInterface i) {
        if (getRow() > i.getRow()) {
            return false;
        }
        if ((getRow() == i.getRow()) && (getColumn() > i.getColumn())) {
            return false;
        }
        if ((getRow() == i.getRow()) && (getColumn() == i.getColumn())) {
            return false;
        }
        return true;
    }

    public boolean isEqual(CellValueRecordInterface i) {
        return ((getRow() == i.getRow()) && (getColumn() == i.getColumn()));
    }

    @Override
    public boolean isInValueSection() {
        return true;
    }

    @Override
    public boolean isValue() {
        return true;
    }

    /**
     * called by the class that is responsible for writing this sucker.
     * Subclasses should implement this so that their data is passed back in a
     * byte array.
     *
     * @return byte array containing instance data
     */

    @Override
    public int serialize(int offset, byte[] data) {
        LittleEndian.putShort(data, 0 + offset, sid);
        LittleEndian.putShort(data, 2 + offset, (short) 14);
        // LittleEndian.putShort(data, 4 + offset, getRow());
        LittleEndian.putShort(data, 4 + offset, (short) getRow());
        LittleEndian.putShort(data, 6 + offset, getColumn());
        LittleEndian.putShort(data, 8 + offset, getXFIndex());
        LittleEndian.putDouble(data, 10 + offset, getValue());
        return getRecordSize();
    }

    public void setColumn(short col) {
        field_2_col = col;
    }

    // public void setRow(short row)
    public void setRow(int row) {
        field_1_row = row;
    }

    /**
     * set the value for the cell
     *
     * @param value double representing the value
     */

    public void setValue(double value) {
        field_4_value = value;
    }

    /**
     * set the index to the ExtendedFormat
     *
     * @see org.apache.poi.hssf.record.ExtendedFormatRecord
     * @param xf index to the XF record
     */

    public void setXFIndex(short xf) {
        field_3_xf = xf;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[NUMBER]\n");
        buffer.append("    .row            = ").append(Integer.toHexString(getRow())).append("\n");
        buffer.append("    .col            = ").append(Integer.toHexString(getColumn())).append("\n");
        buffer.append("    .xfindex        = ").append(Integer.toHexString(getXFIndex())).append("\n");
        buffer.append("    .value          = ").append(getValue()).append("\n");
        buffer.append("[/NUMBER]\n");
        return buffer.toString();
    }

    /**
     * called by constructor, should throw runtime exception in the event of a
     * record passed with a differing ID.
     *
     * @param id alleged id for this record
     */

    @Override
    protected void validateSid(short id) {
        if (id != sid) {
            throw new RecordFormatException("NOT A Number RECORD");
        }
    }
}

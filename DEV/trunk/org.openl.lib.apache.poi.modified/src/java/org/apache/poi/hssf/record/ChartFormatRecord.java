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

package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.BitField;

/**
 * Class ChartFormatRecord
 *
 *
 * @author Glen Stampoultzis (glens at apache.org)
 * @version %I%, %G%
 */

public class ChartFormatRecord extends Record {
    public static final short sid = 0x1014;

    // ignored?
    private int field1_x_position; // lower left
    private int field2_y_position; // lower left
    private int field3_width;
    private int field4_height;
    private short field5_grbit;
    private BitField varyDisplayPattern = new BitField(0x01);

    public ChartFormatRecord() {
    }

    /**
     * Constructs a ChartFormatRecord record and sets its fields appropriately.
     *
     * @param id id must equal the sid or an exception will be throw upon
     *            validation
     * @param size the size of the data area of the record
     * @param data data of the record (should not contain sid/len)
     */

    public ChartFormatRecord(short id, short size, byte[] data) {
        super(id, size, data);
    }

    /**
     * Constructs a ChartFormatRecord record and sets its fields appropriately.
     *
     * @param id id must equal the sid or an exception will be throw upon
     *            validation
     * @param size the size of the data area of the record
     * @param data data of the record (should not contain sid/len)
     * @param offset of the record's data
     */

    public ChartFormatRecord(short id, short size, byte[] data, int offset) {
        super(id, size, data, offset);
    }

    @Override
    protected void fillFields(byte[] data, short size, int offset) {
        field1_x_position = LittleEndian.getInt(data, 0 + offset);
        field2_y_position = LittleEndian.getInt(data, 4 + offset);
        field3_width = LittleEndian.getInt(data, 8 + offset);
        field4_height = LittleEndian.getInt(data, 12 + offset);
        field5_grbit = LittleEndian.getShort(data, 16 + offset);
    }

    public int getHeight() {
        return field4_height;
    }

    @Override
    public int getRecordSize() {
        return 22;
    }

    @Override
    public short getSid() {
        return sid;
    }

    public boolean getVaryDisplayPattern() {
        return varyDisplayPattern.isSet(field5_grbit);
    }

    public int getWidth() {
        return field3_width;
    }

    public int getXPosition() {
        return field1_x_position;
    }

    public int getYPosition() {
        return field2_y_position;
    }

    @Override
    public int serialize(int offset, byte[] data) {
        LittleEndian.putShort(data, 0 + offset, sid);
        LittleEndian.putShort(data, 2 + offset, ((short) 22)); // 22 byte
                                                                // length
        LittleEndian.putInt(data, 4 + offset, getXPosition());
        LittleEndian.putInt(data, 8 + offset, getYPosition());
        LittleEndian.putInt(data, 12 + offset, getWidth());
        LittleEndian.putInt(data, 16 + offset, getHeight());
        LittleEndian.putShort(data, 20 + offset, field5_grbit);
        return getRecordSize();
    }

    public void setHeight(int height) {
        field4_height = height;
    }

    public void setVaryDisplayPattern(boolean value) {
        field5_grbit = varyDisplayPattern.setShortBoolean(field5_grbit, value);
    }

    public void setWidth(int width) {
        field3_width = width;
    }

    public void setXPosition(int xPosition) {
        field1_x_position = xPosition;
    }

    public void setYPosition(int yPosition) {
        field2_y_position = yPosition;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[CHARTFORMAT]\n");
        buffer.append("    .xPosition       = ").append(getXPosition()).append("\n");
        buffer.append("    .yPosition       = ").append(getYPosition()).append("\n");
        buffer.append("    .width           = ").append(getWidth()).append("\n");
        buffer.append("    .height          = ").append(getHeight()).append("\n");
        buffer.append("    .grBit           = ").append(Integer.toHexString(field5_grbit)).append("\n");
        buffer.append("[/CHARTFORMAT]\n");
        return buffer.toString();
    }

    @Override
    protected void validateSid(short id) {
        if (id != sid) {
            throw new RecordFormatException("NOT A CHARTFORMAT RECORD");
        }
    }
}

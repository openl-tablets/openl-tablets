/* ====================================================================
   Copyright 2003-2004   Apache Software Foundation

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
 * Ptg.java
 *
 * Created on October 28, 2001, 6:30 PM
 */
package org.apache.poi.hssf.record.formula;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.hssf.model.Workbook;

/**
 * @author Glen Stampoultzis (glens at apache.org)
 */
public class MemFuncPtg extends ControlPtg {

    public final static byte sid = 0x29;
    private short field_1_len_ref_subexpression = 0;

    public MemFuncPtg() {
        // Required for clone methods
    }

    /**
     * Creates new function pointer from a byte array usually called while
     * reading an excel file.
     */
    public MemFuncPtg(byte[] data, int offset) {
        offset++;
        field_1_len_ref_subexpression = LittleEndian.getShort(data, offset + 0);
    }

    @Override
    public Object clone() {
        MemFuncPtg ptg = new MemFuncPtg();
        ptg.field_1_len_ref_subexpression = field_1_len_ref_subexpression;
        return ptg;
    }

    @Override
    public byte getDefaultOperandClass() {
        return 0;
    }

    public int getLenRefSubexpression() {
        return field_1_len_ref_subexpression;
    }

    public int getNumberOfOperands() {
        return field_1_len_ref_subexpression;
    }

    @Override
    public int getSize() {
        return 3;
    }

    public void setLenRefSubexpression(int len) {
        field_1_len_ref_subexpression = (short) len;
    }

    @Override
    public String toFormulaString(Workbook book) {
        return "";
    }

    @Override
    public void writeBytes(byte[] array, int offset) {
        array[offset + 0] = sid;
        LittleEndian.putShort(array, offset + 1, field_1_len_ref_subexpression);
    }

}
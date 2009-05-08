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

package org.apache.poi.hwpf.model;

import java.util.HashSet;
import java.io.IOException;

import org.apache.poi.hwpf.model.io.*;

import org.apache.poi.hwpf.model.types.FIBAbstractType;

/**
 *
 * @author andy
 */
public class FileInformationBlock extends FIBAbstractType implements Cloneable {

    FIBLongHandler _longHandler;
    FIBShortHandler _shortHandler;
    FIBFieldHandler _fieldHandler;

    /** Creates a new instance of FileInformationBlock */
    public FileInformationBlock(byte[] mainDocument) {
        fillFields(mainDocument, 0);
    }

    public void clearOffsetsSizes() {
        _fieldHandler.clearFields();
    }

    public void fillVariableFields(byte[] mainDocument, byte[] tableStream) {
        HashSet fieldSet = new HashSet();
        fieldSet.add(new Integer(FIBFieldHandler.STSHF));
        fieldSet.add(new Integer(FIBFieldHandler.CLX));
        fieldSet.add(new Integer(FIBFieldHandler.DOP));
        fieldSet.add(new Integer(FIBFieldHandler.PLCFBTECHPX));
        fieldSet.add(new Integer(FIBFieldHandler.PLCFBTEPAPX));
        fieldSet.add(new Integer(FIBFieldHandler.PLCFSED));
        fieldSet.add(new Integer(FIBFieldHandler.PLCFLST));
        fieldSet.add(new Integer(FIBFieldHandler.PLFLFO));
        fieldSet.add(new Integer(FIBFieldHandler.PLCFFLDMOM));
        fieldSet.add(new Integer(FIBFieldHandler.STTBFFFN));
        fieldSet.add(new Integer(FIBFieldHandler.MODIFIED));

        _shortHandler = new FIBShortHandler(mainDocument);
        _longHandler = new FIBLongHandler(mainDocument, FIBShortHandler.START + _shortHandler.sizeInBytes());
        _fieldHandler = new FIBFieldHandler(mainDocument, FIBShortHandler.START + _shortHandler.sizeInBytes()
                + _longHandler.sizeInBytes(), tableStream, fieldSet, true);
    }

    public int getFcClx() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.CLX);
    }

    public int getFcDop() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.DOP);
    }

    public int getFcPlcfbteChpx() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFBTECHPX);
    }

    public int getFcPlcfbtePapx() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFBTEPAPX);
    }

    public int getFcPlcffldMom() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFFLDMOM);
    }

    public int getFcPlcfLst() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFLST);
    }

    public int getFcPlcfsed() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLCFSED);
    }

    public int getFcPlfLfo() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLFLFO);
    }

    public int getFcStshf() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.STSHF);
    }

    public int getFcSttbfffn() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.STTBFFFN);
    }

    public int getLcbClx() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.CLX);
    }

    public int getLcbDop() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.DOP);
    }

    public int getLcbPlcfbteChpx() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFBTECHPX);
    }

    public int getLcbPlcfbtePapx() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFBTEPAPX);
    }

    public int getLcbPlcffldMom() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFFLDMOM);
    }

    public int getLcbPlcfLst() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFLST);
    }

    public int getLcbPlcfsed() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLCFSED);
    }

    public int getLcbPlfLfo() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLFLFO);
    }

    public int getLcbStshf() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.STSHF);
    }

    public int getLcbSttbfffn() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.STTBFFFN);
    }

    public int getModifiedHigh() {
        return _fieldHandler.getFieldSize(FIBFieldHandler.PLFLFO);
    }

    public int getModifiedLow() {
        return _fieldHandler.getFieldOffset(FIBFieldHandler.PLFLFO);
    }

    @Override
    public int getSize() {
        return super.getSize() + _shortHandler.sizeInBytes() + _longHandler.sizeInBytes() + _fieldHandler.sizeInBytes();
    }
    // public Object clone()
    // {
    // try
    // {
    // return super.clone();
    // }
    // catch (CloneNotSupportedException e)
    // {
    // e.printStackTrace();
    // return null;
    // }
    // }

    public void setCbMac(int cbMac) {
        _longHandler.setLong(FIBLongHandler.CBMAC, cbMac);
    }

    public void setFcClx(int fcClx) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.CLX, fcClx);
    }

    public void setFcDop(int fcDop) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.DOP, fcDop);
    }

    public void setFcPlcfbteChpx(int fcPlcfBteChpx) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.PLCFBTECHPX, fcPlcfBteChpx);
    }

    public void setFcPlcfbtePapx(int fcPlcfBtePapx) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.PLCFBTEPAPX, fcPlcfBtePapx);
    }

    public void setFcPlcfLst(int fcPlcfLst) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.PLCFLST, fcPlcfLst);
    }

    public void setFcPlcfsed(int fcPlcfSed) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.PLCFSED, fcPlcfSed);
    }

    public void setFcPlfLfo(int fcPlfLfo) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.PLFLFO, fcPlfLfo);
    }

    public void setFcStshf(int fcStshf) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.STSHF, fcStshf);
    }

    public void setFcSttbfffn(int fcSttbFffn) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.STTBFFFN, fcSttbFffn);
    }

    public void setLcbClx(int lcbClx) {
        _fieldHandler.setFieldSize(FIBFieldHandler.CLX, lcbClx);
    }

    public void setLcbDop(int lcbDop) {
        _fieldHandler.setFieldSize(FIBFieldHandler.DOP, lcbDop);
    }

    public void setLcbPlcfbteChpx(int lcbPlcfBteChpx) {
        _fieldHandler.setFieldSize(FIBFieldHandler.PLCFBTECHPX, lcbPlcfBteChpx);
    }

    public void setLcbPlcfbtePapx(int lcbPlcfBtePapx) {
        _fieldHandler.setFieldSize(FIBFieldHandler.PLCFBTEPAPX, lcbPlcfBtePapx);
    }

    public void setLcbPlcfLst(int lcbPlcfLst) {
        _fieldHandler.setFieldSize(FIBFieldHandler.PLCFLST, lcbPlcfLst);
    }

    public void setLcbPlcfsed(int lcbPlcfSed) {
        _fieldHandler.setFieldSize(FIBFieldHandler.PLCFSED, lcbPlcfSed);
    }

    public void setLcbPlfLfo(int lcbPlfLfo) {
        _fieldHandler.setFieldSize(FIBFieldHandler.PLFLFO, lcbPlfLfo);
    }

    public void setLcbStshf(int lcbStshf) {
        _fieldHandler.setFieldSize(FIBFieldHandler.STSHF, lcbStshf);
    }

    public void setLcbSttbfffn(int lcbSttbFffn) {
        _fieldHandler.setFieldSize(FIBFieldHandler.STTBFFFN, lcbSttbFffn);
    }

    public void setModifiedHigh(int modifiedHigh) {
        _fieldHandler.setFieldSize(FIBFieldHandler.PLFLFO, modifiedHigh);
    }

    public void setModifiedLow(int modifiedLow) {
        _fieldHandler.setFieldOffset(FIBFieldHandler.PLFLFO, modifiedLow);
    }

    public void writeTo(byte[] mainStream, HWPFOutputStream tableStream) throws IOException {
        // HWPFOutputStream mainDocument = sys.getStream("WordDocument");
        // HWPFOutputStream tableStream = sys.getStream("1Table");

        super.serialize(mainStream, 0);

        int size = super.getSize();
        _shortHandler.serialize(mainStream);
        _longHandler.serialize(mainStream, size + _shortHandler.sizeInBytes());
        _fieldHandler.writeTo(mainStream, super.getSize() + _shortHandler.sizeInBytes() + _longHandler.sizeInBytes(),
                tableStream);

    }
}

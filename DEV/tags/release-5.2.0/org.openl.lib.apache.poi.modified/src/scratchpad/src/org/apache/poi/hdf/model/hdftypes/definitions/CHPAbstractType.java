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

package org.apache.poi.hdf.model.hdftypes.definitions;

import org.apache.poi.util.BitField;
import org.apache.poi.hdf.model.hdftypes.HDFType;

/**
 * Character Properties. NOTE: This source is automatically generated please do
 * not modify this file. Either subclass or remove the record in
 * src/records/definitions.
 *
 * @author S. Ryan Ackley
 */
public abstract class CHPAbstractType implements HDFType {

    private short field_1_chse;
    private int field_2_format_flags;
    private BitField fBold = new BitField(0x0001);
    private BitField fItalic = new BitField(0x0002);
    private BitField fRMarkDel = new BitField(0x0004);
    private BitField fOutline = new BitField(0x0008);
    private BitField fFldVanish = new BitField(0x0010);
    private BitField fSmallCaps = new BitField(0x0020);
    private BitField fCaps = new BitField(0x0040);
    private BitField fVanish = new BitField(0x0080);
    private BitField fRMark = new BitField(0x0100);
    private BitField fSpec = new BitField(0x0200);
    private BitField fStrike = new BitField(0x0400);
    private BitField fObj = new BitField(0x0800);
    private BitField fShadow = new BitField(0x1000);
    private BitField fLowerCase = new BitField(0x2000);
    private BitField fData = new BitField(0x4000);
    private BitField fOle2 = new BitField(0x8000);
    private int field_3_format_flags1;
    private BitField fEmboss = new BitField(0x0001);
    private BitField fImprint = new BitField(0x0002);
    private BitField fDStrike = new BitField(0x0004);
    private BitField fUsePgsuSettings = new BitField(0x0008);
    private int field_4_ftcAscii;
    private int field_5_ftcFE;
    private int field_6_ftcOther;
    private int field_7_hps;
    private int field_8_dxaSpace;
    private byte field_9_iss;
    private byte field_10_kul;
    private byte field_11_ico;
    private int field_12_hpsPos;
    private int field_13_lidDefault;
    private int field_14_lidFE;
    private byte field_15_idctHint;
    private int field_16_wCharScale;
    private int field_17_fcPic;
    private int field_18_fcObj;
    private int field_19_lTagObj;
    private int field_20_ibstRMark;
    private int field_21_ibstRMarkDel;
    private short[] field_22_dttmRMark;
    private short[] field_23_dttmRMarkDel;
    private int field_24_istd;
    private int field_25_baseIstd;
    private int field_26_ftcSym;
    private int field_27_xchSym;
    private int field_28_idslRMReason;
    private int field_29_idslReasonDel;
    private byte field_30_ysr;
    private byte field_31_chYsr;
    private int field_32_hpsKern;
    private short field_33_Highlight;
    private BitField icoHighlight = new BitField(0x001f);
    private BitField fHighlight = new BitField(0x0020);
    private BitField kcd = new BitField(0x01c0);
    private BitField fNavHighlight = new BitField(0x0200);
    private BitField fChsDiff = new BitField(0x0400);
    private BitField fMacChs = new BitField(0x0800);
    private BitField fFtcAsciSym = new BitField(0x1000);
    private short field_34_fPropMark;
    private int field_35_ibstPropRMark;
    private int field_36_dttmPropRMark;
    private byte field_37_sfxtText;
    private byte field_38_fDispFldRMark;
    private int field_39_ibstDispFldRMark;
    private int field_40_dttmDispFldRMark;
    private byte[] field_41_xstDispFldRMark;
    private int field_42_shd;
    private short[] field_43_brc;

    public CHPAbstractType() {

    }

    /**
     * Get the baseIstd field for the CHP record.
     */
    public int getBaseIstd() {
        return field_25_baseIstd;
    }

    /**
     * Get the brc field for the CHP record.
     */
    public short[] getBrc() {
        return field_43_brc;
    }

    /**
     * Get the chse field for the CHP record.
     */
    public short getChse() {
        return field_1_chse;
    }

    /**
     * Get the chYsr field for the CHP record.
     */
    public byte getChYsr() {
        return field_31_chYsr;
    }

    /**
     * Get the dttmDispFldRMark field for the CHP record.
     */
    public int getDttmDispFldRMark() {
        return field_40_dttmDispFldRMark;
    }

    /**
     * Get the dttmPropRMark field for the CHP record.
     */
    public int getDttmPropRMark() {
        return field_36_dttmPropRMark;
    }

    /**
     * Get the dttmRMark field for the CHP record.
     */
    public short[] getDttmRMark() {
        return field_22_dttmRMark;
    }

    /**
     * Get the dttmRMarkDel field for the CHP record.
     */
    public short[] getDttmRMarkDel() {
        return field_23_dttmRMarkDel;
    }

    /**
     * Get the dxaSpace field for the CHP record.
     */
    public int getDxaSpace() {
        return field_8_dxaSpace;
    }

    /**
     * Get the fcObj field for the CHP record.
     */
    public int getFcObj() {
        return field_18_fcObj;
    }

    /**
     * Get the fcPic field for the CHP record.
     */
    public int getFcPic() {
        return field_17_fcPic;
    }

    /**
     * Get the fDispFldRMark field for the CHP record.
     */
    public byte getFDispFldRMark() {
        return field_38_fDispFldRMark;
    }

    /**
     * Get the format_flags field for the CHP record.
     */
    public int getFormat_flags() {
        return field_2_format_flags;
    }

    /**
     * Get the format_flags1 field for the CHP record.
     */
    public int getFormat_flags1() {
        return field_3_format_flags1;
    }

    /**
     * Get the fPropMark field for the CHP record.
     */
    public short getFPropMark() {
        return field_34_fPropMark;
    }

    /**
     * Get the ftcAscii field for the CHP record.
     */
    public int getFtcAscii() {
        return field_4_ftcAscii;
    }

    /**
     * Get the ftcFE field for the CHP record.
     */
    public int getFtcFE() {
        return field_5_ftcFE;
    }

    /**
     * Get the ftcOther field for the CHP record.
     */
    public int getFtcOther() {
        return field_6_ftcOther;
    }

    /**
     * Get the ftcSym field for the CHP record.
     */
    public int getFtcSym() {
        return field_26_ftcSym;
    }

    /**
     * Get the Highlight field for the CHP record.
     */
    public short getHighlight() {
        return field_33_Highlight;
    }

    /**
     * Get the hps field for the CHP record.
     */
    public int getHps() {
        return field_7_hps;
    }

    /**
     * Get the hpsKern field for the CHP record.
     */
    public int getHpsKern() {
        return field_32_hpsKern;
    }

    /**
     * Get the hpsPos field for the CHP record.
     */
    public int getHpsPos() {
        return field_12_hpsPos;
    }

    /**
     * Get the ibstDispFldRMark field for the CHP record.
     */
    public int getIbstDispFldRMark() {
        return field_39_ibstDispFldRMark;
    }

    /**
     * Get the ibstPropRMark field for the CHP record.
     */
    public int getIbstPropRMark() {
        return field_35_ibstPropRMark;
    }

    /**
     * Get the ibstRMark field for the CHP record.
     */
    public int getIbstRMark() {
        return field_20_ibstRMark;
    }

    /**
     * Get the ibstRMarkDel field for the CHP record.
     */
    public int getIbstRMarkDel() {
        return field_21_ibstRMarkDel;
    }

    /**
     * Get the ico field for the CHP record.
     */
    public byte getIco() {
        return field_11_ico;
    }

    /**
     *
     * @return the icoHighlight field value.
     */
    public byte getIcoHighlight() {
        return (byte) icoHighlight.getValue(field_33_Highlight);

    }

    /**
     * Get the idctHint field for the CHP record.
     */
    public byte getIdctHint() {
        return field_15_idctHint;
    }

    /**
     * Get the idslReasonDel field for the CHP record.
     */
    public int getIdslReasonDel() {
        return field_29_idslReasonDel;
    }

    /**
     * Get the idslRMReason field for the CHP record.
     */
    public int getIdslRMReason() {
        return field_28_idslRMReason;
    }

    /**
     * Get the iss field for the CHP record.
     */
    public byte getIss() {
        return field_9_iss;
    }

    /**
     * Get the istd field for the CHP record.
     */
    public int getIstd() {
        return field_24_istd;
    }

    /**
     *
     * @return the kcd field value.
     */
    public byte getKcd() {
        return (byte) kcd.getValue(field_33_Highlight);

    }

    /**
     * Get the kul field for the CHP record.
     */
    public byte getKul() {
        return field_10_kul;
    }

    /**
     * Get the lidDefault field for the CHP record.
     */
    public int getLidDefault() {
        return field_13_lidDefault;
    }

    /**
     * Get the lidFE field for the CHP record.
     */
    public int getLidFE() {
        return field_14_lidFE;
    }

    /**
     * Get the lTagObj field for the CHP record.
     */
    public int getLTagObj() {
        return field_19_lTagObj;
    }

    /**
     * Get the sfxtText field for the CHP record.
     */
    public byte getSfxtText() {
        return field_37_sfxtText;
    }

    /**
     * Get the shd field for the CHP record.
     */
    public int getShd() {
        return field_42_shd;
    }

    /**
     * Size of record (exluding 4 byte header)
     */
    public int getSize() {
        return 4 + 2 + 2 + 2 + 2 + 2 + 2 + 2 + 4 + 1 + 1 + 1 + 2 + 2 + 2 + 1 + 2 + 4 + 4 + 4 + 2 + 2 + 4 + 4 + 2 + 2
                + 2 + 2 + 2 + 2 + 1 + 1 + 2 + 2 + 2 + 2 + 4 + 1 + 1 + 2 + 4 + 32 + 2 + 4;
    }

    /**
     * Get the wCharScale field for the CHP record.
     */
    public int getWCharScale() {
        return field_16_wCharScale;
    }

    /**
     * Get the xchSym field for the CHP record.
     */
    public int getXchSym() {
        return field_27_xchSym;
    }

    /**
     * Get the xstDispFldRMark field for the CHP record.
     */
    public byte[] getXstDispFldRMark() {
        return field_41_xstDispFldRMark;
    }

    /**
     * Get the ysr field for the CHP record.
     */
    public byte getYsr() {
        return field_30_ysr;
    }

    /**
     *
     * @return the fBold field value.
     */
    public boolean isFBold() {
        return fBold.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fCaps field value.
     */
    public boolean isFCaps() {
        return fCaps.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fChsDiff field value.
     */
    public boolean isFChsDiff() {
        return fChsDiff.isSet(field_33_Highlight);

    }

    /**
     *
     * @return the fData field value.
     */
    public boolean isFData() {
        return fData.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fDStrike field value.
     */
    public boolean isFDStrike() {
        return fDStrike.isSet(field_3_format_flags1);

    }

    /**
     *
     * @return the fEmboss field value.
     */
    public boolean isFEmboss() {
        return fEmboss.isSet(field_3_format_flags1);

    }

    /**
     *
     * @return the fFldVanish field value.
     */
    public boolean isFFldVanish() {
        return fFldVanish.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fFtcAsciSym field value.
     */
    public boolean isFFtcAsciSym() {
        return fFtcAsciSym.isSet(field_33_Highlight);

    }

    /**
     *
     * @return the fHighlight field value.
     */
    public boolean isFHighlight() {
        return fHighlight.isSet(field_33_Highlight);

    }

    /**
     *
     * @return the fImprint field value.
     */
    public boolean isFImprint() {
        return fImprint.isSet(field_3_format_flags1);

    }

    /**
     *
     * @return the fItalic field value.
     */
    public boolean isFItalic() {
        return fItalic.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fLowerCase field value.
     */
    public boolean isFLowerCase() {
        return fLowerCase.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fMacChs field value.
     */
    public boolean isFMacChs() {
        return fMacChs.isSet(field_33_Highlight);

    }

    /**
     *
     * @return the fNavHighlight field value.
     */
    public boolean isFNavHighlight() {
        return fNavHighlight.isSet(field_33_Highlight);

    }

    /**
     *
     * @return the fObj field value.
     */
    public boolean isFObj() {
        return fObj.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fOle2 field value.
     */
    public boolean isFOle2() {
        return fOle2.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fOutline field value.
     */
    public boolean isFOutline() {
        return fOutline.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fRMark field value.
     */
    public boolean isFRMark() {
        return fRMark.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fRMarkDel field value.
     */
    public boolean isFRMarkDel() {
        return fRMarkDel.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fShadow field value.
     */
    public boolean isFShadow() {
        return fShadow.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fSmallCaps field value.
     */
    public boolean isFSmallCaps() {
        return fSmallCaps.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fSpec field value.
     */
    public boolean isFSpec() {
        return fSpec.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fStrike field value.
     */
    public boolean isFStrike() {
        return fStrike.isSet(field_2_format_flags);

    }

    /**
     *
     * @return the fUsePgsuSettings field value.
     */
    public boolean isFUsePgsuSettings() {
        return fUsePgsuSettings.isSet(field_3_format_flags1);

    }

    /**
     *
     * @return the fVanish field value.
     */
    public boolean isFVanish() {
        return fVanish.isSet(field_2_format_flags);

    }

    /**
     * Set the baseIstd field for the CHP record.
     */
    public void setBaseIstd(int field_25_baseIstd) {
        this.field_25_baseIstd = field_25_baseIstd;
    }

    /**
     * Set the brc field for the CHP record.
     */
    public void setBrc(short[] field_43_brc) {
        this.field_43_brc = field_43_brc;
    }

    /**
     * Set the chse field for the CHP record.
     */
    public void setChse(short field_1_chse) {
        this.field_1_chse = field_1_chse;
    }

    /**
     * Set the chYsr field for the CHP record.
     */
    public void setChYsr(byte field_31_chYsr) {
        this.field_31_chYsr = field_31_chYsr;
    }

    /**
     * Set the dttmDispFldRMark field for the CHP record.
     */
    public void setDttmDispFldRMark(int field_40_dttmDispFldRMark) {
        this.field_40_dttmDispFldRMark = field_40_dttmDispFldRMark;
    }

    /**
     * Set the dttmPropRMark field for the CHP record.
     */
    public void setDttmPropRMark(int field_36_dttmPropRMark) {
        this.field_36_dttmPropRMark = field_36_dttmPropRMark;
    }

    /**
     * Set the dttmRMark field for the CHP record.
     */
    public void setDttmRMark(short[] field_22_dttmRMark) {
        this.field_22_dttmRMark = field_22_dttmRMark;
    }

    /**
     * Set the dttmRMarkDel field for the CHP record.
     */
    public void setDttmRMarkDel(short[] field_23_dttmRMarkDel) {
        this.field_23_dttmRMarkDel = field_23_dttmRMarkDel;
    }

    /**
     * Set the dxaSpace field for the CHP record.
     */
    public void setDxaSpace(int field_8_dxaSpace) {
        this.field_8_dxaSpace = field_8_dxaSpace;
    }

    /**
     * Sets the fBold field value.
     *
     */
    public void setFBold(boolean value) {
        field_2_format_flags = fBold.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fCaps field value.
     *
     */
    public void setFCaps(boolean value) {
        field_2_format_flags = fCaps.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fChsDiff field value.
     *
     */
    public void setFChsDiff(boolean value) {
        field_33_Highlight = (short) fChsDiff.setBoolean(field_33_Highlight, value);

    }

    /**
     * Set the fcObj field for the CHP record.
     */
    public void setFcObj(int field_18_fcObj) {
        this.field_18_fcObj = field_18_fcObj;
    }

    /**
     * Set the fcPic field for the CHP record.
     */
    public void setFcPic(int field_17_fcPic) {
        this.field_17_fcPic = field_17_fcPic;
    }

    /**
     * Sets the fData field value.
     *
     */
    public void setFData(boolean value) {
        field_2_format_flags = fData.setBoolean(field_2_format_flags, value);

    }

    /**
     * Set the fDispFldRMark field for the CHP record.
     */
    public void setFDispFldRMark(byte field_38_fDispFldRMark) {
        this.field_38_fDispFldRMark = field_38_fDispFldRMark;
    }

    /**
     * Sets the fDStrike field value.
     *
     */
    public void setFDStrike(boolean value) {
        field_3_format_flags1 = fDStrike.setBoolean(field_3_format_flags1, value);

    }

    /**
     * Sets the fEmboss field value.
     *
     */
    public void setFEmboss(boolean value) {
        field_3_format_flags1 = fEmboss.setBoolean(field_3_format_flags1, value);

    }

    /**
     * Sets the fFldVanish field value.
     *
     */
    public void setFFldVanish(boolean value) {
        field_2_format_flags = fFldVanish.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fFtcAsciSym field value.
     *
     */
    public void setFFtcAsciSym(boolean value) {
        field_33_Highlight = (short) fFtcAsciSym.setBoolean(field_33_Highlight, value);

    }

    /**
     * Sets the fHighlight field value.
     *
     */
    public void setFHighlight(boolean value) {
        field_33_Highlight = (short) fHighlight.setBoolean(field_33_Highlight, value);

    }

    /**
     * Sets the fImprint field value.
     *
     */
    public void setFImprint(boolean value) {
        field_3_format_flags1 = fImprint.setBoolean(field_3_format_flags1, value);

    }

    /**
     * Sets the fItalic field value.
     *
     */
    public void setFItalic(boolean value) {
        field_2_format_flags = fItalic.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fLowerCase field value.
     *
     */
    public void setFLowerCase(boolean value) {
        field_2_format_flags = fLowerCase.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fMacChs field value.
     *
     */
    public void setFMacChs(boolean value) {
        field_33_Highlight = (short) fMacChs.setBoolean(field_33_Highlight, value);

    }

    /**
     * Sets the fNavHighlight field value.
     *
     */
    public void setFNavHighlight(boolean value) {
        field_33_Highlight = (short) fNavHighlight.setBoolean(field_33_Highlight, value);

    }

    /**
     * Sets the fObj field value.
     *
     */
    public void setFObj(boolean value) {
        field_2_format_flags = fObj.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fOle2 field value.
     *
     */
    public void setFOle2(boolean value) {
        field_2_format_flags = fOle2.setBoolean(field_2_format_flags, value);

    }

    /**
     * Set the format_flags field for the CHP record.
     */
    public void setFormat_flags(int field_2_format_flags) {
        this.field_2_format_flags = field_2_format_flags;
    }

    /**
     * Set the format_flags1 field for the CHP record.
     */
    public void setFormat_flags1(int field_3_format_flags1) {
        this.field_3_format_flags1 = field_3_format_flags1;
    }

    /**
     * Sets the fOutline field value.
     *
     */
    public void setFOutline(boolean value) {
        field_2_format_flags = fOutline.setBoolean(field_2_format_flags, value);

    }

    /**
     * Set the fPropMark field for the CHP record.
     */
    public void setFPropMark(short field_34_fPropMark) {
        this.field_34_fPropMark = field_34_fPropMark;
    }

    /**
     * Sets the fRMark field value.
     *
     */
    public void setFRMark(boolean value) {
        field_2_format_flags = fRMark.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fRMarkDel field value.
     *
     */
    public void setFRMarkDel(boolean value) {
        field_2_format_flags = fRMarkDel.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fShadow field value.
     *
     */
    public void setFShadow(boolean value) {
        field_2_format_flags = fShadow.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fSmallCaps field value.
     *
     */
    public void setFSmallCaps(boolean value) {
        field_2_format_flags = fSmallCaps.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fSpec field value.
     *
     */
    public void setFSpec(boolean value) {
        field_2_format_flags = fSpec.setBoolean(field_2_format_flags, value);

    }

    /**
     * Sets the fStrike field value.
     *
     */
    public void setFStrike(boolean value) {
        field_2_format_flags = fStrike.setBoolean(field_2_format_flags, value);

    }

    /**
     * Set the ftcAscii field for the CHP record.
     */
    public void setFtcAscii(int field_4_ftcAscii) {
        this.field_4_ftcAscii = field_4_ftcAscii;
    }

    /**
     * Set the ftcFE field for the CHP record.
     */
    public void setFtcFE(int field_5_ftcFE) {
        this.field_5_ftcFE = field_5_ftcFE;
    }

    /**
     * Set the ftcOther field for the CHP record.
     */
    public void setFtcOther(int field_6_ftcOther) {
        this.field_6_ftcOther = field_6_ftcOther;
    }

    /**
     * Set the ftcSym field for the CHP record.
     */
    public void setFtcSym(int field_26_ftcSym) {
        this.field_26_ftcSym = field_26_ftcSym;
    }

    /**
     * Sets the fUsePgsuSettings field value.
     *
     */
    public void setFUsePgsuSettings(boolean value) {
        field_3_format_flags1 = fUsePgsuSettings.setBoolean(field_3_format_flags1, value);

    }

    /**
     * Sets the fVanish field value.
     *
     */
    public void setFVanish(boolean value) {
        field_2_format_flags = fVanish.setBoolean(field_2_format_flags, value);

    }

    /**
     * Set the Highlight field for the CHP record.
     */
    public void setHighlight(short field_33_Highlight) {
        this.field_33_Highlight = field_33_Highlight;
    }

    /**
     * Set the hps field for the CHP record.
     */
    public void setHps(int field_7_hps) {
        this.field_7_hps = field_7_hps;
    }

    /**
     * Set the hpsKern field for the CHP record.
     */
    public void setHpsKern(int field_32_hpsKern) {
        this.field_32_hpsKern = field_32_hpsKern;
    }

    /**
     * Set the hpsPos field for the CHP record.
     */
    public void setHpsPos(int field_12_hpsPos) {
        this.field_12_hpsPos = field_12_hpsPos;
    }

    /**
     * Set the ibstDispFldRMark field for the CHP record.
     */
    public void setIbstDispFldRMark(int field_39_ibstDispFldRMark) {
        this.field_39_ibstDispFldRMark = field_39_ibstDispFldRMark;
    }

    /**
     * Set the ibstPropRMark field for the CHP record.
     */
    public void setIbstPropRMark(int field_35_ibstPropRMark) {
        this.field_35_ibstPropRMark = field_35_ibstPropRMark;
    }

    /**
     * Set the ibstRMark field for the CHP record.
     */
    public void setIbstRMark(int field_20_ibstRMark) {
        this.field_20_ibstRMark = field_20_ibstRMark;
    }

    /**
     * Set the ibstRMarkDel field for the CHP record.
     */
    public void setIbstRMarkDel(int field_21_ibstRMarkDel) {
        this.field_21_ibstRMarkDel = field_21_ibstRMarkDel;
    }

    /**
     * Set the ico field for the CHP record.
     */
    public void setIco(byte field_11_ico) {
        this.field_11_ico = field_11_ico;
    }

    /**
     * Sets the icoHighlight field value.
     *
     */
    public void setIcoHighlight(byte value) {
        field_33_Highlight = (short) icoHighlight.setValue(field_33_Highlight, value);

    }

    /**
     * Set the idctHint field for the CHP record.
     */
    public void setIdctHint(byte field_15_idctHint) {
        this.field_15_idctHint = field_15_idctHint;
    }

    /**
     * Set the idslReasonDel field for the CHP record.
     */
    public void setIdslReasonDel(int field_29_idslReasonDel) {
        this.field_29_idslReasonDel = field_29_idslReasonDel;
    }

    /**
     * Set the idslRMReason field for the CHP record.
     */
    public void setIdslRMReason(int field_28_idslRMReason) {
        this.field_28_idslRMReason = field_28_idslRMReason;
    }

    /**
     * Set the iss field for the CHP record.
     */
    public void setIss(byte field_9_iss) {
        this.field_9_iss = field_9_iss;
    }

    /**
     * Set the istd field for the CHP record.
     */
    public void setIstd(int field_24_istd) {
        this.field_24_istd = field_24_istd;
    }

    /**
     * Sets the kcd field value.
     *
     */
    public void setKcd(byte value) {
        field_33_Highlight = (short) kcd.setValue(field_33_Highlight, value);

    }

    /**
     * Set the kul field for the CHP record.
     */
    public void setKul(byte field_10_kul) {
        this.field_10_kul = field_10_kul;
    }

    /**
     * Set the lidDefault field for the CHP record.
     */
    public void setLidDefault(int field_13_lidDefault) {
        this.field_13_lidDefault = field_13_lidDefault;
    }

    /**
     * Set the lidFE field for the CHP record.
     */
    public void setLidFE(int field_14_lidFE) {
        this.field_14_lidFE = field_14_lidFE;
    }

    /**
     * Set the lTagObj field for the CHP record.
     */
    public void setLTagObj(int field_19_lTagObj) {
        this.field_19_lTagObj = field_19_lTagObj;
    }

    /**
     * Set the sfxtText field for the CHP record.
     */
    public void setSfxtText(byte field_37_sfxtText) {
        this.field_37_sfxtText = field_37_sfxtText;
    }

    /**
     * Set the shd field for the CHP record.
     */
    public void setShd(int field_42_shd) {
        this.field_42_shd = field_42_shd;
    }

    /**
     * Set the wCharScale field for the CHP record.
     */
    public void setWCharScale(int field_16_wCharScale) {
        this.field_16_wCharScale = field_16_wCharScale;
    }

    /**
     * Set the xchSym field for the CHP record.
     */
    public void setXchSym(int field_27_xchSym) {
        this.field_27_xchSym = field_27_xchSym;
    }

    /**
     * Set the xstDispFldRMark field for the CHP record.
     */
    public void setXstDispFldRMark(byte[] field_41_xstDispFldRMark) {
        this.field_41_xstDispFldRMark = field_41_xstDispFldRMark;
    }

    /**
     * Set the ysr field for the CHP record.
     */
    public void setYsr(byte field_30_ysr) {
        this.field_30_ysr = field_30_ysr;
    }

} // END OF CLASS


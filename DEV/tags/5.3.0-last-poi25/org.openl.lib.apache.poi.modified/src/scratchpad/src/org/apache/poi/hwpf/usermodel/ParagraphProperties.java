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

package org.apache.poi.hwpf.usermodel;

import org.apache.poi.hwpf.model.types.PAPAbstractType;

public class ParagraphProperties extends PAPAbstractType implements Cloneable {

    public ParagraphProperties() {
        field_21_lspd = new LineSpacingDescriptor();
        field_24_phe = new byte[12];
        field_46_brcTop = new BorderCode();
        field_47_brcLeft = new BorderCode();
        field_48_brcBottom = new BorderCode();
        field_49_brcRight = new BorderCode();
        field_50_brcBetween = new BorderCode();
        field_51_brcBar = new BorderCode();
        field_60_anld = new byte[84];
        field_17_fWidowControl = 1;
        field_21_lspd.setMultiLinespace((short) 1);
        field_21_lspd.setDyaLine((short) 240);
        field_12_ilvl = (byte) 9;
        field_66_rgdxaTab = new int[0];
        field_67_rgtbd = new byte[0];
        field_63_dttmPropRMark = new DateAndTime();

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ParagraphProperties pp = (ParagraphProperties) super.clone();
        pp.field_21_lspd = (LineSpacingDescriptor) field_21_lspd.clone();
        pp.field_24_phe = field_24_phe.clone();
        pp.field_46_brcTop = (BorderCode) field_46_brcTop.clone();
        pp.field_47_brcLeft = (BorderCode) field_47_brcLeft.clone();
        pp.field_48_brcBottom = (BorderCode) field_48_brcBottom.clone();
        pp.field_49_brcRight = (BorderCode) field_49_brcRight.clone();
        pp.field_50_brcBetween = (BorderCode) field_50_brcBetween.clone();
        pp.field_51_brcBar = (BorderCode) field_51_brcBar.clone();
        pp.field_60_anld = field_60_anld.clone();
        return pp;
    }

    public BorderCode getBarBorder() {
        return super.getBrcBar();
    }

    public BorderCode getBottomBorder() {
        return super.getBrcBottom();
    }

    public DropCapSpecifier getDropCap() {
        return super.getDcs();
    }

    public int getFirstLineIndent() {
        return super.getDxaLeft1();
    }

    public int getFontAlignment() {
        return super.getWAlignFont();
    }

    public int getIndentFromLeft() {
        return super.getDxaLeft();
    }

    public int getIndentFromRight() {
        return super.getDxaRight();
    }

    public int getJustification() {
        return super.getJc();
    }

    public BorderCode getLeftBorder() {
        return super.getBrcLeft();
    }

    public LineSpacingDescriptor getLineSpacing() {
        return super.getLspd();
    }

    public BorderCode getRightBorder() {
        return super.getBrcRight();
    }

    public ShadingDescriptor getShading() {
        return super.getShd();
    }

    public int getSpacingAfter() {
        return super.getDyaAfter();
    }

    public int getSpacingBefore() {
        return super.getDyaBefore();
    }

    public BorderCode getTopBorder() {
        return super.getBrcTop();
    }

    public boolean isAutoHyphenated() {
        return super.getFNoAutoHyph() == 0;
    }

    public boolean isBackward() {
        return super.isFBackward();
    }

    public boolean isKinsoku() {
        return super.getFKinsoku() != 0;
    }

    public boolean isLineNotNumbered() {
        return super.getFNoLnn() != 0;
    }

    public boolean isSideBySide() {
        return super.getFSideBySide() != 0;
    }

    public boolean isVertical() {
        return super.isFVertical();
    }

    public boolean isWidowControlled() {
        return super.getFWidowControl() != 0;
    }

    public boolean isWordWrapped() {
        return super.getFWordWrap() != 0;
    }

    public boolean keepOnPage() {
        return super.getFKeep() != 0;
    }

    public boolean keepWithNext() {
        return super.getFKeepFollow() != 0;
    }

    public boolean pageBreakBefore() {
        return super.getFPageBreakBefore() != 0;
    }

    public void setAutoHyphenated(boolean auto) {
        super.setFNoAutoHyph((byte) (!auto ? 1 : 0));
    }

    public void setBackward(boolean bward) {
        super.setFBackward(bward);
    }

    public void setBarBorder(BorderCode bar) {
        super.setBrcBar(bar);
    }

    public void setBottomBorder(BorderCode bottom) {
        super.setBrcBottom(bottom);
    }

    public void setDropCap(DropCapSpecifier dcs) {
        super.setDcs(dcs);
    }

    public void setFirstLineIndent(int first) {
        super.setDxaLeft1(first);
    }

    public void setFontAlignment(int align) {
        super.setWAlignFont(align);
    }

    public void setIndentFromLeft(int dxaLeft) {
        super.setDxaLeft(dxaLeft);
    }

    public void setIndentFromRight(int dxaRight) {
        super.setDxaRight(dxaRight);
    }

    public void setJustification(byte jc) {
        super.setJc(jc);
    }

    public void setKeepOnPage(boolean fKeep) {
        super.setFKeep((byte) (fKeep ? 1 : 0));
    }

    public void setKeepWithNext(boolean fKeepFollow) {
        super.setFKeepFollow((byte) (fKeepFollow ? 1 : 0));
    }

    public void setKinsoku(boolean kinsoku) {
        super.setFKinsoku((byte) (kinsoku ? 1 : 0));
    }

    public void setLeftBorder(BorderCode left) {
        super.setBrcLeft(left);
    }

    public void setLineNotNumbered(boolean fNoLnn) {
        super.setFNoLnn((byte) (fNoLnn ? 1 : 0));
    }

    public void setLineSpacing(LineSpacingDescriptor lspd) {
        super.setLspd(lspd);
    }

    public void setPageBreakBefore(boolean fPageBreak) {
        super.setFPageBreakBefore((byte) (fPageBreak ? 1 : 0));
    }

    public void setRightBorder(BorderCode right) {
        super.setBrcRight(right);
    }

    public void setShading(ShadingDescriptor shd) {
        super.setShd(shd);
    }

    public void setSideBySide(boolean fSideBySide) {
        super.setFSideBySide((byte) (fSideBySide ? 1 : 0));
    }

    public void setSpacingAfter(int after) {
        super.setDyaAfter(after);
    }

    public void setSpacingBefore(int before) {
        super.setDyaBefore(before);
    }

    public void setTopBorder(BorderCode top) {
        super.setBrcTop(top);
    }

    public void setVertical(boolean vertical) {
        super.setFVertical(vertical);
    }

    public void setWidowControl(boolean widowControl) {
        super.setFWidowControl((byte) (widowControl ? 1 : 0));
    }

    public void setWordWrapped(boolean wrap) {
        super.setFWordWrap((byte) (wrap ? 1 : 0));
    }

}

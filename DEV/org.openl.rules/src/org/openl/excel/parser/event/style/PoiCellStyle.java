package org.openl.excel.parser.event.style;

import java.util.Map;

import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;

class PoiCellStyle implements CellStyle {
    private final short index;
    private final ExtendedFormatRecord format;
    private final Map<Integer, FormatRecord> formats;

    PoiCellStyle(short index, ExtendedFormatRecord format, Map<Integer, FormatRecord> formats) {
        this.index = index;
        this.format = format;
        this.formats = formats;
    }

    @Override
    public short getIndex() {
        return index;
    }

    @Override
    public void setDataFormat(short fmt) {

    }

    @Override
    public short getDataFormat() {
        return format.getFormatIndex();
    }

    @Override
    public String getDataFormatString() {
        int formatIndex = getDataFormat();
        if (formatIndex < 0) {
            return null;
        }

        String format;
        if (formatIndex >= HSSFDataFormat.getNumberOfBuiltinBuiltinFormats() || formats.get(formatIndex) != null) {
            format = formats.get(formatIndex).getFormatString();
        } else {
            format = HSSFDataFormat.getBuiltinFormat((short) formatIndex);
        }
        return format;
    }

    @Override
    public void setFont(Font font) {

    }

    @Override
    public short getFontIndex() {
        return format.getFontIndex();
    }

    @Override
    public int getFontIndexAsInt() {
        return format.getFontIndex();
    }

    @Override
    public void setHidden(boolean hidden) {

    }

    @Override
    public boolean getHidden() {
        return format.isHidden();
    }

    @Override
    public void setLocked(boolean locked) {

    }

    @Override
    public boolean getLocked() {
        return format.isLocked();
    }

    @Override
    public void setQuotePrefixed(boolean quotePrefix) {

    }

    @Override
    public boolean getQuotePrefixed() {
        return format.get123Prefix();
    }

    @Override
    public void setAlignment(HorizontalAlignment align) {

    }

    @Override
    public HorizontalAlignment getAlignment() {
        return HorizontalAlignment.forInt(format.getAlignment());
    }

    @Override
    public HorizontalAlignment getAlignmentEnum() {
        return HorizontalAlignment.forInt(format.getAlignment());
    }

    @Override
    public void setWrapText(boolean wrapped) {

    }

    @Override
    public boolean getWrapText() {
        return format.getWrapText();
    }

    @Override
    public void setVerticalAlignment(VerticalAlignment align) {

    }

    @Override
    public VerticalAlignment getVerticalAlignment() {
        return VerticalAlignment.forInt(format.getVerticalAlignment());
    }

    @Override
    public VerticalAlignment getVerticalAlignmentEnum() {
        return VerticalAlignment.forInt(format.getVerticalAlignment());
    }

    @Override
    public void setRotation(short rotation) {

    }

    @Override
    public short getRotation() {
        short rotation = format.getRotation();
        if (rotation == 0xff) {
            // Vertical aligned special case
            return rotation;
        }
        if (rotation > 90) {
            //This is actually the 4th quadrant
            rotation = (short) (90 - rotation);
        }
        return rotation;
    }

    @Override
    public void setIndention(short indent) {

    }

    @Override
    public short getIndention() {
        return format.getIndent();
    }

    @Override
    public void setBorderLeft(BorderStyle border) {

    }

    @Override
    public BorderStyle getBorderLeft() {
        return BorderStyle.valueOf(format.getBorderLeft());
    }

    @Override
    public BorderStyle getBorderLeftEnum() {
        return BorderStyle.valueOf(format.getBorderLeft());
    }

    @Override
    public void setBorderRight(BorderStyle border) {

    }

    @Override
    public BorderStyle getBorderRight() {
        return BorderStyle.valueOf(format.getBorderRight());
    }

    @Override
    public BorderStyle getBorderRightEnum() {
        return BorderStyle.valueOf(format.getBorderRight());
    }

    @Override
    public void setBorderTop(BorderStyle border) {

    }

    @Override
    public BorderStyle getBorderTop() {
        return BorderStyle.valueOf(format.getBorderTop());
    }

    @Override
    public BorderStyle getBorderTopEnum() {
        return BorderStyle.valueOf(format.getBorderTop());
    }

    @Override
    public void setBorderBottom(BorderStyle border) {

    }

    @Override
    public BorderStyle getBorderBottom() {
        return BorderStyle.valueOf(format.getBorderBottom());
    }

    @Override
    public BorderStyle getBorderBottomEnum() {
        return BorderStyle.valueOf(format.getBorderBottom());
    }

    @Override
    public void setLeftBorderColor(short color) {

    }

    @Override
    public short getLeftBorderColor() {
        return format.getLeftBorderPaletteIdx();
    }

    @Override
    public void setRightBorderColor(short color) {

    }

    @Override
    public short getRightBorderColor() {
        return format.getRightBorderPaletteIdx();
    }

    @Override
    public void setTopBorderColor(short color) {

    }

    @Override
    public short getTopBorderColor() {
        return format.getTopBorderPaletteIdx();
    }

    @Override
    public void setBottomBorderColor(short color) {

    }

    @Override
    public short getBottomBorderColor() {
        return format.getBottomBorderPaletteIdx();
    }

    @Override
    public void setFillPattern(FillPatternType fp) {

    }

    @Override
    public FillPatternType getFillPattern() {
        return FillPatternType.forInt(format.getAdtlFillPattern());
    }

    @Override
    public FillPatternType getFillPatternEnum() {
        return FillPatternType.forInt(format.getAdtlFillPattern());
    }

    @Override
    public void setFillBackgroundColor(short bg) {

    }

    @Override
    public short getFillBackgroundColor() {
        final short autoIndex = HSSFColor.HSSFColorPredefined.AUTOMATIC.getIndex();
        short result = format.getFillBackground();

        if (result == autoIndex + 1) {
            return autoIndex;
        }
        return result;
    }

    @Override
    public Color getFillBackgroundColorColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFillForegroundColor(short bg) {

    }

    @Override
    public short getFillForegroundColor() {
        return format.getFillForeground();
    }

    @Override
    public Color getFillForegroundColorColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cloneStyleFrom(CellStyle source) {

    }

    @Override
    public void setShrinkToFit(boolean shrinkToFit) {

    }

    @Override
    public boolean getShrinkToFit() {
        return format.getShrinkToFit();
    }
}

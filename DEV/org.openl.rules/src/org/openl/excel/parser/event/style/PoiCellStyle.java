package org.openl.excel.parser.event.style;

import java.util.EnumMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellPropertyType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellUtil;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PoiCellStyle implements CellStyle {
    @Getter
    private final short index;
    private final ExtendedFormatRecord format;
    private final Map<Integer, FormatRecord> formats;

    @Override
    public void setDataFormat(short fmt) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getDataFormat() {
        return format.getFormatIndex();
    }

    @Override
    public String getDataFormatString() {
        var formatIndex = getDataFormat();
        if (formatIndex < 0) {
            return null;
        }

        // The formats are keyed by an Integer, so the index is widened before the lookup.
        var formatRecord = formats.get((int) formatIndex);
        if (formatRecord != null) {
            return formatRecord.getFormatString();
        }
        return formatIndex < HSSFDataFormat.getNumberOfBuiltinBuiltinFormats()
                ? HSSFDataFormat.getBuiltinFormat(formatIndex)
                : null;
    }

    @Override
    public void setFont(Font font) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public int getFontIndex() {
        return format.getFontIndex();
    }

    @Deprecated
    @Override
    public int getFontIndexAsInt() {
        return format.getFontIndex();
    }

    @Override
    public void setHidden(boolean hidden) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public boolean getHidden() {
        return format.isHidden();
    }

    @Override
    public void setLocked(boolean locked) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public boolean getLocked() {
        return format.isLocked();
    }

    @Override
    public void setQuotePrefixed(boolean quotePrefix) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public boolean getQuotePrefixed() {
        return format.get123Prefix();
    }

    @Override
    public void setAlignment(HorizontalAlignment align) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public HorizontalAlignment getAlignment() {
        return HorizontalAlignment.forInt(format.getAlignment());
    }

    @Override
    public void setWrapText(boolean wrapped) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public boolean getWrapText() {
        return format.getWrapText();
    }

    @Override
    public void setVerticalAlignment(VerticalAlignment align) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public VerticalAlignment getVerticalAlignment() {
        return VerticalAlignment.forInt(format.getVerticalAlignment());
    }

    @Override
    public void setRotation(short rotation) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getRotation() {
        short rotation = format.getRotation();
        if (rotation == 0xff) {
            // Vertical aligned special case
            return rotation;
        }
        if (rotation > 90) {
            // This is actually the 4th quadrant
            rotation = (short) (90 - rotation);
        }
        return rotation;
    }

    @Override
    public void setIndention(short indent) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getIndention() {
        return format.getIndent();
    }

    @Override
    public void setBorderLeft(BorderStyle border) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public BorderStyle getBorderLeft() {
        return BorderStyle.valueOf(format.getBorderLeft());
    }

    @Override
    public void setBorderRight(BorderStyle border) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public BorderStyle getBorderRight() {
        return BorderStyle.valueOf(format.getBorderRight());
    }

    @Override
    public void setBorderTop(BorderStyle border) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public BorderStyle getBorderTop() {
        return BorderStyle.valueOf(format.getBorderTop());
    }

    @Override
    public void setBorderBottom(BorderStyle border) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public BorderStyle getBorderBottom() {
        return BorderStyle.valueOf(format.getBorderBottom());
    }

    @Override
    public void setLeftBorderColor(short color) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getLeftBorderColor() {
        return format.getLeftBorderPaletteIdx();
    }

    @Override
    public void setRightBorderColor(short color) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getRightBorderColor() {
        return format.getRightBorderPaletteIdx();
    }

    @Override
    public void setTopBorderColor(short color) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getTopBorderColor() {
        return format.getTopBorderPaletteIdx();
    }

    @Override
    public void setBottomBorderColor(short color) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public short getBottomBorderColor() {
        return format.getBottomBorderPaletteIdx();
    }

    @Override
    public void setFillPattern(FillPatternType fp) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public FillPatternType getFillPattern() {
        return FillPatternType.forInt(format.getAdtlFillPattern());
    }

    @Override
    public void setFillBackgroundColor(short bg) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public void setFillBackgroundColor(Color color) {
        // The style is read from the workbook and never changed.
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
        // The style is read from the workbook and never changed.
    }

    @Override
    public void setFillForegroundColor(Color color) {
        // The style is read from the workbook and never changed.
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
        // The style is read from the workbook and never changed.
    }

    @Override
    public void setShrinkToFit(boolean shrinkToFit) {
        // The style is read from the workbook and never changed.
    }

    @Override
    public boolean getShrinkToFit() {
        return format.getShrinkToFit();
    }

    private EnumMap<CellPropertyType, Object> _cachedProperties;

    @Override
    public EnumMap<CellPropertyType, Object> getFormatProperties() {
        EnumMap<CellPropertyType, Object> props = this._cachedProperties;
        if (props == null) {
            props = CellUtil.getFormatProperties(this);
            this._cachedProperties = props;
        }

        return props;
    }

    @Override
    public void invalidateCachedProperties() {
        this._cachedProperties = null;
    }
}

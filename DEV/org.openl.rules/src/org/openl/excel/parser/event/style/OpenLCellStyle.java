package org.openl.excel.parser.event.style;

import static org.openl.excel.parser.event.style.PoiUtils.toRgb;

import java.util.Map;

import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.record.PaletteRecord;
import org.openl.rules.table.xls.XlsCellStyle;

class OpenLCellStyle extends XlsCellStyle {
    private final PaletteRecord palette;

    public OpenLCellStyle(int index,
            ExtendedFormatRecord format,
            PaletteRecord palette,
            Map<Integer, FormatRecord> formats) {
        super(new PoiCellStyle((short) index, format, formats), null);
        this.palette = palette;
    }

    @Override
    public short[][] getBorderRGB() {
        short[][] colors = new short[4][];

        colors[0] = toRgb(palette, getXlsStyle().getTopBorderColor());
        colors[1] = toRgb(palette, getXlsStyle().getRightBorderColor());
        colors[2] = toRgb(palette, getXlsStyle().getBottomBorderColor());
        colors[3] = toRgb(palette, getXlsStyle().getLeftBorderColor());

        return colors;
    }

    @Override
    public short[] getFillBackgroundColor() {
        if (hasNoFill()) {
            return null;
        }

        return toRgb(palette, getXlsStyle().getFillBackgroundColor());
    }

    @Override
    public short[] getFillForegroundColor() {
        if (hasNoFill()) {
            return null;
        }

        return toRgb(palette, getXlsStyle().getFillForegroundColor());
    }

}

package org.openl.excel.parser.event.style;

import static org.openl.excel.parser.event.style.PoiUtils.toRgb;

import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.ss.usermodel.Font;
import org.openl.rules.table.ui.ICellFont;

class OpenLCellFont implements ICellFont {
    private static final short BOLDWEIGHT_BOLD = 0x2bc;

    private final FontRecord font;
    private final PaletteRecord palette;

    public OpenLCellFont(FontRecord font, PaletteRecord palette) {
        this.font = font;
        this.palette = palette;
    }

    @Override
    public short[] getFontColor() {
        return toRgb(palette, font.getColorPaletteIndex());
    }

    @Override
    public String getName() {
        return font.getFontName();
    }

    public int getSize() {
        return (short) (font.getFontHeight() / 20);
    }

    @Override
    public boolean isBold() {
        return font.getBoldWeight() == BOLDWEIGHT_BOLD;
    }

    @Override
    public boolean isItalic() {
        return font.isItalic();
    }

    @Override
    public boolean isStrikeout() {
        return font.isStruckout();
    }

    @Override
    public boolean isUnderlined() {
        return font.getUnderline() != Font.U_NONE;
    }
}

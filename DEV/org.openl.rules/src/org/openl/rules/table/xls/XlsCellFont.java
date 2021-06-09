package org.openl.rules.table.xls;

import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.table.ui.ICellFont;

public class XlsCellFont implements ICellFont {

    private final Font font;
    private final Workbook workbook;

    public XlsCellFont(Font font, Workbook workbook) {
        this.font = font;
        this.workbook = workbook;
    }

    @Override
    public short[] getFontColor() {
        return PoiExcelHelper.getFontColor(font, workbook);
    }

    @Override
    public String getName() {
        return font.getFontName();
    }

    @Override
    public int getSize() {
        return font.getFontHeightInPoints();
    }

    @Override
    public boolean isBold() {
        return font.getBold();
    }

    @Override
    public boolean isItalic() {
        return font.getItalic();
    }

    @Override
    public boolean isStrikeout() {
        return font.getStrikeout();
    }

    @Override
    public boolean isUnderlined() {
        return font.getUnderline() != Font.U_NONE;
    }

}

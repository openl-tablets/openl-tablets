package org.openl.rules.table.xls;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openl.rules.table.ui.ICellFont;

public class XlsCellFont implements ICellFont {
    HSSFFont font;
    HSSFWorkbook workbook;

    public XlsCellFont(HSSFFont font, HSSFWorkbook workbook) {
        this.font = font;
        this.workbook = workbook;
    }

    public short[] getFontColor() {
        short x = font.getColor();
        return XlsCellStyle.colorToArray(x, workbook);
    }

    public String getName() {
        return font.getFontName();
    }

    public int getSize() {
        return font.getFontHeightInPoints();
    }

    public boolean isBold() {
        return font.getBoldweight() == HSSFFont.BOLDWEIGHT_BOLD;
    }

    public boolean isItalic() {
        return font.getItalic();
    }

    public boolean isStrikeout() {
        return font.getStrikeout();
    }

    public boolean isUnderlined() {
        return font.getUnderline() != HSSFFont.U_NONE;
    }

}

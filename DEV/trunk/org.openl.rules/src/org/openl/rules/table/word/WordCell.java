package org.openl.rules.table.word;

import java.util.Date;

import org.apache.poi.hwpf.usermodel.RangeHack;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

public class WordCell implements ICell{
    TableCell tcell;

    boolean merged;
    int row; 
    int column;

    public WordCell(TableCell tc, int row, int column) {
        tcell = tc;
        this.row = row;
        this.column = column;
    }

    public int getParEnd() {
//		return tcell.getParStart() + tcell.numParagraphs();
        return RangeHack.getParStart(tcell) + tcell.numParagraphs();
    }

    public int getParStart() {
//		return tcell.getParStart() + 1;
        return RangeHack.getParStart(tcell) + 1;
    }

    public String getStringValue() {
        String text = tcell.text();
        int len = text.length();
        if (len == 0) {
            return text;
        }
        if (text.charAt(len - 1) == 7) {
            return text.substring(0, len - 1);
        }
        return text;
    }

    public String getUri() {
		return XlsURLConstants.PARAGRAPH_START + "=" + getParStart() + 
			"&" + XlsURLConstants.PARAGRAPH_END + "=" + getParEnd();
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public int getAbsoluteColumn() {
        return getColumn();
    }

    public int getAbsoluteRow() {
        return getRow();
    }

    public IGridRegion getAbsoluteRegion() {
        return getRegion();
    }

    public int getColumn() {
        return column;
    }

    public ICellFont getFont() {
        return null;
    }

    public String getFormula() {
        return null;
    }

    public int getHeight() {
        return 1;
    }

    public Object getObjectValue() {
        return getStringValue();
    }

    public IGridRegion getRegion() {
        return null;
    }

    public int getRow() {
        return row;
    }

    public ICellStyle getStyle() {
        return null;
    }

    public int getType() {
        return IGrid.CELL_TYPE_STRING;
    }

    public int getWidth() {
        return 1;
    }

    public boolean hasNativeType() {
        return false;
    }

    public boolean getNativeBoolean() {
        throw new UnsupportedOperationException();
    }

    public double getNativeNumber() {
        throw new UnsupportedOperationException();
    }

    public Date getNativeDate() {
        throw new UnsupportedOperationException();
    }

    public int getNativeType() {
        throw new UnsupportedOperationException();
    }

    public CellMetaInfo getMetaInfo() {
        return null;
    }

}

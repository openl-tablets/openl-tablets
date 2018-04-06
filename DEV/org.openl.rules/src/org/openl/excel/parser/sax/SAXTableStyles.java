package org.openl.excel.parser.sax;

import org.apache.poi.xssf.model.StylesTable;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsCellFont;
import org.openl.rules.table.xls.XlsCellStyle;

public class SAXTableStyles implements TableStyles {
    private final IGridRegion region;
    private final int[][] cellIndexes;
    private final StylesTable stylesTable;

    public SAXTableStyles(IGridRegion region, int[][] cellIndexes, StylesTable stylesTable) {
        this.region = region;
        this.cellIndexes = cellIndexes;
        this.stylesTable = stylesTable;
    }

    @Override
    public IGridRegion getRegion() {
        return region;
    }

    @Override
    public ICellStyle getStyle(int row, int column) {
        int index = cellIndexes[row - region.getTop()][column - region.getLeft()];
        // For XSSF workbook isn't needed
        return new XlsCellStyle(stylesTable.getStyleAt(index), null);
    }

    @Override
    public ICellFont getFont(int row, int column) {
        int index = cellIndexes[row - region.getTop()][column - region.getLeft()];
        // For XSSF workbook isn't needed
        return new XlsCellFont(stylesTable.getStyleAt(index).getFont(), null);
    }

    @Override
    public ICellComment getComment(int row, int column) {
        // TODO: Implement
        return null;
    }
}

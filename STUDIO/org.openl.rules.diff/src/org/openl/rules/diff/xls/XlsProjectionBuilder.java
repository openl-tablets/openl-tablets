package org.openl.rules.diff.xls;

import static org.openl.rules.diff.xls.XlsProjectionType.*;

import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.diff.hierarchy.AbstractProperty;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.*;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.source.IOpenSourceCodeModule;

public final class XlsProjectionBuilder {

    public static XlsProjection build(XlsMetaInfo xmi, String xlsName) {
        XlsProjection projection = new XlsProjection(xlsName, BOOK);
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();

        // Here we have list of all tables.
        // But for usability reasons it is good to group tables
        // as they are located in XLS WorkBook, i.e. on Sheets
        // So here we have a tree:
        // - 1st level is for Sheets and
        // - 2nd for tables.
        Map<String, XlsProjection> sheetProjections = new TreeMap<>();

        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode node : nodes) {
            // find or create corresponding Sheet
            IOpenSourceCodeModule sheet = node.getModule();
            String sheetName = ((XlsSheetSourceCodeModule) sheet).getSheetName();
            XlsProjection sheetProjection = sheetProjections.get(sheetName);
            if (sheetProjection == null) {
                sheetProjection = new XlsProjection(sheetName, SHEET);
                sheetProjections.put(sheetName, sheetProjection);
            }

            // deal with table
            IOpenLTable table = new TableSyntaxNodeAdapter(node);
            String header = table.getGridTable().getCell(0, 0).getStringValue();
            String tableName = header == null ? "" : header;
            GridLocation location = node.getGridLocation();
            tableName += " (" + location.getStart() + ":" + location.getEnd() + ")";
            sheetProjection.addChild(buildTable(table, tableName));
        }

        for (XlsProjection sheetProjection : sheetProjections.values()) {
            projection.addChild(sheetProjection);
        }
        return projection;
    }

    public static XlsProjection buildTable(IOpenLTable table, String tableName) {
        XlsProjection projection = new XlsProjection(tableName, TABLE);
        projection.setData(table);

        // GRID
        XlsProjection grid = new XlsProjection("GRID", XlsProjectionType.GRID);
        projection.addChild(grid);

        IGridTable gridTable = table.getGridTable();
        int height = gridTable.getHeight();
        int width = gridTable.getWidth();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ICell cell = gridTable.getCell(j, i);
                grid.addChild(buildCell(cell, i + "x" + j));
            }
        }

        // ROWS
        /*
         * for (int i = 1; i < gridTable.getLogicalHeight(); i++) { ILogicalTable row = gridTable.getLogicalRow(i);
         * projection.addChild(buildRow(row, "row" + i)); }
         */
        return projection;
    }

    public static XlsProjection buildRow(ILogicalTable row, String rowName) {
        XlsProjection projection = new XlsProjection(rowName, ROW);
        ITable<?> grid = row.getSource();
        int height = grid.getHeight();
        int width = grid.getWidth();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ICell cell = grid.getCell(j, i);
                projection.addChild(buildCell(cell, i + "x" + j));
            }
        }
        return projection;
    }

    public static XlsProjection buildCell(ICell cell, String cellName) {
        Object cellValue = cell.getObjectValue();
        /*
         * int cellHeight = cell.getCellHeight(); int cellWidth = cell.getCellWidth(); ICellStyle cellStyle =
         * cell.getCellStyle(); ICellFont cellFont = cell.getCellInfo().getFont();
         */
        XlsProjection projection = new XlsProjection(cellName, CELL);
        projection.addProperty(new AbstractProperty(CELL_VALUE.name(), cellValue));
        /*
         * projection.addProperty(new AbstractProperty(CELL_HEIGHT.name(), int.class, cellHeight));
         * projection.addProperty(new AbstractProperty(CELL_WIDTH.name(), int.class, cellWidth));
         * projection.addChild(buildCellStyle(cellStyle)); projection.addChild(buildCellFont(cellFont));
         */
        projection.setData(cell);
        // AbstractProperty cellProp = new AbstractProperty("cell", ICell.class, cell);
        // projection.addProperty(cellProp);
        return projection;
    }

    public static XlsProjection buildCellStyle(ICellStyle style) {
        return new XlsProjection("style", CELL_STYLE);
    }

    public static XlsProjection buildCellFont(ICellFont font) {
        return new XlsProjection("font", CELL_FONT);
    }
}

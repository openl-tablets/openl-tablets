package org.openl.rules.diff.xls;

import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

import org.openl.rules.diff.test.AbstractProjection;
import org.openl.rules.diff.test.AbstractProperty;
import org.openl.source.IOpenSourceCodeModule;

import static org.openl.rules.diff.xls.XlsProjectionType.*;

public class XlsProjectionBuilder {

    public static AbstractProjection build(XlsMetaInfo xmi, String xlsName) {
        AbstractProjection projection = new AbstractProjection(xlsName, BOOK.name());
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        Map<String, AbstractProjection> sheetProjections = new TreeMap<String, AbstractProjection>();
        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();
        for (TableSyntaxNode node : nodes) {
            IOpenSourceCodeModule sheet = node.getModule();
            String sheetName = ((XlsSheetSourceCodeModule) sheet).getSheetName();
            AbstractProjection sheetProjection = sheetProjections.get(sheetName);
            if (sheetProjection == null) {
                sheetProjection = new AbstractProjection(sheetName, SHEET.name());
                sheetProjections.put(sheetName, sheetProjection);
            }

            ITable table = new TableSyntaxNodeAdapter(node);
            String header = table.getGridTable().getCell(0, 0).getStringValue();
            String tableName = header == null ? "" : header;
            GridLocation location = node.getGridLocation();
            tableName += " (" + location.getStart() + ":" + location.getEnd() + ")";
            sheetProjection.addChild(buildTable(table, tableName));
        }
        for (AbstractProjection sheetProjection : sheetProjections.values()) {
            projection.addChild(sheetProjection);
        }
        return projection;
    }

    public static AbstractProjection buildTable(ITable table, String tableName) {
        AbstractProjection projection = new AbstractProjection(tableName, TABLE.name());
        AbstractProperty grid = new AbstractProperty("grid", ITable.class, table, false);
        projection.addProperty(grid);
        IGridTable gridTable = table.getGridTable();
        /*for (int i = 1; i < gridTable.getLogicalHeight(); i++) {
            IGridTable row = gridTable.getLogicalRow(i);
            projection.addChild(buildRow(row, "row" + i));
        }*/
        int height = gridTable.getGridHeight();
        int width = gridTable.getGridWidth();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ICell cell = gridTable.getCell(j, i);
                Object cellValue = cell.getObjectValue();
                projection.addChild(buildCell(cell, (i + "x" + j + " - " + (cellValue == null ? "" : cellValue))));
            }
        }
        return projection;
    }

    public static AbstractProjection buildRow(IGridTable row, String rowName) {
        AbstractProjection projection = new AbstractProjection(rowName, ROW.name());
        IGridTable grid = row.getGridTable();
        int height = grid.getGridHeight();
        int width = grid.getGridWidth();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ICell cell = grid.getCell(j, i);
                Object cellValue = cell.getObjectValue();
                projection.addChild(buildCell(cell, (i + "x" + j + " - " + (cellValue == null ? "" : cellValue))));
            }
        }
        return projection;
    }

    public static AbstractProjection buildCell(ICell cell, String cellName) {
        Object cellValue = cell.getObjectValue();
        /*int cellHeight = cell.getCellHeight();
        int cellWidth = cell.getCellWidth();
        ICellStyle cellStyle = cell.getCellStyle();
        ICellFont cellFont = cell.getCellInfo().getFont();*/
        AbstractProjection projection = new AbstractProjection(cellName, CELL.name());
        projection.addProperty(new AbstractProperty(CELL_VALUE.name(),
                cellValue != null ? cellValue.getClass() : null, cellValue));
        /*projection.addProperty(new AbstractProperty(CELL_HEIGHT.name(),
                int.class, cellHeight));
        projection.addProperty(new AbstractProperty(CELL_WIDTH.name(),
                int.class, cellWidth));
        projection.addChild(buildCellStyle(cellStyle));
        projection.addChild(buildCellFont(cellFont));*/
        AbstractProperty cellProp = new AbstractProperty("cell", ICell.class, cell, false);
        projection.addProperty(cellProp);
        return projection;
    }

    public static AbstractProjection buildCellStyle(ICellStyle style) {
        return new AbstractProjection("style", CELL_STYLE.name());
    }

    public static AbstractProjection buildCellFont(ICellFont font) {
        return new AbstractProjection("font", CELL_FONT.name());
    }
}

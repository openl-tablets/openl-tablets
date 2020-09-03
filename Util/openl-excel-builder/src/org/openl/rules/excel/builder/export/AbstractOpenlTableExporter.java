package org.openl.rules.excel.builder.export;

import java.util.Collection;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.OpenlTableWriter;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.Model;
import org.openl.rules.table.xls.PoiExcelHelper;

public abstract class AbstractOpenlTableExporter<T extends Model> implements OpenlTableWriter<T> {

    public static final Cursor TOP_LEFT_POSITION = new Cursor(1, 2);
    public static final int DEFAULT_MARGIN = 3;
    public static final String DEFAULT_STRING_VALUE = "_DEFAULT_";

    private TableStyle tableStyle;

    @Override
    public Sheet export(Collection<T> models, Sheet sheet) {
        if (models == null || models.isEmpty()) {
            return null;
        }
        exportTables(models, sheet);
        return sheet;
    }

    protected void exportTables(Collection<T> models, Sheet sheet) {
        Cursor startPosition = getStartPosition();
        Cursor endPosition;
        for (T table : models) {
            endPosition = exportTable(table, startPosition, getTableStyle(), sheet);
            startPosition = startPosition.equals(endPosition) ? startPosition : nextFreePosition(endPosition);
        }
    }

    public void addMergedHeader(Sheet sheet, Cursor cursor, CellStyle style, CellRangeSettings cellRangeSettings) {
        CellRangeAddress mergedRegion = new CellRangeAddress(cursor.getRow(),
            cursor.getRow() + cellRangeSettings.getHeight(),
            cursor.getColumn(),
            cursor.getColumn() + cellRangeSettings.getWidth());
        sheet.addMergedRegionUnsafe(mergedRegion);
        for (int i = mergedRegion.getFirstRow(); i <= mergedRegion.getLastRow(); i++) {
            for (int j = mergedRegion.getFirstColumn(); j <= mergedRegion.getLastColumn(); j++) {
                Cell sheetCell = PoiExcelHelper.getOrCreateCell(j, i, sheet);
                sheetCell.setCellStyle(style);
            }
        }
    }

    protected abstract Cursor exportTable(T model, Cursor position, TableStyle tableStyle, Sheet sheet);

    protected abstract String getExcelSheetName();

    protected Cursor getStartPosition() {
        return TOP_LEFT_POSITION;
    }

    protected Cursor nextFreePosition(Cursor endPosition) {
        if (endPosition == null) {
            return TOP_LEFT_POSITION;
        }
        return new Cursor(endPosition.getColumn(), endPosition.getRow() + DEFAULT_MARGIN);
    }

    public TableStyle getTableStyle() {
        return tableStyle;
    }

    public void setTableStyle(TableStyle tableStyle) {
        this.tableStyle = tableStyle;
    }
}

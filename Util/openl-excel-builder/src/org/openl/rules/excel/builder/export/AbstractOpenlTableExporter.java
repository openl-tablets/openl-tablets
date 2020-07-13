package org.openl.rules.excel.builder.export;

import java.util.Collection;

import org.openl.rules.excel.builder.OpenlTableWriter;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.Model;

public abstract class AbstractOpenlTableExporter<T extends Model> implements OpenlTableWriter<T> {

    public static final Cursor TOP_LEFT_POSITION = new Cursor(1, 2);
    public static final int DEFAULT_MARGIN = 3;

    private TableStyle tableStyle;

    @Override
    public IWritableExtendedGrid export(IWritableExtendedGrid gridToExport, Collection<T> models) {
        if (models == null || models.isEmpty()) {
            return null;
        }
        exportTables(models, gridToExport);
        return gridToExport;
    }

    protected void exportTables(Collection<T> models, IWritableExtendedGrid gridToExport) {
        Cursor startPosition = getStartPosition();
        Cursor endPosition;
        for (T table : models) {
            endPosition = exportTable(table, gridToExport, startPosition);
            startPosition = startPosition.equals(endPosition) ? startPosition : nextFreePosition(endPosition);
        }
    }

    protected abstract Cursor exportTable(T model, IWritableExtendedGrid gridToWrite, Cursor position);

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

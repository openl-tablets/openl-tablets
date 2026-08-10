package org.openl.studio.projects.service.tables.write;

import java.util.List;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.studio.projects.model.tables.DatatypeAppend;
import org.openl.studio.projects.model.tables.DatatypeFieldView;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.util.StringUtils;

/**
 * Writes {@link DatatypeView} to {@code Datatype} table.
 *
 * @author Vladyslav Pikus
 */
public class DatatypeTableWriter extends TableWriter<DatatypeView> {

    public static final String EXTENDS_KEYWORD = "extends";

    public static final int TYPE_COLUMN = 0;
    public static final int NAME_COLUMN = 1;
    public static final int DEFAULT_VALUE_COLUMN = 2;

    public DatatypeTableWriter(IOpenLTable table) {
        super(table);
    }

    public DatatypeTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void updateHeader(DatatypeView tableView) {
        var header = new StringBuilder(getBusinessTableType(tableView)).append(' ').append(tableView.name);
        if (StringUtils.isNotBlank(tableView.extendz)) {
            header.append(' ').append(EXTENDS_KEYWORD).append(' ').append(tableView.extendz);
        }
        var gridTable = getGridTable();
        createOrUpdateCell(gridTable, buildCellKey(0, 0), header.toString());
        if (!isUpdateMode()) {
            var mergeTitleRegion = new GridRegion(0, 0, 0, DEFAULT_VALUE_COLUMN);
            applyMergeRegions(gridTable, List.of(mergeTitleRegion));
        }
    }

    @Override
    protected void updateBusinessBody(DatatypeView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);
        var columns = columnsOf(tableBody);
        var row = columns.firstFieldRow();
        for (var field : tableView.fields) {
            write(tableBody, row, field, columns);
            row++;
        }
        if (isUpdateMode()) {
            // clean up removed rows
            var height = IGridRegion.Tool.height(tableBody.getRegion());
            if (row < height) {
                removeRows(tableBody, height - row, row);
            }
        }
    }

    private void write(IGridTable tableBody, int row, DatatypeFieldView fieldView, DatatypeColumns columns) {
        createOrUpdateCell(tableBody, buildCellKey(columns.type(), row), fieldView.type);
        createOrUpdateCell(tableBody, buildCellKey(columns.name(), row), fieldView.name);
        if (columns.defaultValue() >= 0) {
            createOrUpdateCell(tableBody, buildCellKey(columns.defaultValue(), row), fieldView.defaultValue);
        }
    }

    /**
     * Where this body keeps each column, and which row its fields start on.
     *
     * <p>A body that titles its columns keeps the titles on its first row and may order the columns as it likes,
     * so writing at fixed positions would overwrite the titles and swap the values — see EPBDS-16418.
     */
    private static DatatypeColumns columnsOf(IGridTable tableBody) {
        var titles = DatatypeHelper.getColumnTitlesOrder(LogicalTableHelper.logicalTable(tableBody));
        if (titles.isEmpty()) {
            return new DatatypeColumns(TYPE_COLUMN, NAME_COLUMN, DEFAULT_VALUE_COLUMN, 0);
        }
        return new DatatypeColumns(titles.get(DatatypeHelper.TYPE_COLUMN_TITLE),
                titles.get(DatatypeHelper.NAME_COLUMN_TITLE),
                titles.getOrDefault(DatatypeHelper.DEFAULT_COLUMN_TITLE, -1),
                1);
    }

    private record DatatypeColumns(int type, int name, int defaultValue, int firstFieldRow) {
    }

    public void append(DatatypeAppend tableAppend) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            var columns = columnsOf(tableBody);
            var row = IGridRegion.Tool.height(tableBody.getRegion());
            for (var field : tableAppend.getFields()) {
                write(tableBody, row, field, columns);
                row++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

}

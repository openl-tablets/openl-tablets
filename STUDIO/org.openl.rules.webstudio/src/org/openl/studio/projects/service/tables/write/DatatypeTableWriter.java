package org.openl.studio.projects.service.tables.write;

import java.util.Collection;
import java.util.List;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.DatatypeAppend;
import org.openl.studio.projects.model.tables.DatatypeFieldView;
import org.openl.studio.projects.model.tables.DatatypeLayout;
import org.openl.studio.projects.model.tables.DatatypeView;
import org.openl.util.StringUtils;

/**
 * Writes {@link DatatypeView} to {@code Datatype} table.
 *
 * @author Vladyslav Pikus
 */
public class DatatypeTableWriter extends TableWriter<DatatypeView> {

    public static final String EXTENDS_KEYWORD = "extends";

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
            // The header names the whole table, so it spans every column the body turned out to need.
            var mergeTitleRegion = new GridRegion(0, 0, 0, DatatypeLayout.forFields(tableView.fields).width() - 1);
            applyMergeRegions(gridTable, List.of(mergeTitleRegion));
        }
    }

    @Override
    protected void updateBusinessBody(DatatypeView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);
        var update = isUpdateMode();
        DatatypeLayout.Columns columns;
        if (update) {
            columns = columnsOf(tableBody);
            requireColumnsFor(tableView.fields, columns);
        } else {
            // A created table is laid out around its fields, so it always has a column for every value.
            columns = DatatypeLayout.forFields(tableView.fields);
            if (columns.titled()) {
                writeColumnTitles(tableBody, columns);
            }
        }
        var row = columns.firstFieldRow();
        for (var field : tableView.fields) {
            write(tableBody, row, field, columns);
            row++;
        }
        if (update) {
            // clean up removed rows
            var height = IGridRegion.Tool.height(tableBody.getRegion());
            if (row < height) {
                removeRows(tableBody, height - row, row);
            }
        }
    }

    /**
     * Verifies that the body has a column for every value the fields carry.
     *
     * <p>Checked before the first cell is written, so that a rejected request leaves the table as it was rather
     * than half rewritten.
     *
     * @throws BadRequestException if a field carries a value the body keeps no column for
     */
    private static void requireColumnsFor(Collection<DatatypeFieldView> fields, DatatypeLayout.Columns columns) {
        for (var column : DatatypeLayout.OPTIONAL_COLUMNS) {
            var declared = columns.at(column.title()) >= 0;
            if (!declared && fields.stream().anyMatch(field -> DatatypeLayout.carries(field, column))) {
                // Rather than drop the value, say that this table keeps no column for it.
                throw new BadRequestException("table.datatype.column.absent.message", new Object[]{column.title()});
            }
        }
    }

    private void write(IGridTable tableBody, int row, DatatypeFieldView fieldView, DatatypeLayout.Columns columns) {
        createOrUpdateCell(tableBody, buildCellKey(columns.type(), row), fieldView.type);
        createOrUpdateCell(tableBody, buildCellKey(columns.name(), row), fieldView.name);
        for (var column : DatatypeLayout.OPTIONAL_COLUMNS) {
            var position = columns.at(column.title());
            if (position >= 0) {
                createOrUpdateCell(tableBody, buildCellKey(position, row), column.valueOf().apply(fieldView));
            }
        }
    }

    /** Names the columns on the first row, so that the body may carry more than the three positional ones. */
    private void writeColumnTitles(IGridTable tableBody, DatatypeLayout.Columns columns) {
        createOrUpdateCell(tableBody, buildCellKey(columns.type(), 0), DatatypeHelper.TYPE_COLUMN_TITLE);
        createOrUpdateCell(tableBody, buildCellKey(columns.name(), 0), DatatypeHelper.NAME_COLUMN_TITLE);
        columns.optional().forEach((title, column) -> createOrUpdateCell(tableBody, buildCellKey(column, 0), title));
    }

    /**
     * The layout the body already has.
     *
     * <p>A body that titles its columns keeps the titles on its first row and may order the columns as it likes,
     * so writing at fixed positions would overwrite the titles and swap the values — see EPBDS-16418.
     */
    private static DatatypeLayout.Columns columnsOf(IGridTable tableBody) {
        return DatatypeLayout.of(LogicalTableHelper.logicalTable(tableBody));
    }

    public void append(DatatypeAppend tableAppend) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        var columns = columnsOf(tableBody);
        requireColumnsFor(tableAppend.getFields(), columns);
        try {
            table.getGridTable().edit();
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

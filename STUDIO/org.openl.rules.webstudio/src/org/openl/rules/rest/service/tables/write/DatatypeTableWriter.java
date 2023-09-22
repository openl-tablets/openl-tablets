package org.openl.rules.rest.service.tables.write;

import java.util.Collection;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.rest.model.tables.DatatypeAppend;
import org.openl.rules.rest.model.tables.DatatypeFieldView;
import org.openl.rules.rest.model.tables.DatatypeView;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
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

    @Override
    protected void updateHeader(DatatypeView tableView) {
        var header = new StringBuilder(getBusinessTableType()).append(' ').append(tableView.name);
        if (StringUtils.isNotBlank(tableView.extendz)) {
            header.append(' ').append(EXTENDS_KEYWORD).append(' ').append(tableView.extendz);
        }
        createOrUpdateCell(table.getGridTable(), buildCellKey(0, 0), header.toString());
    }

    @Override
    protected void updateBusinessBody(DatatypeView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        int row = 0;
        for (var field : tableView.fields) {
            write(tableBody, row, field);
            row++;
        }
        // clean up removed rows
        var height = IGridRegion.Tool.height(tableBody.getRegion());
        if (row < height) {
            removeRows(tableBody, height - row, row);
        }
    }

    private void write(IGridTable tableBody, int row, DatatypeFieldView fieldView) {
        createOrUpdateCell(tableBody, buildCellKey(TYPE_COLUMN, row), fieldView.type);
        createOrUpdateCell(tableBody, buildCellKey(NAME_COLUMN, row), fieldView.name);
        createOrUpdateCell(tableBody, buildCellKey(DEFAULT_VALUE_COLUMN, row), fieldView.defaultValue);
    }

    public void append(DatatypeAppend tableAppend) {
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            int row = IGridRegion.Tool.height(tableBody.getRegion());
            for (var field : tableAppend.getFields()) {
                write(tableBody, row, field);
                row++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }

}

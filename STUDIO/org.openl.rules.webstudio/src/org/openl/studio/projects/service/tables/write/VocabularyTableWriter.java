package org.openl.studio.projects.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.VocabularyAppend;
import org.openl.studio.projects.model.tables.VocabularyView;

/**
 * Writes {@link VocabularyView} model to legacy {@code Vocabulary} table.
 *
 * @author Vladyslav Pikus
 */
public class VocabularyTableWriter extends TableWriter<VocabularyView> {

    public static final char TYPE_OPEN = '<';
    public static final char TYPE_CLOSE = '>';

    public VocabularyTableWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateHeader(VocabularyView tableView) {
        String headerSign = getBusinessTableType(tableView) + " " + tableView.name + " " + TYPE_OPEN + tableView.type + TYPE_CLOSE;
        createOrUpdateCell(table.getGridTable(), buildCellKey(0, 0), headerSign);
    }

    @Override
    protected void updateBusinessBody(VocabularyView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
        int row = 0;
        for (var value : tableView.values) {
            createOrUpdateCell(tableBody, buildCellKey(0, row), value.value);
            row++;
        }
        // clean up removed rows
        var height = IGridRegion.Tool.height(tableBody.getRegion());
        if (row < height) {
            removeRows(tableBody, height - row, row);
        }
    }

    public void append(VocabularyAppend tableAppend) {
        try {
            table.getGridTable().edit();
            var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);
            int row = IGridRegion.Tool.height(tableBody.getRegion());
            for (var value : tableAppend.getValues()) {
                createOrUpdateCell(tableBody, buildCellKey(0, row), value.value);
                row++;
            }
            save();
        } finally {
            table.getGridTable().stopEditing();
        }
    }
}

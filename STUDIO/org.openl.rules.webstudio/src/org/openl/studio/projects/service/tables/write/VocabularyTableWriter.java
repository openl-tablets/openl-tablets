package org.openl.studio.projects.service.tables.write;

import java.util.List;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.VocabularyAppend;
import org.openl.studio.projects.model.tables.VocabularyView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.util.CollectionUtils;

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

    public VocabularyTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void updateHeader(VocabularyView tableView) {
        String headerSign = getBusinessTableType(tableView) + " " + tableView.name + " " + TYPE_OPEN + tableView.type + TYPE_CLOSE;
        var gridTable = getGridTable();
        createOrUpdateCell(gridTable, buildCellKey(0, 0), headerSign);
        if (!isUpdateMode() && CollectionUtils.isNotEmpty(tableView.properties)) {
            var mergeTitleRegion = new GridRegion(0, 0, 0, NUMBER_PROPERTIES_COLUMNS - 1);
            applyMergeRegions(gridTable, List.of(mergeTitleRegion));
        }
    }

    @Override
    protected void updateBusinessBody(VocabularyView tableView) {
        var tableBody = getGridTable(IXlsTableNames.VIEW_BUSINESS);
        int row = 0;
        if (!isUpdateMode()) {
            // in creation mode, table does not have business body yet
            row = 1;
        }
        for (var value : tableView.values) {
            createOrUpdateCell(tableBody, buildCellKey(0, row), value.value);
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

    public void append(VocabularyAppend tableAppend) {
        if (!isUpdateMode()) {
            throw new IllegalStateException("Append operation is only allowed in update mode.");
        }
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

    @Override
    protected String getBusinessTableType(VocabularyView tableView) {
        return OpenLTableUtils.getTableTypeItems().get(XlsNodeTypes.XLS_DATATYPE.toString());
    }
}

package org.openl.studio.projects.service.tables.write;

import java.util.List;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.DataAppend;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Writes {@link DataView} to {@code Data} table.
 *
 * Structure:
 * - updateHeader(): Writes table header ("Data <dataType> tableName")
 * - updateTableHeaders(): Inherited from AbstractDataTableWriter - writes field names (row 0) and foreign keys (row 1)
 * - updateBusinessBody(): Inherited from AbstractDataTableWriter - writes display names and data rows
 *
 * @author Vladyslav Pikus
 */
public class DataTableWriter extends AbstractDataTableWriter<DataView> {

    public DataTableWriter(IOpenLTable table) {
        super(table);
    }

    public DataTableWriter(IGridTable gridTable, MetaInfoWriter metaInfoWriter) {
        super(gridTable, metaInfoWriter);
    }

    @Override
    protected void updateHeader(DataView tableView) {
        var header = new StringBuilder(getBusinessTableType(tableView));
        if (StringUtils.isNotBlank(tableView.dataType)) {
            header.append(' ').append(tableView.dataType);
        }
        header.append(' ').append(tableView.name);
        var gridTable = getGridTable();
        createOrUpdateCell(gridTable, buildCellKey(0, 0), header.toString());
        if (!isUpdateMode()) {
            int latestCol = tableView.headers.size();
            if (CollectionUtils.isNotEmpty(tableView.properties)) {
                latestCol = Math.max(NUMBER_PROPERTIES_COLUMNS, latestCol);
            }
            var mergeTitleRegion = new GridRegion(0, 0, 0, latestCol - 1);
            applyMergeRegions(gridTable, List.of(mergeTitleRegion));
        }
    }

    /**
     * Append new rows to the Data table
     */
    public void append(DataAppend dataAppend) {
        appendRows(dataAppend.getRows());
    }

}

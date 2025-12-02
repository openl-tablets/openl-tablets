package org.openl.studio.projects.service.tables.read;

import org.springframework.stereotype.Component;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.util.StringUtils;

/**
 * Reads {@code Data} table to {@link DataView} model.
 * <p>
 * Header format: "Data <dataType> tableName"
 * Example: "Data Bank bankData" where dataType="Bank", name="bankData"
 *
 * @author Vladyslav Pikus
 */
@Component
public class DataTableReader extends AbstractDataTableReader<DataView, DataView.Builder> {

    public DataTableReader() {
        super(DataView::builder);
    }

    @Override
    protected void initialize(DataView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var header = tsn.getHeader();
        builder.dataType(extractDataType(header.getSourceString()));
        var cellValueReader = new CellValueReader(tsn.getMetaInfoReader());

        var tableBody = tsn.getTableBody();
        if (tableBody != null) {
            readAndSetTableBody(builder, tableBody, cellValueReader);
        }
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isDataTable(table);
    }

    /**
     * Extract data type from header source string.
     * Header format: "Data TypeName tableName"
     * Returns the TypeName (second token after "Data")
     * Example: "Data Bank bankData" -> "Bank"
     */
    private String extractDataType(String headerSource) {
        if (StringUtils.isBlank(headerSource)) {
            return null;
        }

        String[] tokens = headerSource.trim().split("\\s+");
        // Expected format: Data <TypeName> <tableName>
        if (tokens.length >= 2) {
            return StringUtils.trimToNull(tokens[1]);
        }
        return null;
    }

}

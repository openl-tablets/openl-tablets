package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for appending rows to Data tables
 *
 * @author Vladyslav Pikus
 */
public class DataAppend implements AppendTableView {

    @Schema(description = "Collection of data rows to append")
    private Collection<DataRowView> rows;

    public Collection<DataRowView> getRows() {
        return rows;
    }

    public void setRows(Collection<DataRowView> rows) {
        this.rows = rows;
    }

    @Override
    public String getTableType() {
        return DataView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }

}

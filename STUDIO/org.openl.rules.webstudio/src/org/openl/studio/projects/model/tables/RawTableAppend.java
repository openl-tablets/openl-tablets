package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for appending rows to raw tables.
 * <p>
 * Represents rows to append to the end of a table in raw 2D matrix format.
 * Each row is a list of RawTableCell objects.
 *
 * @author Vladyslav Pikus
 */
public class RawTableAppend implements AppendTableView {

    @Schema(description = "Rows to append as a 2D matrix of raw table cells")
    private List<List<RawTableCell>> rows;

    public List<List<RawTableCell>> getRows() {
        return rows;
    }

    public void setRows(List<List<RawTableCell>> rows) {
        this.rows = rows;
    }

    @Override
    public String getTableType() {
        return RawTableView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }

}

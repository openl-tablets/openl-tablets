package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for appending rows to SmartLookup table
 *
 * @author Vladyslav Pikus
 */
public class LookupAppend implements AppendTableView {

    @Schema(description = "Type of lookup table (SmartLookup or SimpleLookup)")
    public String tableType;

    @Schema(description = "Data rows with hierarchical structure to append")
    private List<LinkedHashMap<String, Object>> rows;

    public List<LinkedHashMap<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<LinkedHashMap<String, Object>> rows) {
        this.rows = rows;
    }

    @Override
    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }
}

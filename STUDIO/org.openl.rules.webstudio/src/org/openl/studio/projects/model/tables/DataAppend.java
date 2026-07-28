package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for appending rows to Data tables
 *
 * @author Vladyslav Pikus
 */
public class DataAppend implements AppendTableView {

    @Getter
    @Schema(description = "Collection of data rows to append")
    @Setter
    private Collection<DataRowView> rows;

    @Override
    public String getTableType() {
        return DataView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }

}

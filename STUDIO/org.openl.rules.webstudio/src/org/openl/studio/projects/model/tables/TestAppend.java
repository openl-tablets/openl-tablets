package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for appending rows to Test tables
 *
 * @author Vladyslav Pikus
 */
public class TestAppend implements AppendTableView {

    @Getter
    @Schema(description = "Collection of test data rows to append")
    @Setter
    private Collection<DataRowView> rows;

    @Override
    public String getTableType() {
        return TestView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }

}

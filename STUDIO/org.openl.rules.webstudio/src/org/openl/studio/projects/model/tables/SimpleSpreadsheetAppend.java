package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for append lines to {@code SimpleSpreadsheet} table
 *
 * @author Vladyslav Pikus
 */
public class SimpleSpreadsheetAppend implements AppendTableView {

    @Getter
    @Schema(description = "Collection of spreadsheet steps/rows to append")
    @Setter
    private Collection<SpreadsheetStepView> steps;

    @Override
    public String getTableType() {
        return SimpleSpreadsheetView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}

package org.openl.studio.projects.model.tables;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for append lines to {@code SimpleSpreadsheet} table
 *
 * @author Vladyslav Pikus
 */
public class SimpleSpreadsheetAppend implements AppendTableView {

    @Schema(description = "Collection of spreadsheet steps/rows to append")
    private Collection<SpreadsheetStepView> steps;

    public Collection<SpreadsheetStepView> getSteps() {
        return steps;
    }

    public void setSteps(Collection<SpreadsheetStepView> steps) {
        this.steps = steps;
    }

    @Override
    public String getTableType() {
        return SimpleSpreadsheetView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}

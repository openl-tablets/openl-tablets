package org.openl.studio.projects.model.tables;

import java.util.Collection;

/**
 * Request model for append lines to {@code SimpleSpreadsheet} table
 *
 * @author Vladyslav Pikus
 */
public class SimpleSpreadsheetAppend implements AppendTableView {

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
}

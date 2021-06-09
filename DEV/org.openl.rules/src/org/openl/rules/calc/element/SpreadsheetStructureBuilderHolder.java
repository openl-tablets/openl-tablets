package org.openl.rules.calc.element;

import java.util.Objects;

import org.openl.rules.calc.SpreadsheetStructureBuilder;

public class SpreadsheetStructureBuilderHolder {

    SpreadsheetStructureBuilder spreadsheetStructureBuilder;

    public SpreadsheetStructureBuilderHolder(SpreadsheetStructureBuilder spreadsheetStructureBuilder) {
        this.spreadsheetStructureBuilder = Objects.requireNonNull(spreadsheetStructureBuilder, "spreadsheetStructureBuilder cannot be null");
    }

    public SpreadsheetStructureBuilder getSpreadsheetStructureBuilder() {
        return spreadsheetStructureBuilder;
    }

    public void clear() {
        spreadsheetStructureBuilder = null;
    }
}

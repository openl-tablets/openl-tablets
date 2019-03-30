package org.openl.rules.calc.element;

import org.openl.rules.calc.SpreadsheetStructureBuilder;

public class SpreadsheetStructureBuilderHolder {

    SpreadsheetStructureBuilder spreadsheetStructureBuilder;

    public SpreadsheetStructureBuilderHolder(SpreadsheetStructureBuilder spreadsheetStructureBuilder) {
        this.spreadsheetStructureBuilder = spreadsheetStructureBuilder;
    }

    public SpreadsheetStructureBuilder getSpreadsheetStructureBuilder() {
        return spreadsheetStructureBuilder;
    }

    public void clear() {
        spreadsheetStructureBuilder = null;
    }
}

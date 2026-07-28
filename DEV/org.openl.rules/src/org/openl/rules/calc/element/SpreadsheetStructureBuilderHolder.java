package org.openl.rules.calc.element;

import java.util.Objects;

import lombok.Getter;

import org.openl.rules.calc.SpreadsheetStructureBuilder;

public class SpreadsheetStructureBuilderHolder {

    @Getter
    SpreadsheetStructureBuilder spreadsheetStructureBuilder;

    public SpreadsheetStructureBuilderHolder(SpreadsheetStructureBuilder spreadsheetStructureBuilder) {
        this.spreadsheetStructureBuilder = Objects.requireNonNull(spreadsheetStructureBuilder, "spreadsheetStructureBuilder cannot be null");
    }

    public void clear() {
        spreadsheetStructureBuilder = null;
    }
}

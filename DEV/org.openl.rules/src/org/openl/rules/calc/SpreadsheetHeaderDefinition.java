package org.openl.rules.calc;

import lombok.Getter;
import lombok.Setter;

import org.openl.types.IOpenClass;

public class SpreadsheetHeaderDefinition {

    @Getter
    private final int row;
    @Getter
    private final int column;

    @Getter
    @Setter
    private IOpenClass type;
    @Getter
    private final SymbolicTypeDefinition definition;

    public SpreadsheetHeaderDefinition(SymbolicTypeDefinition definition, int row, int column) {
        this.definition = definition;
        this.row = row;
        this.column = column;
    }

    public String getDefinitionName() {
        if (definition != null && definition.getName() != null) {
            return definition.getName().getIdentifier();
        }
        return null;
    }

    public boolean isRow() {
        return row >= 0;
    }
}

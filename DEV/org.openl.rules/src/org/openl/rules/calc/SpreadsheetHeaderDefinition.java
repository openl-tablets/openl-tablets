package org.openl.rules.calc;

import org.openl.types.IOpenClass;

public class SpreadsheetHeaderDefinition {

    private int row;
    private int column;

    private IOpenClass type;
    private SymbolicTypeDefinition definition;

    public SpreadsheetHeaderDefinition(SymbolicTypeDefinition definition, int row, int column) {
        this.definition = definition;
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public IOpenClass getType() {
        return type;
    }

    public void setType(IOpenClass type) {
        this.type = type;
    }

    public SymbolicTypeDefinition getDefinition() {
        return definition;
    }

    public SymbolicTypeDefinition findDefinition(String name) {
        if (definition != null && definition.getName() != null && definition.getName().getIdentifier().equals(name)) {
            return definition;
        }
        return null;
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

    public String rowOrColumn() {
        return isRow() ? "row" : "column";
    }

}

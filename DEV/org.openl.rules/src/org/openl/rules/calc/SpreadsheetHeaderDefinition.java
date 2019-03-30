package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.List;

import org.openl.types.IOpenClass;

public class SpreadsheetHeaderDefinition {

    private int row;
    private int column;

    private IOpenClass type;
    private List<SymbolicTypeDefinition> vars = new ArrayList<>();

    public SpreadsheetHeaderDefinition(int row, int column) {
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

    public List<SymbolicTypeDefinition> getVars() {
        return vars;
    }

    public void setType(IOpenClass type) {
        this.type = type;
    }

    public void addVarHeader(SymbolicTypeDefinition parsed) {
        vars.add(parsed);
    }

    public SymbolicTypeDefinition findVarDef(String name) {

        for (SymbolicTypeDefinition sdef : vars) {
            if (sdef.getName().getIdentifier().equals(name)) {
                return sdef;
            }
        }

        return null;
    }

    public String getFirstname() {

        for (SymbolicTypeDefinition definition : vars) {
            if (definition != null && definition.getName() != null) {
                return definition.getName().getIdentifier();
            }
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

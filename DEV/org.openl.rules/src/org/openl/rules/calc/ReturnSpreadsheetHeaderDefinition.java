package org.openl.rules.calc;

import org.openl.types.IOpenClass;

public class ReturnSpreadsheetHeaderDefinition extends SpreadsheetHeaderDefinition {
    private final IOpenClass returnType;

    public ReturnSpreadsheetHeaderDefinition(SpreadsheetHeaderDefinition source, IOpenClass returnType) {
        super(source.getRow(), source.getColumn());
        this.setType(source.getType());
        for (SymbolicTypeDefinition var : source.getVars()) {
            addVarHeader(var);
        }

        this.returnType = returnType;
    }

    public IOpenClass getReturnType() {
        return returnType;
    }
}

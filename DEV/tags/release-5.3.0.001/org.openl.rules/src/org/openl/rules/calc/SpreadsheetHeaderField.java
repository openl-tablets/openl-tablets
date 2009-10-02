package org.openl.rules.calc;

import org.openl.syntax.impl.IdentifierNode;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetHeaderField extends ASpreadsheetField {

    IdentifierNode name;
    SpreadsheetHeaderDefinition header;

    public SpreadsheetHeaderField(SpreadsheetType declaringClass, IdentifierNode name, SpreadsheetHeaderDefinition h) {
        super(declaringClass, name.getIdentifier(), h.getType());
        this.name = name;
        header = h;
    }

    @Override
    public Object calculate(SpreadsheetResult spreadsheetResult, Object targetModule, Object[] params, IRuntimeEnv env) {
        return header.getArray(spreadsheetResult, env);
    }

}

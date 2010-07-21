package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObjectField;
import org.openl.vm.IRuntimeEnv;

public abstract class ASpreadsheetField extends DynamicObjectField {

    public ASpreadsheetField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
    }

    public abstract Object calculate(SpreadsheetResultCalculator spreadsheetResult,
            Object targetModule,
            Object[] params,
            IRuntimeEnv env);
}

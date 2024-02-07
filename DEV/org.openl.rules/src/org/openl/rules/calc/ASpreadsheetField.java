package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObjectField;

public abstract class ASpreadsheetField extends DynamicObjectField {

    public ASpreadsheetField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
    }

}

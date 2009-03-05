package org.openl.rules.domaintype;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenFieldDelegator;

public class ModifiedField extends OpenFieldDelegator {

    IOpenClass newType;

    public ModifiedField(IOpenField field, IOpenClass newType) {
        super(field);
        this.newType = newType;
    }

    @Override
    public IOpenClass getType() {
        return newType;
    }

}

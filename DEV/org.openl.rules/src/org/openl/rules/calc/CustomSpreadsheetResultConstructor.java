package org.openl.rules.calc;

import java.util.Objects;

import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

final class CustomSpreadsheetResultConstructor extends AOpenMethodDelegator {
    private final IOpenClass type;

    public CustomSpreadsheetResultConstructor(IOpenMethod delegate, IOpenClass type) {
        super(delegate);
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    @Override
    public IOpenClass getType() {
        return type;
    }
}

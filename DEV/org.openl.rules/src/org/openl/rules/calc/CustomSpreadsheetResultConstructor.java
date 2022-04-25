package org.openl.rules.calc;

import java.util.Objects;

import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

final class CustomSpreadsheetResultConstructor extends AOpenMethodDelegator {
    private final CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;

    public CustomSpreadsheetResultConstructor(IOpenMethod delegate,
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        super(delegate);
        this.customSpreadsheetResultOpenClass = Objects.requireNonNull(customSpreadsheetResultOpenClass,
            "type cannot be null");
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return customSpreadsheetResultOpenClass.newInstance(env);
    }

    @Override
    public IOpenClass getType() {
        return customSpreadsheetResultOpenClass;
    }
}

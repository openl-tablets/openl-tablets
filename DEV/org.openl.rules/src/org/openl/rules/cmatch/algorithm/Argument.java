package org.openl.rules.cmatch.algorithm;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class Argument {
    private final int index;
    /**
     * Type of argument
     */
    private final IOpenClass type;
    private IOpenField field;

    public Argument(int index, IOpenClass type) {
        this.index = index;
        this.type = type;
    }

    public Argument(int index, IOpenField field) {
        this.index = index;
        type = field.getType();
        this.field = field;
    }

    public Object extractValue(Object target, Object[] params, IRuntimeEnv env) {
        if (field == null) {
            return params[index];
        } else {
            return field.get(params[index], env);
        }
    }

    public IOpenClass getType() {
        return type;
    }
}

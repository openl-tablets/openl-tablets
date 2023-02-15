package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

class ExprDecisionRowField implements IOpenField {
    private final ExprDecisionRowFieldDataType type;
    private final String name;

    ExprDecisionRowField(DecisionRowField decisionRowField, OpenL openl) {
        this.type = new ExprDecisionRowFieldDataType(decisionRowField.getType(), openl);
        this.name = decisionRowField.getName();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return target;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isContextProperty() {
        return false;
    }

    @Override
    public String getContextProperty() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public String getDisplayName(int mode) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return null;
    }
}

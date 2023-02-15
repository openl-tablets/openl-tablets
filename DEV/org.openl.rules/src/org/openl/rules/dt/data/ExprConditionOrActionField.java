package org.openl.rules.dt.data;

import org.openl.rules.dt.Expr;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

class ExprConditionOrActionField implements IOpenField {
    private final DecisionRowField decisionRowField;

    ExprConditionOrActionField(DecisionRowField decisionRowField) {
        this.decisionRowField = decisionRowField;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return decisionRowField.getConditionOrAction().getExpr();
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
        return getName();
    }

    @Override
    public String getName() {
        return decisionRowField.getConditionOrAction().getName();
    }

    @Override
    public IOpenClass getType() {
        return Expr.EXPR_JAVA_OPEN_CLASS;
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

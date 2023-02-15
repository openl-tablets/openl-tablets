package org.openl.rules.dt.data;

import org.openl.rules.dt.Expr;
import org.openl.rules.dt.IBaseDecisionRow;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

class ExprParameterField implements IOpenField {
    private final IBaseDecisionRow conditionOrAction;
    private final int paramNum;
    private final IOpenField delegate;

    ExprParameterField(ConditionOrActionDirectParameterField conditionOrActionDirectParameterField) {
        this.conditionOrAction = conditionOrActionDirectParameterField.getConditionOrAction();
        this.paramNum = conditionOrActionDirectParameterField.getParamNum();
        this.delegate = conditionOrActionDirectParameterField;
    }

    ExprParameterField(ConditionOrActionParameterField conditionOrActionParameterField) {
        this.conditionOrAction = conditionOrActionParameterField.getConditionOrAction();
        this.paramNum = conditionOrActionParameterField.getParamNum();
        this.delegate = conditionOrActionParameterField;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        int ruleNum = reo.getRuleNum();
        Expr expr = conditionOrAction.getExprValue(paramNum, ruleNum);
        return expr != null ? expr : Expr.NULL_EXPR;
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
        return delegate.getDisplayName(mode);
    }

    @Override
    public String getName() {
        return delegate.getName();
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

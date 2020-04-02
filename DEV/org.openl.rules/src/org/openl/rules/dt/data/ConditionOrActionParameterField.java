package org.openl.rules.dt.data;

import org.openl.rules.dt.IBaseDecisionRow;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class ConditionOrActionParameterField implements IOpenField {

    private IBaseDecisionRow conditionOrAction;
    private int paramNum;

    ConditionOrActionParameterField(IBaseDecisionRow conditionOrAction, int paramNum) {
        super();
        this.conditionOrAction = conditionOrAction;
        this.paramNum = paramNum;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }
        Object[] params = (Object[]) target;

        return params[paramNum];
    }

    public IBaseDecisionRow getConditionOrAction() {
        return conditionOrAction;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return null;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getType() {
        return conditionOrAction.getParams()[paramNum].getType();
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    @Override
    public String getName() {
        return conditionOrAction.getParams()[paramNum].getName();
    }

}

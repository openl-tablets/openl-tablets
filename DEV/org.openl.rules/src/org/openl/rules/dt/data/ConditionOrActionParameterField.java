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

    public Object get(Object target, IRuntimeEnv env) {

        Object[] params = (Object[]) target;

        return params[paramNum];
    }

    public IBaseDecisionRow getConditionOrAction() {
        return conditionOrAction;
    }

    public boolean isConst() {
        return true;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWritable() {
        return false;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    public IOpenClass getDeclaringClass() {
        return null;
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public IOpenClass getType() {
        return conditionOrAction.getParams()[paramNum].getType();
    }

    public boolean isStatic() {
        return false;
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    public String getName() {
        return conditionOrAction.getParams()[paramNum].getName();
    }

}

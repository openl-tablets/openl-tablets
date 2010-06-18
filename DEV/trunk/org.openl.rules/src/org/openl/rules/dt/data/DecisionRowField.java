package org.openl.rules.dt.data;

import org.openl.rules.dt.element.IDecisionRow;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class DecisionRowField implements IOpenField {

    IDecisionRow condOrAction; 
    DecisionTableDataType decisionTableDataType;
    ConditionOrActionDataType dataType;

    public DecisionRowField(IDecisionRow condOrAction, ConditionOrActionDataType dataType,  DecisionTableDataType decisionTableDataType) {
        super();
        this.condOrAction = condOrAction;
        this.dataType = dataType;
        this.decisionTableDataType = decisionTableDataType;
        
    }

    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject)target;
        int ruleNum = reo.getRuleNum();
        
        Object[] res = condOrAction.getParamValues()[ruleNum];
        
        return res;
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
        return decisionTableDataType;
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public IOpenClass getType() {
        return dataType;
    }

    public boolean isStatic() {
        return false;
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    public String getName() {
        return "$" + condOrAction.getName();
    }

}

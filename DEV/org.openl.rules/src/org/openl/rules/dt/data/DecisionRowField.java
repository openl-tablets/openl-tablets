package org.openl.rules.dt.data;

import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class DecisionRowField implements IOpenField {

    private IDecisionRow conditionOrAction;
    private DecisionTableDataType decisionTableDataType;
    private ConditionOrActionDataType dataType;

    DecisionRowField(IDecisionRow condOrAction,
            ConditionOrActionDataType dataType,
            DecisionTableDataType decisionTableDataType) {
        this.conditionOrAction = condOrAction;
        this.dataType = dataType;
        this.decisionTableDataType = decisionTableDataType;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        int ruleNum = reo.getRuleNum();

        Object[] res = new Object[conditionOrAction.getNumberOfParams()];
        conditionOrAction.loadValues(res, 0, ruleNum, target, env.getLocalFrame(), env);

        return res;
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
        return decisionTableDataType;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getType() {
        return dataType;
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
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + conditionOrAction.getName();
    }

}

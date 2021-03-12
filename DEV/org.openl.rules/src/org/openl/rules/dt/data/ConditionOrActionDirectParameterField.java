package org.openl.rules.dt.data;

import java.util.Objects;

import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

class ConditionOrActionDirectParameterField implements IOpenField {

    private final IDecisionRow decisionRow;
    private final int paramNum;
    private final DecisionTableDataType decisionTableDataType;

    ConditionOrActionDirectParameterField(IDecisionRow decisionRow,
            int paramNum,
            DecisionTableDataType decisionTableDataType) {
        super();
        this.decisionTableDataType = Objects.requireNonNull(decisionTableDataType, "declaringClass cannot be null");
        this.decisionRow = Objects.requireNonNull(decisionRow, "decisionRow cannot be null");
        this.paramNum = paramNum;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        int ruleNum = reo.getRuleNum();

        Object[] res = new Object[decisionRow.getNumberOfParams()];
        decisionRow.loadValues(res, 0, ruleNum, target, env.getLocalFrame(), env);

        Object ret = res[paramNum];
        if (ret == null) {
            return getType().nullObject();
        }
        return ret;
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
        return decisionRow.getParams()[paramNum].getType();
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public String getDisplayName(int mode) {
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + decisionRow.getName() + "." + getName();
    }

    @Override
    public String getName() {
        return decisionRow.getParams()[paramNum].getName();
    }

}

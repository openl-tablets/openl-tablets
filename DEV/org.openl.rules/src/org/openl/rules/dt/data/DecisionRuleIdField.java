package org.openl.rules.dt.data;

import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class DecisionRuleIdField  implements IOpenField {

    private DecisionTableDataType decisionTableDataType;

    DecisionRuleIdField(DecisionTableDataType decisionTableDataType) {
        this.decisionTableDataType = decisionTableDataType;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        return reo.getRuleNum();
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
        return JavaOpenClass.getOpenClass(Integer.class);
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
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + "RuleId";
    }

}

package org.openl.rules.dt.data;

import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class DecisionRowNumberField  implements IOpenField {

    private DecisionTableDataType decisionTableDataType;

    DecisionRowNumberField(DecisionTableDataType decisionTableDataType) {
        this.decisionTableDataType = decisionTableDataType;
    }

    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        return reo.getRuleNum();
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
        return JavaOpenClass.getOpenClass(Integer.class);
    }

    public boolean isStatic() {
        return false;
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    public String getName() {
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + "ruleId";
    }

}

package org.openl.rules.dt.data;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.dt.element.RuleRow;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class DecisionRuleNameField implements IOpenField {

    private DecisionTableDataType decisionTableDataType;
    private RuleRow ruleRow;
    
    DecisionRuleNameField(DecisionTableDataType decisionTableDataType, RuleRow ruleRow) {
        this.decisionTableDataType = decisionTableDataType;
        this.ruleRow = ruleRow;
    }

    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        int rowNum = reo.getRuleNum();
        return ruleRow != null ? ruleRow.getRuleName(rowNum) : StringUtils.EMPTY;
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
        return JavaOpenClass.STRING;
    }

    public boolean isStatic() {
        return false;
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    public String getName() {
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + "Rule";
    }

}


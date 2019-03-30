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

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        RuleExecutionObject reo = (RuleExecutionObject) target;
        int rowNum = reo.getRuleNum();
        return ruleRow != null ? ruleRow.getRuleName(rowNum) : StringUtils.EMPTY;
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
        return JavaOpenClass.STRING;
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
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + "Rule";
    }

}

package org.openl.rules.dt.data;

import org.openl.OpenL;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

class DecisionExprField implements IOpenField {

    private final DecisionTableDataType decisionTableDataType;
    private final DecisionExprFieldDataType type;

    DecisionExprField(DecisionTableDataType decisionTableDataType, OpenL openl) {
        this.decisionTableDataType = decisionTableDataType;
        this.type = new DecisionExprFieldDataType(decisionTableDataType, openl);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return target;
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
        return type;
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
        return SpreadsheetStructureBuilder.DOLLAR_SIGN + "Expr";
    }

}

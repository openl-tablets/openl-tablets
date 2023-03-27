package org.openl.rules.dt.data;

import org.openl.rules.dt.DTColumnsDefinitionField;
import org.openl.rules.dt.Expr;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class ExprParameterDTColumnsDefinitionField implements IOpenField {
    private final DTColumnsDefinitionField dtColumnsDefinitionField;

    public ExprParameterDTColumnsDefinitionField(DTColumnsDefinitionField dtColumnsDefinitionField) {
        this.dtColumnsDefinitionField = dtColumnsDefinitionField;
    }

    @Override
    public String getDisplayName(int mode) {
        return dtColumnsDefinitionField.getDisplayName(mode);
    }

    @Override
    public String getName() {
        return dtColumnsDefinitionField.getName();
    }

    @Override
    public IOpenClass getType() {
        return Expr.EXPR_JAVA_OPEN_CLASS;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return dtColumnsDefinitionField.getInfo();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return dtColumnsDefinitionField.getDeclaringClass();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return dtColumnsDefinitionField.get(target, env);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        dtColumnsDefinitionField.set(target, value, env);
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
    public boolean isContextProperty() {
        return false;
    }

    @Override
    public String getContextProperty() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }
}

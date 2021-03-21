package org.openl.rules.dt;

import java.util.Objects;

import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.DTColumnsDefinitionType;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

class DTColumnsDefinitionField implements IOpenField {

    private final IOpenClass type;
    private final IOpenClass declaringClass;
    private final String name;
    private final String title;
    private final DTColumnsDefinition dtColumnsDefinition;

    DTColumnsDefinitionField(String name,
            IOpenClass type,
            IOpenClass declaringClass,
            DTColumnsDefinition dtColumnsDefinition,
            String title) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.declaringClass = Objects.requireNonNull(declaringClass, "declaringClass cannot be null");
        this.dtColumnsDefinition = Objects.requireNonNull(dtColumnsDefinition, "dtColumnsDefinition cannot be null");
        this.title = title;
    }

    @Override
    public String getDisplayName(int mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(name).append("'");
        if (!StringUtils.isEmpty(title)) {
            sb.append(String.format(" with column title '%s'", title));
        }
        sb.append(" in external ");
        if (dtColumnsDefinition.getType() == DTColumnsDefinitionType.RETURN) {
            sb.append("return");
        } else if (dtColumnsDefinition.getType() == DTColumnsDefinitionType.ACTION) {
            sb.append("action");
        } else if (dtColumnsDefinition.getType() == DTColumnsDefinitionType.CONDITION) {
            sb.append("condition");
        }
        sb.append(" table");
        if (!StringUtils.isEmpty(dtColumnsDefinition.getTableName())) {
            sb.append(" '").append(dtColumnsDefinition.getTableName()).append("'");
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return name;
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
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
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

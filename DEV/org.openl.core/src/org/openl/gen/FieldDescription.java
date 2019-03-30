package org.openl.gen;

import org.openl.gen.writers.DefaultValue;
import org.openl.util.StringUtils;

public class FieldDescription {

    private final TypeDescription typeDescription;
    private String defaultValueAsString;
    private Object defaultValue;

    public FieldDescription(String typeName) {
        this.typeDescription = new TypeDescription(typeName);
    }

    public FieldDescription(String typeName, Object defaultValue, String defaultValueAsString) {
        this(typeName);
        this.defaultValueAsString = defaultValueAsString;
        this.defaultValue = defaultValue;
    }

    public String getTypeName() {
        return typeDescription.getTypeName();
    }

    public String getTypeDescriptor() {
        return typeDescription.getTypeDescriptor();
    }

    public String getDefaultValueAsString() {
        return defaultValueAsString;
    }

    public boolean isArray() {
        return typeDescription.isArray();
    }

    @Override
    public String toString() {
        return typeDescription.toString();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean hasDefaultValue() {
        return StringUtils.isNotBlank(defaultValueAsString) && getDefaultValue() != null;
    }

    public boolean hasDefaultKeyWord() {
        return hasDefaultValue() && DefaultValue.DEFAULT.equals(getDefaultValue());
    }

}

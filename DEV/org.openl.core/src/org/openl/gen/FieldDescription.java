package org.openl.gen;

import org.openl.gen.writers.DefaultValue;
import org.openl.util.StringUtils;

public class FieldDescription {

    private final TypeDescription typeDescription;
    private String defaultValueAsString;
    private Object defaultValue;
    private String contextPropertyName;
    private String xmlName;

    public FieldDescription(String typeName) {
        this.typeDescription = new TypeDescription(typeName);
    }

    public FieldDescription(String typeName,
            Object defaultValue,
            String defaultValueAsString,
            String contextPropertyName) {
        this(typeName, defaultValue, defaultValueAsString, contextPropertyName, null);
    }

    public FieldDescription(String typeName,
            Object defaultValue,
            String defaultValueAsString,
            String contextPropertyName,
            String xmlName) {
        this(typeName);
        this.defaultValueAsString = defaultValueAsString;
        this.defaultValue = defaultValue;
        this.contextPropertyName = contextPropertyName;
        this.xmlName = xmlName;
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

    public boolean isContextProperty() {
        return contextPropertyName != null;
    }

    public boolean isArray() {
        return typeDescription.isArray();
    }

    public String getContextPropertyName() {
        return contextPropertyName;
    }

    public String getXmlName() {
        return xmlName;
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

package org.openl.gen;

import java.util.Collection;
import java.util.function.Consumer;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import org.openl.gen.writers.DefaultValue;
import org.openl.util.StringUtils;

public class FieldDescription {

    private final TypeDescription typeDescription;
    private String defaultValueAsString;
    private Object defaultValue;
    private String contextPropertyName;
    private String xmlName;
    private boolean isTransient;
    private String description;
    private String[] allowableValues;
    private String example;
    private boolean mandatory;
    private final Collection<Consumer<FieldVisitor>> fieldVisitorWriters;
    private final Collection<Consumer<MethodVisitor>> getterVisitorWriters;

    public FieldDescription(String typeName) {
        this.typeDescription = new TypeDescription(typeName);
        this.fieldVisitorWriters = null;
        this.getterVisitorWriters = null;
    }

    public FieldDescription(String typeName, Collection<Consumer<FieldVisitor>> fieldVisitorWriters, Collection<Consumer<MethodVisitor>> getterVisitorWriters) {
        this.typeDescription = new TypeDescription(typeName);
        this.fieldVisitorWriters = fieldVisitorWriters;
        this.getterVisitorWriters = getterVisitorWriters;
    }

    public FieldDescription(String typeName, Object defaultValue, String defaultValueAsString, String contextPropertyName, boolean isTransient) {
        this(typeName, defaultValue, defaultValueAsString, contextPropertyName, null, isTransient);
    }

    public FieldDescription(String typeName, Object defaultValue, String defaultValueAsString, String contextPropertyName, String xmlName, boolean isTransient) {
        this(typeName, defaultValue, defaultValueAsString, contextPropertyName, xmlName, null, null, null, false, isTransient, null, null);
    }

    public FieldDescription(String typeName, Object defaultValue, String defaultValueAsString, String contextPropertyName, String xmlName, String description, String[] allowableValues, String example, boolean mandatory, boolean isTransient) {
        this(typeName, defaultValue, defaultValueAsString, contextPropertyName, xmlName, description, allowableValues, example, mandatory, isTransient, null, null);
    }

    public FieldDescription(String typeName, Object defaultValue, String defaultValueAsString, String contextPropertyName, String xmlName, String description, String[] allowableValues, String example, boolean mandatory, boolean isTransient, Collection<Consumer<FieldVisitor>> fieldVisitorWriters, Collection<Consumer<MethodVisitor>> getterVisitorWriters) {
        this(typeName, fieldVisitorWriters, getterVisitorWriters);
        this.description = description;
        this.defaultValueAsString = defaultValueAsString;
        this.defaultValue = defaultValue;
        this.contextPropertyName = contextPropertyName;
        this.xmlName = xmlName;
        this.isTransient = isTransient;
        this.allowableValues = allowableValues;
        this.example = example;
        this.mandatory = mandatory;
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

    public boolean isTransient() {
        return isTransient;
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

    public Collection<Consumer<FieldVisitor>> getFieldVisitorWriters() {
        return fieldVisitorWriters;
    }

    public Collection<Consumer<MethodVisitor>> getGetterVisitorWriters() {
        return getterVisitorWriters;
    }

    public String getDescription() {
        return description;
    }

    public String[] getAllowableValues() {
        return allowableValues;
    }

    public String getExample() {
        return example;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}

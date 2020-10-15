package org.openl.gen;

import java.util.Collection;
import java.util.Collections;
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
    private final Collection<Consumer<FieldVisitor>> fieldVisitorWriters;
    private final Collection<Consumer<MethodVisitor>> getterVisitorWriters;

    public FieldDescription(String typeName) {
        this.typeDescription = new TypeDescription(typeName);
        this.fieldVisitorWriters = Collections.emptyList();
        this.getterVisitorWriters = Collections.emptyList();
    }

    public FieldDescription(String typeName,
            Collection<Consumer<FieldVisitor>> fieldVisitorWriters,
            Collection<Consumer<MethodVisitor>> getterVisitorWriters) {
        this.typeDescription = new TypeDescription(typeName);
        this.fieldVisitorWriters = fieldVisitorWriters != null ? fieldVisitorWriters : Collections.emptyList();
        this.getterVisitorWriters = getterVisitorWriters != null ? getterVisitorWriters : Collections.emptyList();
    }

    public FieldDescription(String typeName,
            Object defaultValue,
            String defaultValueAsString,
            String contextPropertyName,
            boolean isTransient) {
        this(typeName, defaultValue, defaultValueAsString, contextPropertyName, null, isTransient);
    }

    public FieldDescription(String typeName,
            Object defaultValue,
            String defaultValueAsString,
            String contextPropertyName,
            String xmlName,
            boolean isTransient) {
        this(typeName, defaultValue, defaultValueAsString, contextPropertyName, xmlName, isTransient, null, null);
    }

    public FieldDescription(String typeName,
            Object defaultValue,
            String defaultValueAsString,
            String contextPropertyName,
            String xmlName,
            boolean isTransient,
            Collection<Consumer<FieldVisitor>> fieldVisitorWriters,
            Collection<Consumer<MethodVisitor>> getterVisitorWriters) {
        this(typeName, fieldVisitorWriters, getterVisitorWriters);
        this.defaultValueAsString = defaultValueAsString;
        this.defaultValue = defaultValue;
        this.contextPropertyName = contextPropertyName;
        this.xmlName = xmlName;
        this.isTransient = isTransient;
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
}

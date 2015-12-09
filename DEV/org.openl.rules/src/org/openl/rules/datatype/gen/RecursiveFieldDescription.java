package org.openl.rules.datatype.gen;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * Field description that is used in the Datatype table of the same type.
 * So the type of the field is equal to the Datatype, so there is no
 * java Class analog yet.(As the bytecode for the given datatype is not generated yet)
 *
 * As a workaround take the Object.class as the instance class
 * and provide the canonicalTypeName based on the package and the type name
 *
 * See {link DatatypeTableBoundNode#isRecursiveField(org.openl.types.IOpenField)}
 *
 */
public class RecursiveFieldDescription implements FieldDescription {

    private DefaultFieldDescription field;

    public RecursiveFieldDescription(IOpenField field) {
        Class<?> instanceClass = field.getType().getInstanceClass();
        if (instanceClass == null) {
            instanceClass = Object.class;
        }
        DefaultFieldDescription fieldDescription = new DefaultFieldDescription(instanceClass);
        fieldDescription.setCanonicalTypeName(getCanonicalTypeName(field));
        this.field = fieldDescription;
    }

    private String getCanonicalTypeName(IOpenField field) {
        IOpenClass type = DatatypeTableBoundNode.getRootComponentClass(field.getType());
        if (type instanceof DatatypeOpenClass) {
            if (field.getType().getInstanceClass() == null) {
                String datatypeName = field.getType().getName();
                String packageName = ((DatatypeOpenClass) type).getPackageName();
                if (StringUtils.isBlank(packageName)) {
                    return datatypeName;
                }
                return String.format("%s.%s", packageName, datatypeName);
            }
        }
        throw new IllegalArgumentException("Unknown field got here");
    }

    @Override
    public String getCanonicalTypeName() {
        return field.getCanonicalTypeName();
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }

    @Override
    public String getDefaultValueAsString() {
        return field.getDefaultValueAsString();
    }
    
    @Override
    public void setDefaultValue(Object value) {
        field.setDefaultValue(value);
    }

    @Override
    public void setDefaultValueAsString(String defaultValueAsString) {
        field.setDefaultValueAsString(defaultValueAsString);
    }

    @Override
    public boolean isArray() {
        return field.isArray();
    }

    @Override
    public Object getDefaultValue() {
        return field.getDefaultValue();
    }

    @Override
    public boolean hasDefaultValue() {
        return field.hasDefaultValue();
    }

    @Override
    public boolean hasDefaultKeyWord() {
        return field.hasDefaultKeyWord();
    }
}

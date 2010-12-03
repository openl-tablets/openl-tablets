package org.openl.rules.datatype.gen;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ArrayOpenClass;

public class FieldDescription {
    
    private String typeName;
    private Class<?> type;
    
    private String defaultValueAsString;
    private Object defaultValue;
    
    public FieldDescription(String typeName, Class<?> type) {
        this(typeName, type, null);
    }
    
    public FieldDescription(String typeName, Class<?> type, String defaultValue) {
        this.typeName = typeName;
        this.type = type;
        this.defaultValueAsString = defaultValue;
    }

    public FieldDescription(IOpenField field) {
        this(field, null);
    }
    
    public FieldDescription(IOpenField field, String defaultValue) {
        this.typeName = getTypeName(field);        
        this.type = field.getType().getInstanceClass();
        this.defaultValueAsString = defaultValue;
    }
        
    private String getTypeName(IOpenField field) {        
        String fieldName = field.getType().getName();
        IOpenClass typeDeclaration = getTypeDeclaration(field);
        if (fieldName.indexOf(".") < 0 && typeDeclaration instanceof DatatypeOpenClass) {
            // it means that name of the field has no package. Just datatype can have empty package, in this place.
            // so we need to add it, to the name of type.
            
            String packageName = ((DatatypeOpenClass)typeDeclaration).getPackageName();    
            if (StringUtils.isBlank(packageName)) {
                return fieldName;
            }
            return String.format("%s.%s", packageName, fieldName);
        } else {
            return fieldName;
        }
    }

    private IOpenClass getTypeDeclaration(IOpenField field) {
        IOpenClass fieldType = field.getType();
        IOpenClass typeDeclaration = null;
        if (fieldType.isArray() && fieldType instanceof ArrayOpenClass) { // Array of datatypes
            typeDeclaration = ((ArrayOpenClass)fieldType).getComponentClass();
        } else {
            typeDeclaration = fieldType;
        }
        return typeDeclaration;
    }    

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
    
    public boolean isArray() {
        if (type != null && type.isArray()) {
            return true;
        } else if (typeName.endsWith("]")){
            return true;
        }
        return false;
    }
    
    public String toString() {
        if (StringUtils.isNotBlank(typeName)) {
            return typeName;
        }
        return super.toString();
    }
    
    /**
     * Gets the default value for current field.<br>
     * Converts the stiraging String value to the type of current field (see {@link #getType()}).<br><br
     * > 
     * In case {@link #getType()} method returns one of the primitive classes,<br>
     * the default value will be represented in the wrapper class for this primitive, e.g.<br>
     * {@link #getType()} returns <code>int.class</code> and the default value will be wrapped<br>
     * with {@link Integer}.
     * 
     * 
     */
    public Object getDefaultValue() {
        if (defaultValue == null) {
            if (defaultValueAsString != null) {
                IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(getType());
                defaultValue = convertor.parse(defaultValueAsString, null, null);                
            } 
        }
        return defaultValue;
    }
    
    public String getDefaultValueAsString() {
        return defaultValueAsString;
    }
    
    public static Class<?> getJavaClass(FieldDescription fieldType) {
        Class<?> fieldClass = fieldType.getType();
        if (fieldClass == null) {
            return Object.class;
        } else {
            return fieldClass;
        }
    }
    
}

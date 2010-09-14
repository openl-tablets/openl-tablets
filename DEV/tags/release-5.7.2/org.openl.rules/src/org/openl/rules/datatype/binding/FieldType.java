package org.openl.rules.datatype.binding;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ArrayOpenClass;

public class FieldType {
    
    private String typeName;
    private Class<?> type;
    
    public FieldType(String typeName, Class<?> type) {
        this.typeName = typeName;
        this.type = type;
    }

    public FieldType(IOpenField field) {
        typeName = getTypeName(field);        
        type = field.getType().getInstanceClass();
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
}

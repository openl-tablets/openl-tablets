package org.openl.rules.datatype.gen;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ArrayOpenClass;

public class FieldDescription {

    public static final String DEFAULT_DATATYPE = "_DEFAULT_";
    
    private String canonicalTypeName;
    private Class<?> type;
    
    private String defaultValueAsString;
    private Object defaultValue;
    
    public FieldDescription(Class<?> type) {
        this.type = type;
        this.canonicalTypeName = type.getCanonicalName();
    }
    
    public FieldDescription(IOpenField field) {
        this.canonicalTypeName = getOpenFieldTypeName(field);        
        this.type = field.getType().getInstanceClass();        
    }
    
    private String getOpenFieldTypeName(IOpenField field) {
        IOpenClass typeDeclaration = getOpenFieldTypeDeclaration(field);
        if (typeDeclaration instanceof DatatypeOpenClass) {
            if (field.getType().getInstanceClass() == null) {
              /** it means that name of the field has no package. Just datatype can have empty package, in this place.
                  so we need to add it, to the name of type.
                  Gets the name of the Datatype, e.g. Driver*/
                String datatypeName = field.getType().getName();
                String packageName = ((DatatypeOpenClass) typeDeclaration).getPackageName();
                if (StringUtils.isBlank(packageName)) {
                    return datatypeName;
                }
                return String.format("%s.%s", packageName, datatypeName);
            }
        } 
        return field.getType().getInstanceClass().getCanonicalName();
  }

    private IOpenClass getOpenFieldTypeDeclaration(IOpenField field) {
        IOpenClass fieldType = field.getType();
        IOpenClass typeDeclaration = null;
        if (fieldType.isArray() && fieldType instanceof ArrayOpenClass) { // Array of datatypes
            typeDeclaration = ((ArrayOpenClass)fieldType).getComponentClass();
        } else {
            typeDeclaration = fieldType;
        }
        return typeDeclaration;
    }    

    public String getCanonicalTypeName() {
        return canonicalTypeName;
    }    

    public Class<?> getType() {
        return type;
    } 
    
    public String getDefaultValueAsString() {
		return defaultValueAsString;
	}

	public void setDefaultValueAsString(String defaultValueAsString) {
		this.defaultValueAsString = defaultValueAsString;
	}

	public boolean isArray() {
        if (type != null && type.isArray()) {
            return true;
        } else if (canonicalTypeName.endsWith("]")){
            return true;
        }
        return false;
    }
    
    public String toString() {
        if (StringUtils.isNotBlank(canonicalTypeName)) {
            return canonicalTypeName;
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
                if (DEFAULT_DATATYPE.equals(defaultValueAsString)) {
                    defaultValue = DEFAULT_DATATYPE;
                } else {
                    IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(getType());
                    defaultValue = convertor.parse(defaultValueAsString, null, null);
                }
            }
        }
        return defaultValue;
    }
    
    public static Class<?> getJavaClass(FieldDescription fieldType) {
        Class<?> fieldClass = fieldType.getType();
        if (fieldClass == null) {
            return Object.class;
        } else {
            return fieldClass;
        }
    }
    
    public boolean hasDefaultValue() {
    	if (StringUtils.isNotBlank(defaultValueAsString) && getDefaultValue() != null) {
    		return true;
    	}
    	return false;
    }
    
}

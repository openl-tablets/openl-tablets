package org.openl.rules.datatype.gen;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.types.IOpenField;
import org.openl.util.StringUtils;

public class DefaultFieldDescription implements FieldDescription {

    private String canonicalTypeName;
    private Class<?> type;
    
    private String defaultValueAsString;
    private Object defaultValue;
    
    public DefaultFieldDescription(Class<?> type) {
        this(type, type.getCanonicalName());
    }
    
    public DefaultFieldDescription(IOpenField field) {
        this(field.getType().getInstanceClass(), field.getType().getInstanceClass().getCanonicalName());
    }

    private DefaultFieldDescription(Class<?> type, String canonicalTypeName) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null for the type declaration");
        }
        this.type = type;
        this.canonicalTypeName = canonicalTypeName;
    }

    @Override
    public String getCanonicalTypeName() {
        return canonicalTypeName;
    }

    public void setCanonicalTypeName(String canonicalTypeName) {
        this.canonicalTypeName = canonicalTypeName;
    }

    /**
     * Returns the actual type of the field.
     * Is never null.
     *
     * @return
     */
    @Override
    public Class<?> getType() {
        return type;
    } 
    
    @Override
    public String getDefaultValueAsString() {
		return defaultValueAsString;
	}

	public void setDefaultValueAsString(String defaultValueAsString) {
		this.defaultValueAsString = defaultValueAsString;
	}

	@Override
    public boolean isArray() {
        if (getType().isArray()) {
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
    @Override
    public Object getDefaultValue() {
        if (defaultValue == null) {
            if (defaultValueAsString != null) {
                if (DEFAULT_KEY_WORD.equals(defaultValueAsString)) {
                    // Keep the default value key word for all the types of the field as the default value.
                    //
                    defaultValue = DEFAULT_KEY_WORD;
                } else if (EMPTY_KEY_WORD.equals(defaultValueAsString)) {
                    defaultValue = EMPTY_KEY_WORD;
                } else {
                    IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(getType());
                    defaultValue = convertor.parse(defaultValueAsString, null);
                }
            }
        }
        return defaultValue;
    }
    
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean hasDefaultValue() {
    	if (StringUtils.isNotBlank(defaultValueAsString) && getDefaultValue() != null) {
    		return true;
    	}
    	return false;
    }

    @Override
    public boolean hasDefaultKeyWord() {
        return hasDefaultValue() && DEFAULT_KEY_WORD.equals(getDefaultValue());
    }

    @Override
    public boolean hasEmptyKeyWord() {
        return hasDefaultValue() && EMPTY_KEY_WORD.equals(getDefaultValue());
    }

}

package org.openl.rules.datatype.gen;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.datatype.gen.bean.writers.DefaultValue;
import org.openl.util.StringUtils;

public class FieldDescription {

    private static final Map<String, Class> classMap = new HashMap<String, Class>(8, 1) {
        {
            put("int", int.class);
            put("long", long.class);
            put("char", char.class);
            put("short", short.class);
            put("byte", byte.class);
            put("double", double.class);
            put("float", float.class);
            put("boolean", boolean.class);
        }
    };

    private final String typeName;
    private final String typeDescriptor;
    private String defaultValueAsString;
    private Object defaultValue;

    public FieldDescription(String typeName) {
        this.typeName = typeName;
        if ("byte".equals(typeName)) {
            this.typeDescriptor = "B";
        } else if ("short".equals(typeName)) {
            this.typeDescriptor = "S";
        } else if ("int".equals(typeName)) {
            this.typeDescriptor = "I";
        } else if ("long".equals(typeName)) {
            this.typeDescriptor = "J";
        } else if ("float".equals(typeName)) {
            this.typeDescriptor = "F";
        } else if ("double".equals(typeName)) {
            this.typeDescriptor = "D";
        } else if ("boolean".equals(typeName)) {
            this.typeDescriptor = "Z";
        } else if ("char".equals(typeName)) {
            this.typeDescriptor =  "C";
        } else {
            String internal = typeName;

            if (typeName.charAt(0) != '[') {
                internal = 'L' + internal + ';';
            }
            this.typeDescriptor = internal.replace('.', '/');
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeDescriptor() {
        return typeDescriptor;
    }

    /**
     * Returns the actual type of the field. Is never null.
     *
     * @return
     */
    private Class<?> getType() {
        Class cl = classMap.get(typeName);
        if (cl != null) {
            return cl;
        }
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            return Object.class; // For datatypes
        }
    }

    public String getDefaultValueAsString() {
        return defaultValueAsString;
    }
    public void setDefaultValueAsString(String defaultValueAsString) {
        this.defaultValueAsString = defaultValueAsString;
    }

    public boolean isArray() {
        return typeName.indexOf('[') >= 0;
    }

    public String toString() {
        if (StringUtils.isNotBlank(typeName)) {
            return typeName;
        }
        return super.toString();
    }

    /**
     * Gets the default value for current field.<br>
     * Converts the stiraging String value to the type of current field (see {@link #getType()}).<br>
     * <br >
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
                if (DefaultValue.DEFAULT.equals(defaultValueAsString)) {
                    // Keep the default value key word for all the types of the field as the default value.
                    //
                    defaultValue = DefaultValue.DEFAULT;
                } else {
                    if (typeName.startsWith("[[")) {
                        throw new IllegalStateException("Multi-dimensional arrays aren't supported!");
                    }
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

    public boolean hasDefaultValue() {
        return StringUtils.isNotBlank(defaultValueAsString) && getDefaultValue() != null;
    }

    public boolean hasDefaultKeyWord() {
        return hasDefaultValue() && DefaultValue.DEFAULT.equals(getDefaultValue());
    }

}

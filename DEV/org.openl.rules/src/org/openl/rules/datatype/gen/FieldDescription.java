package org.openl.rules.datatype.gen;

/**
 * Created by dl on 6/19/14.
 */
public interface FieldDescription {
    String getTypeName();
    String getTypeDescriptor();

    String getDefaultValueAsString();

    void setDefaultValueAsString(String defaultValueAsString);
    
    void setDefaultValue(Object value);

    boolean isArray();

    Object getDefaultValue();

    boolean hasDefaultValue();

    boolean hasDefaultKeyWord();
    
}

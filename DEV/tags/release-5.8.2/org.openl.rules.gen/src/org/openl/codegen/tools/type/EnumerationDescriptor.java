package org.openl.codegen.tools.type;

import org.openl.rules.enumeration.properties.EnumPropertyDefinition;

public class EnumerationDescriptor {

    private String enumName;
    
    private EnumPropertyDefinition[] values;
    
    public String getEnumName() {
        return enumName;
    }
    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }
    public EnumPropertyDefinition[] getValues() {
        return values;
    }
    public void setValues(EnumPropertyDefinition[] values) {
        this.values = values;
    }
}

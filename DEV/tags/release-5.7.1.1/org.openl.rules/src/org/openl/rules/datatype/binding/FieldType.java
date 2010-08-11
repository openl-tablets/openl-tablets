package org.openl.rules.datatype.binding;

public class FieldType {
    
    private String typeName;
    private Class<?> type;
    
    public FieldType(String typeName, Class<?> type) {
        this.typeName = typeName;
        this.type = type;
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

}

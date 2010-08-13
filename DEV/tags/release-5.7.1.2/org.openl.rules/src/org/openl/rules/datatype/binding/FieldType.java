package org.openl.rules.datatype.binding;

import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenField;

public class FieldType {
    
    private String typeName;
    private Class<?> type;
    
    public FieldType(String typeName, Class<?> type) {
        this.typeName = typeName;
        this.type = type;
    }

    public FieldType(IOpenField field) {
        String fieldName = field.getType().getName();
        if (fieldName.indexOf(".") < 0) { // it means that name of the field has no namespace. Just datatype can have empty namespace in this place.
                                          // so we need to add our inner namespace, to the name of type.
            typeName = getDatatypeBeanNameWithNamespace(fieldName);
        } else {
            typeName = fieldName;
        }
        type = field.getType().getInstanceClass();
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

    private String getDatatypeBeanNameWithNamespace(String datatypeName) {
        return String.format("%s.%s", ISyntaxConstants.GENERATED_BEANS, datatypeName);        
    }
}

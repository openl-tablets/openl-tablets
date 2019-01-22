package org.openl.gen;

import java.util.Objects;

import org.openl.util.StringUtils;

public class TypeDescription {

    private final String typeName;
    private final String typeDescriptor;

    public TypeDescription(String typeName) {
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
            this.typeDescriptor = "C";
        } else if ("void".equals(typeName)) {
            this.typeDescriptor = "V";
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

    public String toString() {
        if (StringUtils.isNotBlank(typeName)) {
            return typeName;
        }
        return super.toString();
    }

    public boolean isArray() {
        return typeName.indexOf('[') >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeDescription that = (TypeDescription) o;
        return Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }
}

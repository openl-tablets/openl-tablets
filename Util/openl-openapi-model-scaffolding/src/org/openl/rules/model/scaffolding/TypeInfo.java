package org.openl.rules.model.scaffolding;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class TypeInfo {

    @Getter
    @Setter
    private String javaName;
    @Getter
    private final String simpleName;
    @Getter
    @Setter
    private Type type;
    @Getter
    @Setter
    private int dimension;
    @Getter
    private final boolean reference;

    public TypeInfo(Class<?> javaName) {
        this(javaName, javaName.isPrimitive() ? Type.PRIMITIVE : Type.OBJECT);
    }

    public TypeInfo(Class<?> javaName, Type type) {
        this(javaName.getName(), javaName.getSimpleName(), type);
    }

    public TypeInfo(String javaName, String simpleName, Type type) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.type = type;
        this.reference = false;
    }

    public TypeInfo(String javaName, String simpleName, boolean reference, int dimension) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.reference = reference;
        this.dimension = dimension;
    }

    public TypeInfo(String javaName, String simpleName, Type type, int dimension, boolean reference) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.type = type;
        this.dimension = dimension;
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeInfo typeInfo)) {
            return false;
        }
        return type == typeInfo.type && dimension == typeInfo.dimension && reference == typeInfo.reference
                && Objects.equals(javaName, typeInfo.javaName) && Objects.equals(simpleName, typeInfo.simpleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaName, simpleName, type, dimension, reference);
    }

    public enum Type {
        RUNTIMECONTEXT,
        SPREADSHEET,
        SPREADSHEET_ARRAY,
        DATATYPE,
        OBJECT,
        PRIMITIVE
    }
}

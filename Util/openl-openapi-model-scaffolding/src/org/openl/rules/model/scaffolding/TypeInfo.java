package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class TypeInfo {

    private String javaName;
    private final String simpleName;
    private Type type;
    private int dimension;
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

    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isReference() {
        return reference;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeInfo typeInfo = (TypeInfo) o;
        return type == typeInfo.type && Objects.equals(javaName, typeInfo.javaName) && Objects
            .equals(simpleName, typeInfo.simpleName) && Objects.equals(dimension, typeInfo.dimension) && Objects
                .equals(reference, typeInfo.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaName, simpleName, type, dimension, reference);
    }

    public enum Type {

        RUNTIMECONTEXT,
        SPREADSHEET,
        DATATYPE,
        OBJECT,
        PRIMITIVE

    }
}

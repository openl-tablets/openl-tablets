package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class TypeInfo {

    private String javaName;
    private String simpleName;
    private boolean isDatatype;
    private int dimension;
    private boolean isReference;

    public TypeInfo() {
    }

    public TypeInfo(String name, boolean isDatatype) {
        this.javaName = name;
        this.simpleName = name;
        this.isDatatype = isDatatype;
    }

    public TypeInfo(String javaName, String simpleName) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.isDatatype = false;
    }

    public TypeInfo(String javaName, String simpleName, boolean isDatatype) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.isDatatype = isDatatype;
    }

    public TypeInfo(String javaName, String simpleName, boolean isReference, int dimension) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.isReference = isReference;
        this.dimension = dimension;
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

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public boolean isDatatype() {
        return isDatatype;
    }

    public void setIsDatatype(boolean datatype) {
        isDatatype = datatype;
    }

    public boolean isReference() {
        return isReference;
    }

    public void setIsReference(boolean reference) {
        isReference = reference;
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
        return isDatatype == typeInfo.isDatatype && Objects.equals(javaName, typeInfo.javaName) && Objects
            .equals(simpleName, typeInfo.simpleName) && Objects.equals(dimension, typeInfo.dimension) && Objects
                .equals(isReference, typeInfo.isReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaName, simpleName, isDatatype, dimension, isReference);
    }
}

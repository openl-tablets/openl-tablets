package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class TypeInfo {

    private String javaName;
    private String simpleName;
    private boolean datatype;
    private int dimension;
    private boolean reference;

    public TypeInfo() {
    }

    public TypeInfo(String name, boolean datatype) {
        this.javaName = name;
        this.simpleName = name;
        this.datatype = datatype;
    }

    public TypeInfo(String javaName, String simpleName) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.datatype = false;
    }

    public TypeInfo(String javaName, String simpleName, boolean datatype) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.datatype = datatype;
    }

    public TypeInfo(String javaName, String simpleName, boolean reference, int dimension) {
        this.javaName = javaName;
        this.simpleName = simpleName;
        this.reference = reference;
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
        return datatype;
    }

    public void setIsDatatype(boolean datatype) {
        this.datatype = datatype;
    }

    public boolean isReference() {
        return reference;
    }

    public void setIsReference(boolean reference) {
        this.reference = reference;
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
        return datatype == typeInfo.datatype && Objects.equals(javaName, typeInfo.javaName) && Objects
            .equals(simpleName, typeInfo.simpleName) && Objects.equals(dimension, typeInfo.dimension) && Objects
                .equals(reference, typeInfo.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaName, simpleName, datatype, dimension, reference);
    }
}

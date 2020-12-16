package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class ParameterModel implements InputParameter {

    private TypeInfo type;
    private String name;
    private boolean inPath;

    public ParameterModel() {
    }

    public ParameterModel(TypeInfo type, String name) {
        this.type = type;
        this.name = name;
        this.inPath = false;
    }

    public ParameterModel(TypeInfo type, String name, boolean inPath) {
        this.type = type;
        this.name = name;
        this.inPath = inPath;
    }

    public TypeInfo getType() {
        return type;
    }

    public void setType(TypeInfo type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInPath() {
        return inPath;
    }

    public void setInPath(boolean inPath) {
        this.inPath = inPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ParameterModel that = (ParameterModel) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }

        if (inPath != that.inPath) {
            return false;
        }

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (inPath ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

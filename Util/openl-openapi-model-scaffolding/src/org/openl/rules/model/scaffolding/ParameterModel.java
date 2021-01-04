package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class ParameterModel implements InputParameter {

    private final TypeInfo type;
    private final String name;
    private boolean inPath;

    public ParameterModel(TypeInfo type, String name) {
        this.type = type;
        this.name = name;
    }

    public TypeInfo getType() {
        return type;
    }

    public String getName() {
        return name;
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

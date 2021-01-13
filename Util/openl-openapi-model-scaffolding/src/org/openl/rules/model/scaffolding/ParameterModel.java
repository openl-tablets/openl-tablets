package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class ParameterModel implements InputParameter {

    private final TypeInfo type;
    private final String name;
    private In in;

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

    public In getIn() {
        return in;
    }

    public void setIn(In in) {
        this.in = in;
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
        return Objects.equals(type, that.type) && Objects.equals(name, that.name) && in == that.in;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, in);
    }
}

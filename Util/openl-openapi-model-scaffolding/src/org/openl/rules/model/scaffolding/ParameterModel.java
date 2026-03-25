package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class ParameterModel implements InputParameter {

    private final TypeInfo type;
    private final String formattedName;
    private final String originalName;
    private In in;

    public ParameterModel(TypeInfo type, String formattedName) {
        this.type = type;
        this.formattedName = formattedName;
        this.originalName = formattedName;
    }

    public ParameterModel(TypeInfo type, String formattedName, String originalName) {
        this.type = type;
        this.formattedName = formattedName;
        this.originalName = originalName;
    }

    @Override
    public TypeInfo getType() {
        return type;
    }

    @Override
    public String getFormattedName() {
        return formattedName;
    }

    @Override
    public String getOriginalName() {
        return originalName;
    }

    @Override
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
        return Objects.equals(type, that.type) && Objects.equals(formattedName, that.formattedName) && Objects
                .equals(originalName, that.originalName) && in == that.in;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, formattedName, in, originalName);
    }
}

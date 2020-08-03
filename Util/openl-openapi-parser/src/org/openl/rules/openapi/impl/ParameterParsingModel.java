package org.openl.rules.openapi.impl;

import io.swagger.v3.oas.models.media.Schema;

import java.util.Objects;

public class ParameterParsingModel {
    private String name;
    private String type;

    public ParameterParsingModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterParsingModel that = (ParameterParsingModel) o;

        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}

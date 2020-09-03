package org.openl.rules.model.scaffolding;

import java.util.List;
import java.util.Objects;

public class DatatypeModel implements Model{

    private String parent;
    private String name;
    private List<FieldModel> fields;

    public DatatypeModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public void setFields(List<FieldModel> fields) {
        this.fields = fields;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DatatypeModel that = (DatatypeModel) o;

        if (!Objects.equals(parent, that.parent)) return false;
        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }
}

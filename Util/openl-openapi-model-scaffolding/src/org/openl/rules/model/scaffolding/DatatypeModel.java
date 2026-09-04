package org.openl.rules.model.scaffolding;

import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class DatatypeModel implements Model {

    @Getter
    @Setter
    private String parent;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private List<FieldModel> fields;

    public DatatypeModel(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatatypeModel that)) {
            return false;
        }

        if (!Objects.equals(parent, that.parent)) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }
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

package org.openl.rules.context.properties;

import org.openl.rules.table.constraints.Constraints;
import org.openl.types.IOpenClass;

public class ContextPropertyDefinition {

    private String name;

    private IOpenClass type;

    private String description;
    private Constraints constraints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IOpenClass getType() {
        return type;
    }

    public void setType(IOpenClass type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

}

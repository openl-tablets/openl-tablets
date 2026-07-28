package org.openl.rules.context.properties;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.table.constraints.Constraints;
import org.openl.types.IOpenClass;

public class ContextPropertyDefinition {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private IOpenClass type;

    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private Constraints constraints;

}

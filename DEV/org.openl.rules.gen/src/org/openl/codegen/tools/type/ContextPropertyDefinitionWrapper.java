package org.openl.codegen.tools.type;

import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.DataEnumConstraint;

public class ContextPropertyDefinitionWrapper {

    private ContextPropertyDefinition contextPropertyDefinition;

    public ContextPropertyDefinitionWrapper(ContextPropertyDefinition contextPropertyDefinition) {
        this.contextPropertyDefinition = contextPropertyDefinition;
    }

    public ContextPropertyDefinition getDefinition() {
        return contextPropertyDefinition;
    }
}

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

    public boolean isEnum() {
        return org.openl.rules.enumeration.Enum.class
            .equals(contextPropertyDefinition.getType().getInstanceClass()) || org.openl.rules.enumeration.Enum[].class
                .equals(contextPropertyDefinition.getType().getInstanceClass());
    }

    public String getEnumName() {

        Constraints constraints = contextPropertyDefinition.getConstraints();
        Constraint dataEnumConstraint = null;

        for (int i = 0; i < constraints.size(); i++) {

            Constraint constraint = constraints.get(i);

            if (constraint instanceof DataEnumConstraint) {

                if (dataEnumConstraint != null) {
                    throw new RuntimeException("Ambiguous enumearation definitions");
                } else {
                    dataEnumConstraint = constraint;
                }
            }
        }

        if (dataEnumConstraint == null) {
            throw new RuntimeException("Undefined enumeration constraint");
        }

        Object[] params = dataEnumConstraint.getParams();

        return (String) params[0];
    }
}

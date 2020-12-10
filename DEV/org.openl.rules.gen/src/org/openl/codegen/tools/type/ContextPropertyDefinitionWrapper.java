package org.openl.codegen.tools.type;

import org.openl.rules.context.properties.ContextPropertyDefinition;

public class ContextPropertyDefinitionWrapper {

    private final ContextPropertyDefinition contextPropertyDefinition;

    public ContextPropertyDefinitionWrapper(ContextPropertyDefinition contextPropertyDefinition) {
        this.contextPropertyDefinition = contextPropertyDefinition;
    }

    public ContextPropertyDefinition getDefinition() {
        return contextPropertyDefinition;
    }
}

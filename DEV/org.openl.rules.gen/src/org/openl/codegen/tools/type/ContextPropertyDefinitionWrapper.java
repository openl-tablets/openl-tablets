package org.openl.codegen.tools.type;

import lombok.RequiredArgsConstructor;

import org.openl.rules.context.properties.ContextPropertyDefinition;

@RequiredArgsConstructor
public class ContextPropertyDefinitionWrapper {

    private final ContextPropertyDefinition contextPropertyDefinition;

    public ContextPropertyDefinition getDefinition() {
        return contextPropertyDefinition;
    }
}

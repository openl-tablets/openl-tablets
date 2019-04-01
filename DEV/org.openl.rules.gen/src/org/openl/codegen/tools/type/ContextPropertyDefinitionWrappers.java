package org.openl.codegen.tools.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.context.properties.ContextPropertyDefinition;

public class ContextPropertyDefinitionWrappers {

    private Map<String, ContextPropertyDefinitionWrapper> wrappers = new HashMap<>();

    public ContextPropertyDefinitionWrappers(ContextPropertyDefinition[] definitions) {
        init(definitions);
    }

    private void init(ContextPropertyDefinition[] definitions) {

        for (ContextPropertyDefinition definition : definitions) {

            ContextPropertyDefinitionWrapper wrapper = new ContextPropertyDefinitionWrapper(definition);
            wrappers.put(definition.getName(), wrapper);
        }
    }

    public List<ContextPropertyDefinitionWrapper> asList() {
        return new ArrayList<>(wrappers.values());
    }

    public ContextPropertyDefinitionWrapper findWrapper(String contextPropertyName) {
        return wrappers.get(contextPropertyName);
    }
}

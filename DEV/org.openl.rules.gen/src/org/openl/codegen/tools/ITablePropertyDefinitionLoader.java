package org.openl.codegen.tools;

import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinition;

public interface ITablePropertyDefinitionLoader {
    TablePropertyDefinition[] getDefinitions();

    ContextPropertyDefinition[] getContextDefinitions();

    String[] getTablesPriorityRules();

}

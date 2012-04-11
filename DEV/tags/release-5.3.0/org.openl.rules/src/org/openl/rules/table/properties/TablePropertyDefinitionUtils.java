package org.openl.rules.table.properties;

import java.util.ArrayList;
import java.util.List;

public class TablePropertyDefinitionUtils {

    public static String[] getDimensionalTableProperties() {
        
        List<String> names = new ArrayList<String>(); 
        
        TablePropertyDefinition[] definitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        
        for (TablePropertyDefinition definition : definitions) {
            if (definition.isDimensional()) {
                names.add(definition.getName());
            }
        }
        
        return names.toArray(new String[names.size()]);
    }
}

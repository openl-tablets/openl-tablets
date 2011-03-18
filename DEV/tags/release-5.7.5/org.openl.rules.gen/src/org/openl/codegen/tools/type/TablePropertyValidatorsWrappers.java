package org.openl.codegen.tools.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class TablePropertyValidatorsWrappers {

    private Map<String, TablePropertyValidatorsWrapper> wrappers = new HashMap<String, TablePropertyValidatorsWrapper>();

    public TablePropertyValidatorsWrappers(TablePropertyDefinition[] definitions) {
        init(definitions);
    }

    private void init(TablePropertyDefinition[] definitions) {

        for (TablePropertyDefinition definition : definitions) {
            TablePropertyValidatorsWrapper wrapper = new TablePropertyValidatorsWrapper(definition);
            wrappers.put(definition.getName(), wrapper);
        }
    }

    public List<TablePropertyValidatorsWrapper> asList() {
        return new ArrayList<TablePropertyValidatorsWrapper>(wrappers.values());
    }
    
    public TablePropertyValidatorsWrapper findWrapper(String propertyName) {
        return wrappers.get(propertyName);
    }
}

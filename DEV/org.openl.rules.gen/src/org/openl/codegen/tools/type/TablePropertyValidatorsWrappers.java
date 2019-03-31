package org.openl.codegen.tools.type;

import java.util.ArrayList;
import java.util.List;
import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class TablePropertyValidatorsWrappers {

    private List<TablePropertyValidatorsWrapper> wrappers = new ArrayList<>();

    public TablePropertyValidatorsWrappers(TablePropertyDefinition[] definitions) {
        init(definitions);
    }

    private void init(TablePropertyDefinition[] definitions) {

        for (TablePropertyDefinition definition : definitions) {
            wrappers.add(new TablePropertyValidatorsWrapper(definition));
        }
    }

    public List<TablePropertyValidatorsWrapper> asList() {
        return wrappers;
    }
}

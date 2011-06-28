package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class DimensionProperiesColumnMaker {

    public static IDecisionTableColumn makeColumn(TablePropertyDefinition property, DimensionPropertiesRules rules) {
        if (property.getType().getInstanceClass().isArray()) {                    
            return new ArrayDimensionPropertyColumn(property, rules);
        } else {                    
            return new SimpleDimensionPropertyColumn(property, rules);
        }
    }
}

package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class DimensionProperiesCondionsMaker {

    public static IDecisionTableColumn makeCondition(TablePropertyDefinition property, DimensionPropertiesRules rules) {
        if (property.getType().getInstanceClass().isArray()) {                    
            return new ArrayDimensionPropertyCondition(property, rules);
        } else {                    
            return new SimpleDimensionPropertyCondition(property, rules);
        }
    }
}

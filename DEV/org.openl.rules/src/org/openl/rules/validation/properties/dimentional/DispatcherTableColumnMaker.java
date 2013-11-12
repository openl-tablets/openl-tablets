package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class DispatcherTableColumnMaker {
    
    /**
     * Creates a column for dispatcher table, according to the type of dimension property.
     * 
     * @param dimensionProperty
     * @param rules
     * @return column for dispatcher table
     */
    public static IDecisionTableColumn makeColumn(TablePropertyDefinition dimensionProperty, 
            DispatcherTableRules rules) {
        if (dimensionProperty.getType().getInstanceClass().isArray()) {                    
            return new ArrayParameterColumn(dimensionProperty, rules);
        } else {                    
            return new SimpleParameterColumn(dimensionProperty, rules);
        }
    }
}

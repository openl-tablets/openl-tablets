package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.dt.builder.ArrayConditionBuilder;
import org.openl.rules.dt.builder.IDecisionTableColumnBuilder;
import org.openl.rules.dt.builder.SimpleConditionBuilder;
import org.openl.rules.table.properties.def.TablePropertyDefinition;

public final class DispatcherTableColumnMaker {

    private DispatcherTableColumnMaker(){}
    
    /**
     * Creates a column for dispatcher table, according to the type of dimension property.
     * 
     * @param dimensionProperty
     * @param rules
     * @return column for dispatcher table.
     */
    public static IDecisionTableColumn makeColumn(TablePropertyDefinition dimensionProperty, 
            DispatcherTableRules rules) {
        if (dimensionProperty.getType().getInstanceClass().isArray()) {                    
            return new ArrayParameterColumn(dimensionProperty, rules);
        } else {                    
            return new SimpleParameterColumn(dimensionProperty, rules);
        }
    }

    public static IDecisionTableColumnBuilder getConditionBuilder(IDecisionTableColumn condition, int conditionNumber) {
        if (condition.getNumberOfLocalParameters() > 1) {
            return new ArrayConditionBuilder(condition, conditionNumber);
        } else {
            return new SimpleConditionBuilder(condition, conditionNumber);
        }
    }
}

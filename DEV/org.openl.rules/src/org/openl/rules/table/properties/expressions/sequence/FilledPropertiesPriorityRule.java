package org.openl.rules.table.properties.expressions.sequence;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

/**
 * Common priority rule that supports case when several rules match the runtime context. In this case the most filled by
 * properties rule sill be selected.
 * 
 * @author PUdalau
 */
public class FilledPropertiesPriorityRule implements IPriorityRule {

    private static final String[] dimensionProperties = TablePropertyDefinitionUtils
        .getDimensionalTablePropertiesNames();

    @Override
    public int compare(ITableProperties properties1, ITableProperties properties2) {
        return getNumberOfSpecifiedProperties(properties2) - getNumberOfSpecifiedProperties(properties1);
    }

    public static int getNumberOfSpecifiedProperties(ITableProperties properties) {
        int result = 0;
        for (String dimensionPropertyName : dimensionProperties) {
            if (properties.getPropertyValue(dimensionPropertyName) != null) {
                result++;
            }
        }
        return result;
    }

}

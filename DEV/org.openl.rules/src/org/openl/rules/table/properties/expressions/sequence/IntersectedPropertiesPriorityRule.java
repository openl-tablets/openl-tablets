package org.openl.rules.table.properties.expressions.sequence;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.types.impl.DefaultPropertiesIntersectionFinder;

public class IntersectedPropertiesPriorityRule implements IPriorityRule {
    private static final String PROPERTY_NAMES[] = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
    private final DefaultPropertiesIntersectionFinder intersectionMatcher = new DefaultPropertiesIntersectionFinder();
    private final FilledPropertiesPriorityRule filledPropertiesRule = new FilledPropertiesPriorityRule();

    @Override
    public int compare(ITableProperties tableProperties1, ITableProperties tableProperties2) {
        boolean nested = false;
        boolean contains = false;
        propsLoop: for (String propName : PROPERTY_NAMES) {

            switch (intersectionMatcher.match(propName, tableProperties1, tableProperties2)) {
                case NESTED:
                    nested = true;
                    break;
                case CONTAINS:
                    contains = true;
                    break;
                case EQUALS:
                    // do nothing
                    break;
                case NO_INTERSECTION:
                case PARTLY_INTERSECTS:
                    nested = false;
                    contains = false;
                    break propsLoop;
            }
        }
        if (nested && !contains) {
            return -1;
        } else if (contains && !nested) {
            return 1;
        }

        // Not intersected and partly intersected properties cannot be
        // sorted. For such cases for (partly) backward compatibility use
        // the previous version of comparator
        return filledPropertiesRule.compare(tableProperties1, tableProperties2);
    }
}

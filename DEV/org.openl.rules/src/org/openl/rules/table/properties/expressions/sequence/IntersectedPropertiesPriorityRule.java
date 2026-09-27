package org.openl.rules.table.properties.expressions.sequence;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.types.impl.DefaultPropertiesIntersectionFinder;

public class IntersectedPropertiesPriorityRule implements IPriorityRule {
    private static final String[] PROPERTY_NAMES = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
    private final DefaultPropertiesIntersectionFinder intersectionMatcher = new DefaultPropertiesIntersectionFinder();
    private final FilledPropertiesPriorityRule filledPropertiesRule = new FilledPropertiesPriorityRule();

    @Override
    public int compare(ITableProperties tableProperties1, ITableProperties tableProperties2) {
        var result = compareByIntersection(tableProperties1, tableProperties2);
        if (result != 0) {
            return result;
        }

        // Not intersected and partly intersected properties cannot be
        // sorted. For such cases for partial backward compatibility use
        // the previous version of comparator
        return filledPropertiesRule.compare(tableProperties1, tableProperties2);
    }

    /**
     * Compares the tables by how their dimensional properties intersect: the table whose properties are nested into
     * the properties of the other table goes first.
     *
     * @return {@code -1} or {@code 1} for nested properties, or {@code 0} when the properties are equal, not
     * intersected, partly intersected, or nested both ways
     */
    private int compareByIntersection(ITableProperties tableProperties1, ITableProperties tableProperties2) {
        var nested = false;
        var contains = false;
        for (String propName : PROPERTY_NAMES) {

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
                case NO_INTERSECTION, PARTLY_INTERSECTS:
                    return 0;
                default:
                    // an unknown intersection does not affect the order
                    break;
            }
        }
        if (nested && !contains) {
            return -1;
        } else if (contains && !nested) {
            return 1;
        }
        return 0;
    }
}

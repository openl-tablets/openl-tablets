package org.openl.rules.table.properties.expressions.sequence;

import org.openl.rules.table.properties.ITableProperties;

public abstract class ASimplePriorityRule<T extends Comparable<T>> implements IPriorityRule {
    private String tablePropertyName;

    public ASimplePriorityRule(String tablePropertyName) {
        this.tablePropertyName = tablePropertyName;
    }

    public String getTablePropertyName() {
        return tablePropertyName;
    }

    public abstract String getOperationName();

    public abstract T getProprtyValue(ITableProperties properties);

    @Override
    public int compare(ITableProperties properties1, ITableProperties properties2) {
        T propertyValue1 = getProprtyValue(properties1);
        T propertyValue2 = getProprtyValue(properties2);
        if (propertyValue1 == null) {
            if (propertyValue2 == null) {
                return 0;
            } else {
                return 1;
            }
        } else if (propertyValue2 == null) {
            return -1;
        }
        return compareNotNulls(propertyValue1, propertyValue2);
    }

    public abstract int compareNotNulls(T propertyValue1, T propertyValue2);

    public int compare(T value1, T value2) {
        return value1.compareTo(value2);
    }

    public static final String MIN_OPERATION_NAME = "MIN";

    public int MIN(T value1, T value2) {
        return value1.compareTo(value2);
    }

    public static final String MAX_OPERATION_NAME = "MAX";

    public int MAX(T value1, T value2) {
        return value2.compareTo(value1);
    }
}

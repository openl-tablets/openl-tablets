package org.openl.rules.table.properties.expressions.sequence;

import java.util.Comparator;

import org.openl.rules.table.properties.ITableProperties;

public class JavaClassTablesComparator implements IPriorityRule {
    public static final String PREFIX = "javaclass:";

    private Comparator<ITableProperties> comparator;

    public JavaClassTablesComparator(Comparator<ITableProperties> comparator) {
        this.comparator = comparator;
    }

    public String getClassName() {
        return comparator.getClass().getName();
    }

    public Comparator<ITableProperties> getComparator() {
        return comparator;
    }

    @Override
    public int compare(ITableProperties properties1, ITableProperties properties2) {
        return comparator.compare(properties1, properties2);
    }
}

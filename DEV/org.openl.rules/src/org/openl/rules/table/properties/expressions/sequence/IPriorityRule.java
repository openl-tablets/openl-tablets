package org.openl.rules.table.properties.expressions.sequence;

import java.util.Comparator;

import org.openl.rules.table.properties.ITableProperties;

/**
 * Priority rule serves to determine most suitable table to invoke in overloaded methods group.
 * 
 * @author PUdalau
 */
public interface IPriorityRule extends Comparator<ITableProperties> {

    /**
     * @return a negative integer, zero, or a positive integer as the first argument is more prior than, has the same
     *         priority to, or is less prior.
     */
    @Override
    int compare(ITableProperties properties1, ITableProperties properties2);
}

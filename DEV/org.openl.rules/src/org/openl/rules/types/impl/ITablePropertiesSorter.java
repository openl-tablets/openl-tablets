package org.openl.rules.types.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.openl.types.IOpenMethod;

/**
 * Sorts tables candidates in dispatcher to determine the most suitable table to invoke. It is related with
 * DecisionTable concept that defines: The first rule that satisfies conditions will be fired.
 * 
 * So if we have several tables that corresponds current context dispatcher will select the first(sequence defined by
 * the sorter).
 * 
 * @author PUdalau
 */
public interface ITablePropertiesSorter {
    List<IOpenMethod> sort(Collection<IOpenMethod> candidates);

    Comparator<IOpenMethod> getMethodsComparator();
}

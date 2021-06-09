package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.expressions.sequence.IntersectedPropertiesPriorityRule;
import org.openl.types.IOpenMethod;

public class DefaultTablePropertiesSorter implements ITablePropertiesSorter {
    private final List<Comparator<ITableProperties>> maxMinPriorityRules = new ArrayList<>();
    private final List<Comparator<ITableProperties>> tablesPriorityRules = new ArrayList<>();

    private Comparator<IOpenMethod> methodsComparator;

    public DefaultTablePropertiesSorter() {
        initTablesPriorityRules();
        initMethodsComparator();
    }

    private void initTablesPriorityRules() {
        // <<< INSERT >>>
        maxMinPriorityRules.add(Comparator.comparing(ITableProperties::getStartRequestDate, Comparator.nullsLast(Comparator.reverseOrder())));
        maxMinPriorityRules.add(Comparator.comparing(ITableProperties::getEndRequestDate, Comparator.nullsLast(Comparator.naturalOrder())));
        maxMinPriorityRules.add(Comparator.comparing(ITableProperties::getEffectiveDate, Comparator.nullsLast(Comparator.reverseOrder())));
        maxMinPriorityRules.add(Comparator.comparing(ITableProperties::getOrigin, Comparator.nullsLast(Comparator.reverseOrder())));
        // <<< END INSERT >>>
        tablesPriorityRules.addAll(maxMinPriorityRules);
        tablesPriorityRules.add(new IntersectedPropertiesPriorityRule());
    }

    private void initMethodsComparator() {
        methodsComparator = (o1, o2) -> {
            ITableProperties tableProperties1 = PropertiesHelper.getTableProperties(o1);
            ITableProperties tableProperties2 = PropertiesHelper.getTableProperties(o2);
            int comparisonResult = 0;
            for (Comparator<ITableProperties> tablesPriorityRule : tablesPriorityRules) {
                comparisonResult = tablesPriorityRule.compare(tableProperties1, tableProperties2);
                if (comparisonResult != 0) {
                    break;
                }
            }
            return comparisonResult;
        };
    }

    @Override
    public List<IOpenMethod> sort(Collection<IOpenMethod> candidates) {
        List<IOpenMethod> result = new ArrayList<>(candidates);
        result.sort(methodsComparator);
        return result;
    }

    @Override
    public Comparator<IOpenMethod> getMethodsComparator() {
        return methodsComparator;
    }

    public List<Comparator<ITableProperties>> getMaxMinPriorityRules() {
        return maxMinPriorityRules;
    }

}

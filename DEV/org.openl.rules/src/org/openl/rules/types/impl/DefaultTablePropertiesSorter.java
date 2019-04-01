package org.openl.rules.types.impl;

import java.util.*;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.expressions.sequence.ASimplePriorityRule;
import org.openl.rules.table.properties.expressions.sequence.IntersectedPropertiesPriorityRule;
import org.openl.types.IOpenMethod;

public class DefaultTablePropertiesSorter implements ITablePropertiesSorter {
    private List<Comparator<ITableProperties>> maxMinPriorityRules = new ArrayList<>();
    private List<Comparator<ITableProperties>> tablesPriorityRules = new ArrayList<>();

    private Comparator<IOpenMethod> methodsComparator;

    public DefaultTablePropertiesSorter() {
        initTablesPriorityRules();
        initMethodsComparator();
    }

    private void initTablesPriorityRules() {
        // <<< INSERT >>>
        maxMinPriorityRules.add(new ASimplePriorityRule<java.util.Date>("startRequestDate") {

            @Override
            public String getOperationName() {
                return "MAX";
            }

            @Override
            public java.util.Date getProprtyValue(ITableProperties properties) {
                return properties.getStartRequestDate();
            }

            @Override
            public int compareNotNulls(java.util.Date propertyValue1, java.util.Date propertyValue2) {
                return MAX(propertyValue1, propertyValue2);
            }
        });
        maxMinPriorityRules.add(new ASimplePriorityRule<java.util.Date>("endRequestDate") {

            @Override
            public String getOperationName() {
                return "MIN";
            }

            @Override
            public java.util.Date getProprtyValue(ITableProperties properties) {
                return properties.getEndRequestDate();
            }

            @Override
            public int compareNotNulls(java.util.Date propertyValue1, java.util.Date propertyValue2) {
                return MIN(propertyValue1, propertyValue2);
            }
        });
        maxMinPriorityRules.add(new ASimplePriorityRule<org.openl.rules.enumeration.OriginsEnum>("origin") {

            @Override
            public String getOperationName() {
                return "MAX";
            }

            @Override
            public org.openl.rules.enumeration.OriginsEnum getProprtyValue(ITableProperties properties) {
                return properties.getOrigin();
            }

            @Override
            public int compareNotNulls(org.openl.rules.enumeration.OriginsEnum propertyValue1,
                    org.openl.rules.enumeration.OriginsEnum propertyValue2) {
                return MAX(propertyValue1, propertyValue2);
            }
        });
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
        Collections.sort(result, methodsComparator);
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

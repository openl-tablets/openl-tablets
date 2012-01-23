package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.expressions.sequence.ASimplePriorityRule;
import org.openl.types.IOpenMethod;

public class DefaultTablePropertiesSorter implements ITablePropertiesSorter {
    private List<Comparator<ITableProperties>> tablesPriorityRules = new ArrayList<Comparator<ITableProperties>>();

    private Comparator<IOpenMethod> methodsComparator;
    
    public DefaultTablePropertiesSorter(){
        initTablesPriorityRules();
        initMethodsCoparator();
    }

    private void initTablesPriorityRules() {
        // <<< INSERT >>>
        tablesPriorityRules.add(new ASimplePriorityRule<java.util.Date>("startRequestDate") {
            @Override
            public int compare(ITableProperties properties1, ITableProperties properties2) {
                java.util.Date propertyValue1 = getProprtyValue(properties1);
                java.util.Date propertyValue2 = getProprtyValue(properties2);
                if(propertyValue1 == null){
                    if(propertyValue2 == null){
                        return 0;
                    }else{
                        return -1;
                    }
                }else if(propertyValue2 == null){
                    return 1;
                }
                return compareNotNulls(propertyValue1, propertyValue2);
            }
            
            public String getOperationName() {
                return "MAX";
            }

            public java.util.Date getProprtyValue(ITableProperties properties) {
                return properties.getStartRequestDate();
            }

            public int compareNotNulls(java.util.Date propertyValue1, java.util.Date propertyValue2) {
                return MAX (propertyValue1, propertyValue2);
            }
        });
        tablesPriorityRules.add(new ASimplePriorityRule<java.util.Date>("endRequestDate") {
            
            public String getOperationName() {
                return "MIN";
            }

            public java.util.Date getProprtyValue(ITableProperties properties) {
                return properties.getEndRequestDate();
            }

            public int compareNotNulls(java.util.Date propertyValue1, java.util.Date propertyValue2) {
                return MIN (propertyValue1, propertyValue2);
            }
        });
        // <<< END INSERT >>>
    }

    private void initMethodsCoparator() {
        methodsComparator = new Comparator<IOpenMethod>() {
            @Override
            public int compare(IOpenMethod o1, IOpenMethod o2) {
                ITableProperties tableProperties1 = PropertiesHelper.getTableProperties(o1);
                ITableProperties tableProperties2 = PropertiesHelper.getTableProperties(o2);
                int comparisonResult = 0;
                for(Comparator<ITableProperties> tablesPriorityRule : tablesPriorityRules){
                    comparisonResult = tablesPriorityRule.compare(tableProperties1, tableProperties2);
                    if(comparisonResult != 0){
                        break;
                    }
                }
                return comparisonResult;
            }
        };
    }

    @Override
    public List<IOpenMethod> sort(Collection<IOpenMethod> candidates) {
        List<IOpenMethod> result = new ArrayList<IOpenMethod>(candidates);
        Collections.sort(result, methodsComparator);
        return result;
    }

    public Comparator<IOpenMethod> getMethodsComparator() {
        return methodsComparator;
    }
}

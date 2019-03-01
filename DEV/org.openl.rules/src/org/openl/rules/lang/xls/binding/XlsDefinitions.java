package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class XlsDefinitions {

    private Collection<ConditionDefinition> conditionDefinitions = new ArrayList<>();
    private Collection<ReturnDefinition> returnDefinitions = new ArrayList<>();

    public void addConditionDefinition(ConditionDefinition conditionDefinition) {
        this.conditionDefinitions.add(conditionDefinition);
    }

    public void addReturnDefinition(ReturnDefinition returnDefinition) {
        this.returnDefinitions.add(returnDefinition);
    }

    public void addAllConditionDefinitions(Collection<ConditionDefinition> conditionDefinitions) {
        this.conditionDefinitions.addAll(conditionDefinitions);
    }

    public void addAllReturnDefinitions(Collection<ReturnDefinition> returnDefinitions) {
        this.returnDefinitions.addAll(returnDefinitions);
    }

    public Collection<ConditionDefinition> getConditionDefinitions() {
        return Collections.unmodifiableCollection(conditionDefinitions);
    }

    public Collection<ReturnDefinition> getReturnDefinitions() {
        return Collections.unmodifiableCollection(returnDefinitions);
    }

    public void addAllDefinitions(XlsDefinitions xlsModuleDefinitions) {
        addAllConditionDefinitions(xlsModuleDefinitions.getConditionDefinitions());
        addAllReturnDefinitions(xlsModuleDefinitions.getReturnDefinitions());
    }
}

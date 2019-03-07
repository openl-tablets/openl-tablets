package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class XlsDefinitions {

    private Collection<DTColumnDefinition> conditionDefinitions = new LinkedHashSet<>();
    private Collection<DTColumnDefinition> returnDefinitions = new LinkedHashSet<>();

    private static final boolean theSame(DTColumnDefinition dtColumnDefinition1,
            DTColumnDefinition dtColumnDefinition2) {
        if (dtColumnDefinition1.getTitles().length != dtColumnDefinition2.getTitles().length) {
            return false;
        }
        if (dtColumnDefinition1.getNumberOfParameters() != dtColumnDefinition2.getNumberOfParameters()) {
            return false;
        }
        if (dtColumnDefinition1.getHeader().getSignature().getNumberOfParameters() != dtColumnDefinition2.getHeader()
            .getSignature()
            .getNumberOfParameters()) {
            return false;
        }
        
        String dtColumnDefinition1Code = dtColumnDefinition1.getCompositeMethod().getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
        String dtColumnDefinition2Code = dtColumnDefinition2.getCompositeMethod().getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
        if (!Objects.equals(dtColumnDefinition1Code, dtColumnDefinition2Code)) {
            return false;
        }
        
        for (int i = 0; i < dtColumnDefinition1.getTitles().length; i++) {
            String title1 = dtColumnDefinition1.getTitles()[i];
            String title2 = dtColumnDefinition2.getTitles()[i];
            if (!Objects.equals(title1, title2)) {
                return false;
            }
        }
        Map<String, IOpenClass> map = new HashMap<>();
        for (int i = 0; i < dtColumnDefinition1.getHeader().getSignature().getNumberOfParameters(); i++) {
            map.put(dtColumnDefinition1.getHeader().getSignature().getParameterName(i),
                dtColumnDefinition1.getHeader().getSignature().getParameterType(i));
        }
        for (int i = 0; i < dtColumnDefinition2.getHeader().getSignature().getNumberOfParameters(); i++) {
            IOpenClass type = map.get(dtColumnDefinition2.getHeader().getSignature().getParameterName(i));
            if (type == null || !type.equals(dtColumnDefinition2.getHeader().getSignature().getParameterType(i))) {
                return false;
            }
        }
        for (int i = 0; i < dtColumnDefinition1.getNumberOfParameters(); i++) {
            IParameterDeclaration parameterDeclaration1 = dtColumnDefinition1.getParameterDeclarations()[i];
            IParameterDeclaration parameterDeclaration2 = dtColumnDefinition2.getParameterDeclarations()[i];
            if (parameterDeclaration1 == null || parameterDeclaration2 == null) {
                if (parameterDeclaration1 == null && parameterDeclaration2 == null) {
                    continue;
                }
                return false;
            }
            if (!Objects.equals(parameterDeclaration1.getName(), parameterDeclaration2.getName()) || !Objects
                .equals(parameterDeclaration1.getType(), parameterDeclaration2.getType())) {
                return false;
            }
        }

        return true;
    }

    public void addConditionDefinition(DTColumnDefinition conditionDefinition) {
        if (conditionDefinitions.contains(conditionDefinition)) {
            return;
        }
        for (DTColumnDefinition cd : conditionDefinitions) {
            if (theSame(cd, conditionDefinition)) {
                return;
            }
        }
        this.conditionDefinitions.add(conditionDefinition);
    }

    public void addReturnDefinition(DTColumnDefinition returnDefinition) {
        if (returnDefinitions.contains(returnDefinition)) {
            return;
        }
        for (DTColumnDefinition rd : returnDefinitions) {
            if (theSame(rd, returnDefinition)) {
                return;
            }
        }
        this.returnDefinitions.add(returnDefinition);
    }

    public void addAllConditionDefinitions(Collection<DTColumnDefinition> conditionDefinitions) {
        for (DTColumnDefinition conditionDefinition : conditionDefinitions) {
            addConditionDefinition(conditionDefinition);
        }
    }

    public void addAllReturnDefinitions(Collection<DTColumnDefinition> returnDefinitions) {
        for (DTColumnDefinition returnDefinition : returnDefinitions) {
            addReturnDefinition(returnDefinition);
        }
    }

    public Collection<DTColumnDefinition> getConditionDefinitions() {
        return Collections.unmodifiableCollection(conditionDefinitions);
    }

    public Collection<DTColumnDefinition> getReturnDefinitions() {
        return Collections.unmodifiableCollection(returnDefinitions);
    }

    public void addAllDefinitions(XlsDefinitions xlsModuleDefinitions) {
        addAllConditionDefinitions(xlsModuleDefinitions.getConditionDefinitions());
        addAllReturnDefinitions(xlsModuleDefinitions.getReturnDefinitions());
    }
}

package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class XlsDefinitions {

    private Collection<DTColumnsDefinition> dtColumnsDefinitions = new LinkedHashSet<>();

    private static final boolean theSame(DTColumnsDefinition dtColumnDefinition1,
            DTColumnsDefinition dtColumnDefinition2) {
        if (!Objects.equals(dtColumnDefinition1.getType(), dtColumnDefinition2.getType())) {
            return false;
        }
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

        String dtColumnDefinition1Code = dtColumnDefinition1.getCompositeMethod()
            .getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
        String dtColumnDefinition2Code = dtColumnDefinition2.getCompositeMethod()
            .getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
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

    public void addDtColumnsDefinition(DTColumnsDefinition dtColumnsDefinition) {
        if (dtColumnsDefinitions.contains(dtColumnsDefinition)) {
            return;
        }
        for (DTColumnsDefinition cd : dtColumnsDefinitions) {
            if (theSame(cd, dtColumnsDefinition)) {
                return;
            }
        }
        this.dtColumnsDefinitions.add(dtColumnsDefinition);
    }

    public void addAllDtColumnsDefinitions(Collection<DTColumnsDefinition> dtColumnsDefinitions) {
        for (DTColumnsDefinition dtColumnsDefinition : dtColumnsDefinitions) {
            addDtColumnsDefinition(dtColumnsDefinition);
        }
    }

    public Collection<DTColumnsDefinition> getDtColumnsDefinitions() {
        return Collections.unmodifiableCollection(dtColumnsDefinitions);
    }

    public Collection<DTColumnsDefinition> getConditionDefinitions() {
        return dtColumnsDefinitions.stream()
            .filter(e -> DTColumnsDefinitionType.CONDITION.equals(e.getType()))
            .collect(Collectors.toList());
    }

    public Collection<DTColumnsDefinition> getActionDefinitions() {
        return dtColumnsDefinitions.stream()
            .filter(e -> DTColumnsDefinitionType.ACTION.equals(e.getType()))
            .collect(Collectors.toList());
    }

    public Collection<DTColumnsDefinition> getReturnDefinitions() {
        return dtColumnsDefinitions.stream()
            .filter(e -> DTColumnsDefinitionType.RETURN.equals(e.getType()))
            .collect(Collectors.toList());
    }

    public void addAll(XlsDefinitions xlsModuleDefinitions) {
        addAllDtColumnsDefinitions(xlsModuleDefinitions.dtColumnsDefinitions);
    }
}

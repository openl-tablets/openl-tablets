package org.openl.rules.lang.xls.binding;

import java.util.*;
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
        if (dtColumnDefinition1.getNumberOfTitles() != dtColumnDefinition2.getNumberOfTitles()) {
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

        Set<String> titles1 = dtColumnDefinition1.getTitles();
        Set<String> titles2 = dtColumnDefinition1.getTitles();
        for (String title : titles1) {
            if (!titles2.contains(title)) {
                return false;
            }
            List<IParameterDeclaration> parameterDeclarations1 = dtColumnDefinition1.getLocalParameters(title);
            List<IParameterDeclaration> parameterDeclarations2 = dtColumnDefinition2.getLocalParameters(title);
            if (parameterDeclarations1.size() != parameterDeclarations2.size()) {
                return false;
            }
            for (int i = 0; i < parameterDeclarations1.size(); i++) {
                IParameterDeclaration parameterDeclaration1 = parameterDeclarations1.get(0);
                IParameterDeclaration parameterDeclaration2 = parameterDeclarations2.get(0);
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

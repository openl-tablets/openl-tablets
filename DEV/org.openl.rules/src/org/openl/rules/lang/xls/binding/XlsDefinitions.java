package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class XlsDefinitions {

    private final Collection<DTColumnsDefinition> dtColumnsDefinitions = new LinkedHashSet<>();

    private static boolean theSame(DTColumnsDefinition dtColumnDefinition1, DTColumnsDefinition dtColumnDefinition2) {
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
        if (!Objects.equals(dtColumnDefinition1.getExpression(), dtColumnDefinition2.getExpression())) {
            return false;
        }
        if (!haveSameHeaderParameters(dtColumnDefinition1, dtColumnDefinition2)) {
            return false;
        }

        Set<String> titles1 = dtColumnDefinition1.getTitles();
        Set<String> titles2 = dtColumnDefinition1.getTitles();
        for (String title : titles1) {
            if (!titles2.contains(title)) {
                return false;
            }
            var parameterDeclarations1 = dtColumnDefinition1.getParameters(title);
            var parameterDeclarations2 = dtColumnDefinition2.getParameters(title);
            if (!haveSameParameterDeclarations(parameterDeclarations1, parameterDeclarations2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks that each header parameter of the second definition is a header parameter of the first one with the
     * same name and type.
     */
    private static boolean haveSameHeaderParameters(DTColumnsDefinition dtColumnDefinition1,
                                                    DTColumnsDefinition dtColumnDefinition2) {
        var map = new HashMap<String, IOpenClass>();
        for (var i = 0; i < dtColumnDefinition1.getHeader().getSignature().getNumberOfParameters(); i++) {
            map.put(dtColumnDefinition1.getHeader().getSignature().getParameterName(i),
                    dtColumnDefinition1.getHeader().getSignature().getParameterType(i));
        }
        for (var i = 0; i < dtColumnDefinition2.getHeader().getSignature().getNumberOfParameters(); i++) {
            var type = map.get(dtColumnDefinition2.getHeader().getSignature().getParameterName(i));
            if (type == null || !type.equals(dtColumnDefinition2.getHeader().getSignature().getParameterType(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean haveSameParameterDeclarations(List<IParameterDeclaration> parameterDeclarations1,
                                                         List<IParameterDeclaration> parameterDeclarations2) {
        if (parameterDeclarations1.size() != parameterDeclarations2.size()) {
            return false;
        }
        for (var i = 0; i < parameterDeclarations1.size(); i++) {
            var parameterDeclaration1 = parameterDeclarations1.getFirst();
            var parameterDeclaration2 = parameterDeclarations2.getFirst();
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

    public void addAll(XlsDefinitions xlsModuleDefinitions) {
        addAllDtColumnsDefinitions(xlsModuleDefinitions.dtColumnsDefinitions);
    }
}

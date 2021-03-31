package org.openl.rules.dt;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.text.TextInfo;

class MatchedDefinition {
    final List<IdentifierNode> identifierNodes;
    final String statement;
    final int[] usedMethodParameterIndexes;
    final MatchType matchType;
    Map<String, String> parametersToRename;
    Map<String, String> externalParametersToRename;
    final Map<String, String> methodParametersToRename;
    final DTColumnsDefinition dtColumnsDefinition;
    boolean parametersRenamingIsUsed = false;

    public MatchedDefinition(DTColumnsDefinition dtColumnsDefinition,
            String statement,
            int[] usedMethodParameterIndexes,
            Map<String, String> methodParametersToRename,
            List<IdentifierNode> identifierNodes,
            MatchType matchType) {
        super();
        this.dtColumnsDefinition = dtColumnsDefinition;
        this.statement = statement;
        this.usedMethodParameterIndexes = usedMethodParameterIndexes;
        this.matchType = matchType;
        this.methodParametersToRename = methodParametersToRename;
        this.identifierNodes = identifierNodes;
    }

    public void renameParameterName(String name, String newName) {
        if (name != null) {
            if (parametersToRename == null) {
                parametersToRename = new HashMap<>();
            }
            parametersToRename.put(name.toLowerCase(), newName);
            parametersRenamingIsUsed = true;
        }
    }

    public void renameExternalParameter(String name, String newName) {
        if (name != null) {
            if (externalParametersToRename == null) {
                externalParametersToRename = new HashMap<>();
            }
            // External parameter is case-sensitive
            externalParametersToRename.put(name, newName);
            parametersRenamingIsUsed = true;
        }
    }

    public String getParameter(String name) {
        if (parametersToRename == null || name == null) {
            return name;
        }
        String newName = parametersToRename.get(name.toLowerCase());
        return newName != null ? newName : name;
    }

    public String getExternalParameter(String name) {
        if (externalParametersToRename == null || name == null) {
            return name;
        }
        // External parameter is case-sensitive
        String newName = externalParametersToRename.get(name);
        return newName != null ? newName : name;
    }

    public DTColumnsDefinition getDtColumnsDefinition() {
        return dtColumnsDefinition;
    }

    public String getStatementWithReplacedIdentifiers() {
        return replaceIdentifierNodeNamesInCode(statement,
            identifierNodes,
            Pair.of(methodParametersToRename, true),
            Pair.of(externalParametersToRename, false),
            Pair.of(parametersToRename, true));
    }

    public int[] getUsedMethodParameterIndexes() {
        return usedMethodParameterIndexes;
    }

    public MatchType getMatchType() {
        switch (matchType) {
            case STRICT:
                return parametersRenamingIsUsed ? MatchType.STRICT_PARAMS_RENAMED : matchType;
            case STRICT_CASTED:
                return parametersRenamingIsUsed ? MatchType.STRICT_CASTED_PARAMS_RENAMED : matchType;
            case METHOD_ARGS_RENAMED:
                return parametersRenamingIsUsed ? MatchType.METHOD_ARGS_AND_PARAMS_RENAMED : matchType;
            case METHOD_ARGS_RENAMED_CASTED:
                return parametersRenamingIsUsed ? MatchType.METHOD_ARGS_AND_PARAMS_RENAMED_CASTED : matchType;
            default:
                return matchType;
        }
    }

    @SafeVarargs
    static String replaceIdentifierNodeNamesInCode(String code,
            List<IdentifierNode> identifierNodes,
            Pair<Map<String, String>, Boolean>... namesMaps) {
        final TextInfo textInfo = new TextInfo(code);
        identifierNodes.sort(
            Comparator.<IdentifierNode> comparingInt(e -> e.getLocation().getStart().getAbsolutePosition(textInfo))
                .reversed());
        StringBuilder sb = new StringBuilder(code);
        for (IdentifierNode identifierNode : identifierNodes) {
            int start = identifierNode.getLocation().getStart().getAbsolutePosition(textInfo);
            int end = identifierNode.getLocation().getEnd().getAbsolutePosition(textInfo);
            for (Pair<Map<String, String>, Boolean> m : namesMaps) {
                if (m != null && m.getKey() != null && m.getKey()
                    .containsKey(
                        identifierNode.getIdentifier() != null
                                                               ? (Boolean.TRUE.equals(m.getValue()) ? identifierNode
                                                                   .getIdentifier()
                                                                   .toLowerCase() : identifierNode.getIdentifier())
                                                               : null)) {
                    sb.replace(start,
                        end + 1,
                        m.getKey()
                            .get(identifierNode.getIdentifier() != null ? (Boolean.TRUE
                                .equals(m.getValue()) ? identifierNode.getIdentifier().toLowerCase()
                                                      : identifierNode.getIdentifier()) : null));
                }
            }
        }
        return sb.toString();
    }
}

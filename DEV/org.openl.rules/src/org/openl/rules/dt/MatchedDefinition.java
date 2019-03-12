package org.openl.rules.dt;

import java.util.Map;

import org.openl.rules.lang.xls.binding.DTColumnsDefinition;

class MatchedDefinition {
    String statement;
    int[] usedMethodParameterIndexes;
    MatchType matchType;
    Map<String, String> renamedLocalParameters;
    DTColumnsDefinition dtColumnsDefinition;

    public MatchedDefinition(DTColumnsDefinition dtColumnsDefinition,
            String statement,
            int[] usedMethodParameterIndexes,
            Map<String, String> renamedLocalParameters,
            MatchType matchType) {
        super();
        this.dtColumnsDefinition = dtColumnsDefinition;
        this.statement = statement;
        this.usedMethodParameterIndexes = usedMethodParameterIndexes;
        this.matchType = matchType;
        this.renamedLocalParameters = renamedLocalParameters;
    }

    public String getLocalParameterName(String name) {
        if (renamedLocalParameters == null) {
            return name;
        }
        String newName = renamedLocalParameters.get(name);
        return newName != null ? newName : name;
    }

    public DTColumnsDefinition getDtColumnsDefinition() {
        return dtColumnsDefinition;
    }

    public String getStatement() {
        return statement;
    }

    public int[] getUsedMethodParameterIndexes() {
        return usedMethodParameterIndexes;
    }

    public MatchType getMatchType() {
        return matchType;
    }
}

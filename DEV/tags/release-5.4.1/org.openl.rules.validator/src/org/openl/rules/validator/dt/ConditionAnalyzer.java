package org.openl.rules.validator.dt;

import org.openl.domain.IDomain;
import org.openl.rules.dt.IDecisionRow;
import org.openl.types.IParameterDeclaration;

public class ConditionAnalyzer {
    private IDecisionRow condition;

    public ConditionAnalyzer(IDecisionRow condition) {
        this.condition = condition;
    }

    public IDomain<?> getParameterDomain(String parameterName) {
        IParameterDeclaration[] pdd = condition.getParams();
        for (int i = 0; i < pdd.length; i++) {
            if (pdd[i].getName().equals(parameterName)) {
                return pdd[i].getType().getDomain();
            }
        }
        return null;
    }

}

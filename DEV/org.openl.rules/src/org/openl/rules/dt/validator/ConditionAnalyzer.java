package org.openl.rules.dt.validator;

import org.openl.domain.IDomain;
import org.openl.rules.dt.IBaseDecisionRow;
import org.openl.types.IParameterDeclaration;

public class ConditionAnalyzer {

    private IBaseDecisionRow condition;

    public ConditionAnalyzer(IBaseDecisionRow condition) {
        this.condition = condition;
    }

    public IDomain<?> getParameterDomain(String parameterName) {

        IParameterDeclaration[] parametersDeclaration = condition.getParams();

        for (IParameterDeclaration paramDeclaration : parametersDeclaration) {
            if (paramDeclaration.getName().equals(parameterName)) {
                return paramDeclaration.getType().getDomain();
            }
        }

        return null;
    }

}

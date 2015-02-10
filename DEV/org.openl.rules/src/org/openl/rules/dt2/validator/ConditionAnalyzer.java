package org.openl.rules.dt2.validator;

import org.openl.domain.IDomain;
import org.openl.rules.dt2.element.IDecisionRow;
import org.openl.types.IParameterDeclaration;

public class ConditionAnalyzer {
    
    private IDecisionRow condition;

    public ConditionAnalyzer(IDecisionRow condition) {
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

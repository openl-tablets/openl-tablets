package org.openl.rules.dt.validator;

import lombok.RequiredArgsConstructor;

import org.openl.domain.IDomain;
import org.openl.rules.dt.IBaseDecisionRow;
import org.openl.types.IParameterDeclaration;

@RequiredArgsConstructor
public class ConditionAnalyzer {

    private final IBaseDecisionRow condition;

    // The domain comes from IType.getDomain(), whose values have a type known only at run time.
    @SuppressWarnings("java:S1452")
    public IDomain<?> getParameterDomain(String parameterName) {

        var parametersDeclaration = condition.getParams();

        for (IParameterDeclaration paramDeclaration : parametersDeclaration) {
            if (paramDeclaration.getName().equals(parameterName)) {
                return paramDeclaration.getType().getDomain();
            }
        }

        return null;
    }

}

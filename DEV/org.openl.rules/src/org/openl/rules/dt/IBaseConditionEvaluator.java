package org.openl.rules.dt;

import org.openl.domain.IDomain;
import org.openl.rules.dt.algorithm.evaluator.DomainCanNotBeDefined;
import org.openl.source.IOpenSourceCodeModule;

public interface IBaseConditionEvaluator {

    IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition);

    // Each evaluator builds a domain of its own value type, such as integer ranges or any indexed values.
    @SuppressWarnings("java:S1452")
    IDomain<?> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined;

    // Each evaluator builds a domain of its own value type, such as integer ranges or any indexed values.
    @SuppressWarnings("java:S1452")
    IDomain<?> getConditionParameterDomain(int i, IBaseCondition condition) throws DomainCanNotBeDefined;

}

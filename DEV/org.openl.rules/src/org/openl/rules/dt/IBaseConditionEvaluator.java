package org.openl.rules.dt;

import org.openl.domain.IDomain;
import org.openl.rules.dt.algorithm.evaluator.DomainCanNotBeDefined;
import org.openl.source.IOpenSourceCodeModule;

public interface IBaseConditionEvaluator {

    IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition);

    IDomain<?> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined;

    IDomain<?> getConditionParameterDomain(int i, IBaseCondition condition) throws DomainCanNotBeDefined;

}

package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IDomain;
import org.openl.rules.dt.element.ICondition;
import org.openl.types.IMethodCaller;
import org.openl.types.impl.ParameterMethodCaller;

public abstract class AConditionEvaluator implements IConditionEvaluator {

    public IDomain<? extends Object> getRuleParameterDomain(ICondition condition) throws DomainCanNotBeDefined {
        IMethodCaller mc = condition.getEvaluator();
        if (mc instanceof ParameterMethodCaller)
            return indexedDomain(condition);
        throw new DomainCanNotBeDefined("Not a Simple Expression", getFormalSourceCode(condition).getCode());
    }
    
    public IDomain<?> getConditionParameterDomain(int paramIdx, ICondition condition) throws DomainCanNotBeDefined {
        return indexedDomain(condition);
    }
    
    

    protected abstract IDomain<? extends Object> indexedDomain(ICondition condition) throws DomainCanNotBeDefined;    
    
}

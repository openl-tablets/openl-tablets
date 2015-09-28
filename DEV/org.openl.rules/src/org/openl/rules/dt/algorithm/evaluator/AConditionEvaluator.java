package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IDomain;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.dtx.algorithm.evaluator.DomainCanNotBeDefined;
import org.openl.types.IMethodCaller;
import org.openl.types.impl.ParameterMethodCaller;

public abstract class AConditionEvaluator implements IConditionEvaluator {

    public IDomain<? extends Object> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined  {
        IMethodCaller mc = condition.getEvaluator();
        if (mc instanceof ParameterMethodCaller)
            return indexedDomain(condition);
        throw new DomainCanNotBeDefined("Not a Simple Expression", getFormalSourceCode(condition).getCode());
    }
    
    public IDomain<?> getConditionParameterDomain(int paramIdx, IBaseCondition condition) throws DomainCanNotBeDefined {
        return indexedDomain(condition);
    }

    protected abstract IDomain<? extends Object> indexedDomain(IBaseCondition condition) throws DomainCanNotBeDefined;    

    
    //Added to support dependent parameters
    
    private String optimizedSourceCode;


	public String getOptimizedSourceCode() {
		return optimizedSourceCode;
	}

	public void setOptimizedSourceCode(String optimizedSourceCode) {
		this.optimizedSourceCode = optimizedSourceCode;
	}
    

    
}

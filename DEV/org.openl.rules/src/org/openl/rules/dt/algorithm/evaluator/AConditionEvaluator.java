package org.openl.rules.dt.algorithm.evaluator;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.IDomain;
import org.openl.rules.dt.IBaseCondition;
import org.openl.types.IMethodCaller;
import org.openl.types.impl.ParameterMethodCaller;

public abstract class AConditionEvaluator implements IConditionEvaluator {

    private String optimizedSourceCode;

    protected IOpenCast paramToExpressionOpenCast;
    protected IOpenCast expressionToParamOpenCast;

    public AConditionEvaluator(IOpenCast paramToExpressionOpenCast, IOpenCast expressionToParamOpenCast) {
        this.paramToExpressionOpenCast = paramToExpressionOpenCast;
        this.expressionToParamOpenCast = expressionToParamOpenCast;
    }

    protected Object convertWithExpressionToParamOpenCast(Object value) {
        if (expressionToParamOpenCast != null && expressionToParamOpenCast.isImplicit()) {
            return expressionToParamOpenCast.convert(value);
        }
        return value;
    }

    protected Object convertWithParamToExpressionOpenCast(Object value) {
        if (paramToExpressionOpenCast != null && paramToExpressionOpenCast.isImplicit()) {
            return paramToExpressionOpenCast.convert(value);
        }
        return value;
    }

    @Override
    public IDomain<? extends Object> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        IMethodCaller mc = condition.getEvaluator();
        if (mc instanceof ParameterMethodCaller) {
            return indexedDomain(condition);
        }
        throw new DomainCanNotBeDefined("Not a Simple Expression", getFormalSourceCode(condition).getCode());
    }

    @Override
    public IDomain<?> getConditionParameterDomain(int paramIdx, IBaseCondition condition) throws DomainCanNotBeDefined {
        return indexedDomain(condition);
    }

    // Added to support dependent parameters

    protected abstract IDomain<? extends Object> indexedDomain(IBaseCondition condition) throws DomainCanNotBeDefined;

    @Override
    public String getOptimizedSourceCode() {
        return optimizedSourceCode;
    }

    @Override
    public void setOptimizedSourceCode(String optimizedSourceCode) {
        this.optimizedSourceCode = optimizedSourceCode;
    }

}

package org.openl.rules.dt.algorithm.evaluator;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import org.openl.domain.IDomain;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.types.impl.ParameterMethodCaller;

public abstract class AConditionEvaluator implements IConditionEvaluator {

    @Getter
    @Setter
    private String optimizedSourceCode;

    protected final ConditionCasts conditionCasts;

    public AConditionEvaluator(ConditionCasts conditionCasts) {
        this.conditionCasts = Objects.requireNonNull(conditionCasts, "conditionCasts cannot be null");
    }

    @Override
    public IDomain<? extends Object> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        var mc = condition.getEvaluator();
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

}

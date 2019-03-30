package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class DefaultConditionEvaluator implements IConditionEvaluator {

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        return condition.getSourceCodeModule();
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        return new DefaultConditionSelector(condition, target, dtparams, env);
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    /**
     * No indexing for default evaluator
     */
    @Override
    public ARuleIndex makeIndex(ICondition condition, IIntIterator it) {
        throw new UnsupportedOperationException("The evaluator does not support indexing");
    }

    @Override
    public IDomain<?> getRuleParameterDomain(IBaseCondition condition) throws DomainCanNotBeDefined {
        throw new DomainCanNotBeDefined("Non-indexed Evaluator", getFormalSourceCode(condition).getCode());
    }

    @Override
    public IDomain<?> getConditionParameterDomain(int paramIdx, IBaseCondition condition) throws DomainCanNotBeDefined {
        return null;
    }

    @Override
    public String getOptimizedSourceCode() {
        return null;
    }

    @Override
    public void setOptimizedSourceCode(String code) {
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.DEFAULT_CONDITION_PRIORITY;
    }
}

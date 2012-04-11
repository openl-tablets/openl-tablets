package org.openl.rules.dt;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * Created Jul 11, 2007
 */

/**
 * @author snshor
 *
 */
public class DefaultConditionEvaluator implements IDTConditionEvaluator {

    class DefaultConditionSelector implements IIntSelector {

        IDTCondition condition;
        Object target;
        Object[] dtparams;
        IRuntimeEnv env;

        public DefaultConditionSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
            this.condition = condition;
            this.target = target;
            this.dtparams = dtparams;
            this.env = env;
        }

        public boolean select(int rule) {
            return evaluateConditionExpression(condition, rule, target, dtparams, env);
        }

    }

    public boolean evaluateConditionExpression(IDTCondition condition, int rule, Object target, Object[] dtparams,
            IRuntimeEnv env) {
        return condition.calculateCondition(rule, target, dtparams, env).getBooleanValue();
    }

    public IOpenSourceCodeModule getFormalSourceCode(IDTCondition condition) {
        return condition.getSourceCodeModule();
    }

    public IIntSelector getSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        return new DefaultConditionSelector(condition, target, dtparams, env);
    }

    public boolean isIndexed() {
        return false;
    }

    /**
     * No indexing for default evaluator
     */
    public ADTRuleIndex makeIndex(Object[][] indexedParams, IIntIterator it) {
        throw new UnsupportedOperationException("The evaluator does not support indexing");
    }

}

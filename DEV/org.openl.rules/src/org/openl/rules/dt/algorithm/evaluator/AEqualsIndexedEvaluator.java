package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.HashSet;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

abstract class AEqualsIndexedEvaluator extends AConditionEvaluator {

    protected AEqualsIndexedEvaluator(ConditionCasts conditionCasts) {
        super(conditionCasts);
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        var condSource = condition.getSourceCodeModule();
        return new StringSourceCodeModule("(" + condSource.getCode() + ") == " + condition.getParams()[0].getName(),
                condSource.getUri());
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        var value = condition.getEvaluator().invoke(target, dtparams, env);
        // an empty value matches no rule, as in the index, rather than the value a cast makes of it
        return new EqualsSelector(condition,
                value == null ? null : conditionCasts.castToConditionType(value),
                target,
                dtparams,
                env);
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        var len = condition.getNumberOfRules();
        var list = new ArrayList<Object>(len);
        var set = HashSet.<Object>newHashSet(len);

        for (var ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }
            var key = condition.getParamValue(0, ruleN);
            if (key != null && set.add(key)) {
                list.add(key);
            }
        }

        return new EnumDomain<>(list.toArray());
    }

}

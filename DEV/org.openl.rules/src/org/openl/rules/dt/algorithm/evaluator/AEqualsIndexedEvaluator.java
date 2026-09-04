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

    public AEqualsIndexedEvaluator(ConditionCasts conditionCasts) {
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
        var value = conditionCasts.castToConditionType(condition.getEvaluator().invoke(target, dtparams, env));
        return new EqualsSelector(condition, value, target, dtparams, env);
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        var len = condition.getNumberOfRules();
        var list = new ArrayList<Object>(len);
        var set = new HashSet<Object>(len);

        for (var ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }
            var key = condition.getParamValue(0, ruleN);
            if (key == null || !set.add(key)) {
                continue;
            }
            list.add(key);
        }

        return new EnumDomain<>(list.toArray());
    }

}

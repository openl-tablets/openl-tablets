package org.openl.rules.dt.algorithm.evaluator;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

import java.util.ArrayList;
import java.util.HashSet;

abstract class AEqualsIndexedEvaluator extends AConditionEvaluator {

    IOpenCast openCast;

    public AEqualsIndexedEvaluator(IOpenCast openCast) {
        this.openCast = openCast;
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        IOpenSourceCodeModule condSource = condition.getSourceCodeModule();
        return new StringSourceCodeModule("(" + condSource.getCode() + ") == " + condition.getParams()[0].getName(),
            condSource.getUri());
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        return new EqualsSelector(condition, value, target, dtparams, env);
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        int len = condition.getNumberOfRules();
        ArrayList<Object> list = new ArrayList<>(len);
        HashSet<Object> set = new HashSet<>(len);

        for (int ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }
            Object key = condition.getParamValue(0, ruleN);
            if (key == null || !set.add(key)) {
                continue;
            }
            list.add(key);
        }

        return new EnumDomain<>(list.toArray());
    }

}

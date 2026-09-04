package org.openl.rules.dt.algorithm.evaluator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.vm.IRuntimeEnv;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DefaultConditionSelector implements IIntSelector {

    private final ICondition condition;
    private final Object target;
    private final Object[] params;
    private final IRuntimeEnv env;

    @Override
    public boolean select(int rule) {
        return condition.calculateCondition(rule, target, params, env).getBooleanValue();
    }

}

package org.openl.rules.dt.algorithm.evaluator;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

/**
 * Selects the rules whose cell of several values holds the value, and the rules that leave the cell blank.
 *
 * <p>A formula cell is calculated for the call, so its values are known only then.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ContainsInArraySelector implements IIntSelector {

    private final ICondition condition;
    private final Object value;
    private final Object target;
    private final Object[] params;
    private final IRuntimeEnv env;

    @Override
    public boolean select(int ruleN) {
        if (condition.isEmpty(ruleN)) {
            return true;
        }
        var values = new Object[condition.getNumberOfParams()];
        condition.loadValues(values, 0, ruleN, target, params, env);
        return ArrayTool.contains(values[0], value);
    }
}

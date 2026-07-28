package org.openl.studio.projects.service.trace;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;

/**
 * Created by ymolchan on 08.02.2016.
 */
class IntSelectorTracer implements IIntSelector {
    private final IIntSelector selector;
    private final ICondition condition;
    private final DebugHook hook;

    IntSelectorTracer(IIntSelector selector, ICondition condition, DebugHook hook) {
        this.selector = selector;
        this.condition = condition;
        this.hook = hook;
    }

    @Override
    public boolean select(int rule) {
        var successful = selector.select(rule);
        hook.onPut(this, "condition", new Object[]{condition, rule, successful});
        return successful;
    }
}

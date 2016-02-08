package org.openl.rules.webstudio.web.trace;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.vm.Tracer;

/**
 * Created by ymolchan on 08.02.2016.
 */
class IntSelectorTracer implements IIntSelector {
    private final IIntSelector selector;
    private final ICondition condition;

    IntSelectorTracer(IIntSelector selector, ICondition condition) {
        this.selector = selector;
        this.condition = condition;
    }

    @Override
    public boolean select(int rule) {
        boolean successful = selector.select(rule);
        Tracer.put(this, "condition", condition, rule, successful);
        return successful;
    }
}

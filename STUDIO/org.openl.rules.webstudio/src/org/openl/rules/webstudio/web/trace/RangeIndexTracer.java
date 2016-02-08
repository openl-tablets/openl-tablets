package org.openl.rules.webstudio.web.trace;

import java.util.Comparator;

import org.openl.rules.dt.DecisionTableIndexedRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.vm.Tracer;

/**
 * Created by ymolchan on 08.02.2016.
 */
class RangeIndexTracer implements Comparator<Object> {
    final ICondition condition;

    RangeIndexTracer(RangeIndex index, ICondition condition) {
        this.condition = condition;
        index.comparator = this;
    }

    @Override
    public int compare(Object o1, Object o2) {
        DecisionTableIndexedRuleNode rule;
        Object value;
        if (o1 instanceof DecisionTableIndexedRuleNode) {
            rule = (DecisionTableIndexedRuleNode) o1;
            value = o2;
        } else {
            rule = (DecisionTableIndexedRuleNode) o2;
            value = o1;
        }
        Tracer.put(this, "index", condition, rule, false);
        return rule.compareTo(value);
    }
}

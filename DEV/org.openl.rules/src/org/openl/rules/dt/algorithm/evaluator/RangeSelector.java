package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.vm.IRuntimeEnv;

public class RangeSelector implements IIntSelector {

    private ICondition condition;
    private Object value;

    private Object target;
    private Object[] params;
    private IRuntimeEnv env;
    private IRangeAdaptor<Object, ? extends Comparable<Object>> adaptor;

    RangeSelector(ICondition condition,
            Object value,
            Object target,
            Object[] params,
            IRangeAdaptor<Object, ? extends Comparable<Object>> adaptor,
            IRuntimeEnv env) {
        this.condition = condition;
        this.adaptor = adaptor;

        // As income value is of Number type, it should be adapted to the value type
        // from range adaptor for further comparison.
        //
        if (adaptor != null) {
            this.value = this.adaptor.adaptValueType(value);
        } else {
            this.value = value;
        }
        this.params = params;
        this.env = env;
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    public boolean select(int ruleN) {

        if (condition.isEmpty(ruleN)) {
            return true;
        }

        Object[] realParams = new Object[condition.getNumberOfParams()];

        condition.loadValues(realParams, 0, ruleN, target, this.params, env);

        Comparable<Object> vFrom = null;
        Comparable<Object> vTo = null;

        if (adaptor == null) {
            vFrom = (Comparable<Object>) realParams[0];
            vTo = (Comparable<Object>) realParams[1];
        } else {
            vFrom = adaptor.getMin(realParams[0]);
            if (realParams.length == 2) {
                vTo = adaptor.getMax(realParams[1]);
            } else {
                vTo = adaptor.getMax(realParams[0]);
            }
        }

        if (value == null) {
            return vFrom == null && vTo == null;
        }

        return (vFrom == null || vFrom.compareTo(value) <= 0) && (vTo == null || ((Comparable<Object>) value)
            .compareTo(vTo) < 0);
    }

}

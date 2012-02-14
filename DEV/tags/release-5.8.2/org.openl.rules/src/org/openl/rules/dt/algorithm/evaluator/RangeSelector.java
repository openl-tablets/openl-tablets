package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.vm.IRuntimeEnv;

public class RangeSelector implements IIntSelector {

    private ICondition condition;
    private Number value;
    
    private Object target;
    private Object[] params;
    private IRuntimeEnv env;
    private IRangeAdaptor<Object, Object> adaptor;

    public RangeSelector(ICondition condition, Number value, Object target, Object[] params, IRangeAdaptor<Object, Object> adaptor,  IRuntimeEnv env) {
        this.condition = condition;
        this.adaptor = adaptor;
        
        // As income value is of Number type, it should be adapted to the value type 
        // from range adaptor for further comparasion.
        //
        if (adaptor != null) {
            this.value = this.adaptor.adaptValueType(value);
        }else{
            this.value = value;
        }
        this.params = params;        
        this.env = env;
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    public boolean select(int rule) {
        
        Object[][] params = condition.getParamValues();
        Object[] ruleParams = params[rule];

        if (ruleParams == null) {
            return true;
        }

        Object[] realParams = new Object[ruleParams.length];

        RuleRowHelper.loadParams(realParams, 0, ruleParams, target, this.params, env);

        Comparable<Object> vFrom = null;
        Comparable<Object> vTo = null;

        if (adaptor == null) {
            vFrom = (Comparable<Object>) realParams[0];
            vTo = (Comparable<Object>) realParams[1];
        } else {
            vFrom = adaptor.getMin(realParams[0]);
            vTo = adaptor.getMax(realParams[0]);
        }
        
        return vFrom.compareTo(value) <= 0 && ((Comparable<Object>) value).compareTo(vTo) < 0;
    }

}

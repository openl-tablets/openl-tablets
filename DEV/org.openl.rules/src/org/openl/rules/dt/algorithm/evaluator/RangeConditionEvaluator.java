package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

public class RangeConditionEvaluator extends DefaultConditionEvaluator {

    private IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor;
    private int nparams; // 1 or 2

    public RangeConditionEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> adaptor, int nparams) {
        this.rangeAdaptor = adaptor;
        this.nparams = nparams;
    }

    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        if (rangeAdaptor != null && rangeAdaptor.useOriginalSource())
            return condition.getSourceCodeModule();

        IParameterDeclaration[] cparams = condition.getParams();

        IOpenSourceCodeModule conditionSource = condition.getSourceCodeModule();

        String code = cparams.length == 2 ? String.format("%1$s<=(%2$s) && (%2$s) < %3$s",
                cparams[0].getName(),
                conditionSource.getCode(),
                cparams[1].getName())
                : String.format("%1$s.contains(%2$s)",
                cparams[0].getName(),
                conditionSource.getCode());

        return new StringSourceCodeModule(code, conditionSource.getUri());
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);

        return new RangeSelector(condition, value, target, dtparams, rangeAdaptor, env);
    }

}

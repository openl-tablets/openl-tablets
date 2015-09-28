package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dtx.IBaseAction;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;

public interface IAction extends IBaseAction, IDecisionRow {


    boolean isReturnAction();

    void prepareAction(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentOpenClass,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow, IOpenClass ruleExecutionType) throws Exception;


}

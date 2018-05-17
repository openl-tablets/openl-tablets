package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;

public interface IAction extends IBaseAction, IDecisionRow {

    void prepareAction(IOpenMethodHeader header,
            IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentOpenClass,
            IBindingContext bindingContext,
            RuleRow ruleRow,
            IOpenClass ruleExecutionType,
            TableSyntaxNode tableSyntaxNode) throws Exception;


}

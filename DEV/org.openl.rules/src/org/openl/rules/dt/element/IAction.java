package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;

public interface IAction extends IBaseAction, IDecisionRow {

    // Every action type prepares with the table, its header, the rule row and the binding state.
    @SuppressWarnings("java:S107")
    void prepareAction(DecisionTable decisionTable,
                       IOpenMethodHeader header,
                       IMethodSignature signature,
                       OpenL openl,
                       IBindingContext bindingContext,
                       RuleRow ruleRow,
                       IOpenClass ruleExecutionType,
                       TableSyntaxNode tableSyntaxNode) throws SyntaxNodeException;
}

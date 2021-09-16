package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;

/**
 * @author snshor
 */
public class DecisionTableNodeBinder extends AExecutableNodeBinder {

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {
        DecisionTableBoundNode dtBoundNode = (DecisionTableBoundNode) super.preBind(tableSyntaxNode,
            openl,
            bindingContext,
            module);
        dtBoundNode.preBind(bindingContext);
        return dtBoundNode;
    }

    @Override
    protected IMemberBoundNode createNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            OpenMethodHeader header,
            XlsModuleOpenClass module) {

        return new DecisionTableBoundNode(tableSyntaxNode, openl, header, module);
    }
}

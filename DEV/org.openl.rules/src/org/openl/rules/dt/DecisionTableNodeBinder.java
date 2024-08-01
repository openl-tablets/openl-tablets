package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.types.impl.OpenMethodHeader;

public class DecisionTableNodeBinder extends AExecutableNodeBinder<DecisionTableBoundNode> {

    @Override
    public DecisionTableBoundNode preBind(TableSyntaxNode tableSyntaxNode,
                                    OpenL openl,
                                    RulesModuleBindingContext bindingContext,
                                    XlsModuleOpenClass module) throws Exception {
        DecisionTableBoundNode dtBoundNode = super.preBind(tableSyntaxNode,
                openl,
                bindingContext,
                module);
        dtBoundNode.preBind(bindingContext);
        return dtBoundNode;
    }

    @Override
    protected MetaInfoReader createMetaInfoReader(DecisionTableBoundNode node) {
        return new DecisionTableMetaInfoReader(node);
    }

    @Override
    protected DecisionTableBoundNode createNode(TableSyntaxNode tableSyntaxNode,
                                          OpenL openl,
                                          OpenMethodHeader header,
                                          XlsModuleOpenClass module) {

        return new DecisionTableBoundNode(tableSyntaxNode, openl, header, module);
    }
}

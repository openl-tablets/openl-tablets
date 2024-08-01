package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.types.impl.OpenMethodHeader;

public class DecisionTableNodeBinder extends AExecutableNodeBinder<DecisionTableBoundNode> {

    @Override
    protected MetaInfoReader createMetaInfoReader(DecisionTableBoundNode node) {
        return new DecisionTableMetaInfoReader(node);
    }

    @Override
    protected DecisionTableBoundNode createNode(TableSyntaxNode tableSyntaxNode,
                                                OpenL openl,
                                                OpenMethodHeader header,
                                                XlsModuleOpenClass module,
                                                IBindingContext context) {

        return new DecisionTableBoundNode(tableSyntaxNode, openl, header, module, context);
    }
}

package org.openl.rules.method.table;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;

public class MethodTableNodeBinder extends AExecutableNodeBinder<MethodTableBoundNode> {

    @Override
    protected MethodTableBoundNode createNode(TableSyntaxNode tableSyntaxNode,
                                              OpenL openl,
                                              OpenMethodHeader header,
                                              XlsModuleOpenClass module) {

        return new MethodTableBoundNode(tableSyntaxNode, openl, header, module);
    }
}

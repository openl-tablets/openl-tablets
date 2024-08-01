package org.openl.rules.tbasic;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;

public class AlgorithmNodeBinder extends AExecutableNodeBinder<AlgorithmBoundNode> {

    @Override
    protected AlgorithmBoundNode createNode(TableSyntaxNode tableSyntaxNode,
                                          OpenL openl,
                                          OpenMethodHeader header,
                                          XlsModuleOpenClass module) {

        return new AlgorithmBoundNode(tableSyntaxNode, openl, header, module);
    }
}

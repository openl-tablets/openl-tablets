package org.openl.rules.tbasic;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.AlgorithmMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.types.impl.OpenMethodHeader;

public class AlgorithmNodeBinder extends AExecutableNodeBinder<AlgorithmBoundNode> {

    @Override
    protected MetaInfoReader createMetaInfoReader(AlgorithmBoundNode node) {
        return new AlgorithmMetaInfoReader(node);
    }

    @Override
    protected AlgorithmBoundNode createNode(TableSyntaxNode tableSyntaxNode,
                                            OpenL openl,
                                            OpenMethodHeader header,
                                            XlsModuleOpenClass module,
                                            IBindingContext context) {

        return new AlgorithmBoundNode(tableSyntaxNode, openl, header, module);
    }
}

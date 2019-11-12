package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;

public class SpreadsheetNodeBinder extends AExecutableNodeBinder {

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {
        SpreadsheetBoundNode sprBoundNode = (SpreadsheetBoundNode) super.preBind(tableSyntaxNode,
            openl,
            bindingContext,
            module);
        sprBoundNode.preBind(bindingContext);
        return sprBoundNode;
    }

    @Override
    protected IMemberBoundNode createNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            OpenMethodHeader header,
            XlsModuleOpenClass module) {

        return new SpreadsheetBoundNode(tableSyntaxNode, openl, header, module);
    }
}

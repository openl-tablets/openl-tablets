package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.types.impl.OpenMethodHeader;

public class SpreadsheetNodeBinder extends AExecutableNodeBinder<SpreadsheetBoundNode> {

    @Override
    public SpreadsheetBoundNode preBind(TableSyntaxNode tableSyntaxNode,
                                    OpenL openl,
                                    RulesModuleBindingContext bindingContext,
                                    XlsModuleOpenClass module) throws Exception {
        SpreadsheetBoundNode sprBoundNode = super.preBind(tableSyntaxNode,
                openl,
                bindingContext,
                module);
        sprBoundNode.preBind(bindingContext);
        return sprBoundNode;
    }

    @Override
    protected MetaInfoReader createMetaInfoReader(SpreadsheetBoundNode node) {
        return new SpreadsheetMetaInfoReader(node);
    }

    @Override
    protected SpreadsheetBoundNode createNode(TableSyntaxNode tableSyntaxNode,
                                          OpenL openl,
                                          OpenMethodHeader header,
                                          XlsModuleOpenClass module) {

        return new SpreadsheetBoundNode(tableSyntaxNode, openl, header, module);
    }
}

package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AExecutableNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.impl.OpenMethodHeader;

public class DecisionTableNodeBinder extends AExecutableNodeBinder<DecisionTableBoundNode> {

    @Override
    protected MetaInfoReader createMetaInfoReader(DecisionTableBoundNode node) {
        return new DecisionTableMetaInfoReader(node);
    }

    /**
     * Creates the source of the method header of the table.
     *
     * <p>The {@code Collect} keyword in a table that does not support it, such as a {@code Rules} table, is reported
     * here. Otherwise the keyword would be read as the return type and the rest of the header as a syntax error.
     */
    @Override
    public IOpenSourceCodeModule createHeaderSource(TableSyntaxNode tableSyntaxNode,
                                                    IBindingContext bindingContext) throws SyntaxNodeException {
        var source = super.createHeaderSource(tableSyntaxNode, bindingContext);
        var words = source.getCode().split("\\(", 2)[0].trim().split("\\s+");
        if (words.length > 2 && IXlsTableNames.COLLECT.equals(words[0])) {
            throw SyntaxNodeExceptionUtils.createError(
                    "The 'Collect' keyword is supported only in SmartRules, SimpleRules, SmartLookup and SimpleLookup "
                            + "tables. Use a CRET column in a Rules table.",
                    source);
        }
        return source;
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

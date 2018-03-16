package org.openl.rules.constants;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * Binder for constants table.
 * 
 * @author Marat Kamalov
 * 
 */
public class ConstantsTableBinder extends AXlsTableBinder {

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            IBindingContext cxt,
            XlsModuleOpenClass module) throws Exception {

        assert cxt instanceof RulesModuleBindingContext;

        ILogicalTable table = tsn.getTable();
        IOpenSourceCodeModule tableSource = tsn.getHeader().getModule();

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(tableSource, " \n\r");
        if (parsedHeader.length != 1) {
            String message = "Constants table format: Constants";
            throw SyntaxNodeExceptionUtils.createError(message, null, null, tableSource);
        }

        return new ConstantsTableBoundNode(tsn, module, table, openl);
    }

}

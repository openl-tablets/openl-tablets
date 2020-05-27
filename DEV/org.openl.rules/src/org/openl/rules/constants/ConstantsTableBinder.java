package org.openl.rules.constants;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.TableNameChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * Binder for constants table.
 *
 * @author Marat Kamalov
 *
 */
public class ConstantsTableBinder extends AXlsTableBinder {

    private static final byte CONSTANTS_TABLE_NAME_INDEX = 1;

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {

        ILogicalTable table = tsn.getTable();

        IOpenSourceCodeModule source = new GridCellSourceCodeModule(table.getSource(), bindingContext);

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(source, " \n\r");
        // table name can be not presented
        if (parsedHeader.length > 1) {
            String constantsTableName = parsedHeader[CONSTANTS_TABLE_NAME_INDEX].getIdentifier();
            if (TableNameChecker.isInvalidJavaIdentifier(constantsTableName)) {
                String message = "Constants table " + constantsTableName + TableNameChecker.NAME_ERROR_MESSAGE;
                bindingContext
                    .addMessage(OpenLMessagesUtils.newWarnMessage(message, parsedHeader[CONSTANTS_TABLE_NAME_INDEX]));
            }
        }
        return new ConstantsTableBoundNode(tsn, module, table, openl);
    }

}

package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * Binder for returns table.
 * 
 * @author Marat Kamalov
 * 
 */
public class ReturnsTableBinder extends DataNodeBinder {

    private static final String DEFAULT_TABLE_NAME_PREFIX = "Returns: ";

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            IBindingContext cxt,
            XlsModuleOpenClass module) throws Exception {

        assert cxt instanceof RulesModuleBindingContext;

        ReturnsTableBoundNode returnsNode = (ReturnsTableBoundNode) makeNode(tsn, module, openl, cxt);

        String tableName = parseHeader(tsn);
        if (tableName == null) {
            tableName = DEFAULT_TABLE_NAME_PREFIX + tsn.getUri();
        }

        returnsNode.setTableName(tableName);

        return returnsNode;
    }

    /**
     * Parses table header. Consider that second token is the name of the table. <br>
     * <b>e.g.: Properties [tableName].</b>
     * 
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @return table name if exists.
     */
    private String parseHeader(TableSyntaxNode tsn) throws Exception {
        IOpenSourceCodeModule src = tsn.getHeader().getModule();

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

        if (parsedHeader.length > 1) {
            return parsedHeader[1].getIdentifier();
        }

        return null;
    }

    protected ATableBoundNode makeNode(TableSyntaxNode tsn,
            XlsModuleOpenClass module,
            OpenL openl,
            IBindingContext bindingContext) {
        ReturnsTableBoundNode boundNode = new ReturnsTableBoundNode(tsn, openl);
        return boundNode;
    }

}

package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DtColumnsDefinitionMetaInfoReader;
import org.openl.rules.utils.TableNameChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

public abstract class ADtColumnsDefinitionTableBinder extends DataNodeBinder {

    String tableNamePrefix;

    public ADtColumnsDefinitionTableBinder(String tableNamePrefix) {
        if (tableNamePrefix == null) {
            throw new NullPointerException();
        }
        this.tableNamePrefix = tableNamePrefix;
    }

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            RulesModuleBindingContext cxt,
            XlsModuleOpenClass module) throws Exception {

        ADtColumnsDefinitionTableBoundNode aDtColumnsDefinitionTableBoundNode = makeNode(tsn, module, openl, cxt);

        IdentifierNode in = parseHeader(tsn);
        String tableName;
        if (in == null) {
            tableName = tableNamePrefix + tsn.getUri();
        } else {
            tableName = in.getIdentifier();
            if (TableNameChecker.isInvalidJavaIdentifier(tableName)) {
                String formattedPrefix = tableNamePrefix.substring(0, tableNamePrefix.length() - 2);
                String message = formattedPrefix + " table " + tableName + TableNameChecker.NAME_ERROR_MESSAGE;
                throw SyntaxNodeExceptionUtils.createError(message, null, in);
            }
        }

        aDtColumnsDefinitionTableBoundNode.setTableName(tableName);

        tsn.setMetaInfoReader(new DtColumnsDefinitionMetaInfoReader(aDtColumnsDefinitionTableBoundNode));

        return aDtColumnsDefinitionTableBoundNode;
    }

    /**
     * Parses table header. Consider that second token is the name of the table. <br>
     * <b>e.g.: Properties [tableName].</b>
     *
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @return identifier node with name if exists.
     */
    private IdentifierNode parseHeader(TableSyntaxNode tsn) throws Exception {
        IOpenSourceCodeModule src = tsn.getHeader().getModule();

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

        if (parsedHeader.length > 1) {
            return parsedHeader[1];
        }

        return null;
    }

    protected abstract ADtColumnsDefinitionTableBoundNode makeNode(TableSyntaxNode tsn,
            XlsModuleOpenClass module,
            OpenL openl,
            IBindingContext bindingContext);

}

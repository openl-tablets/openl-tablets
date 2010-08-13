/*
 * Created on Oct 3, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * @author snshor
 * 
 */
public class DatatypeNodeBinder extends AXlsTableBinder {

    public static final int PARENT_TYPE_INDEX = 3;
    public static final int TYPE_INDEX = 1;    

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module)
        throws Exception {

        ILogicalTable table = LogicalTableHelper.logicalTable(tsn.getTable());

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

        String errMsg;

        if (parsedHeader.length != 2 && parsedHeader.length != 4
                || (parsedHeader.length == 4 && !parsedHeader[2].getIdentifier().equals("extends"))) {
            errMsg = "Datatype table formats: Datatype <typename>] or [Datatype <typename> extends <parentTypeName>]";
            throw SyntaxNodeExceptionUtils.createError(errMsg, null, null, src);
        }

        String typeName = parsedHeader[TYPE_INDEX].getIdentifier();

        // String tableName =
        // parsedHeader.length > 2
        // ? parsedHeader[TABLE_NAME_INDEX].getIdentifier()
        // : null;

        if (cxt.findType(ISyntaxConstants.THIS_NAMESPACE, typeName) != null) {
            errMsg = "Duplicated Type Definition: " + typeName;

            throw SyntaxNodeExceptionUtils.createError(errMsg, null, parsedHeader[TYPE_INDEX]);
        }
        
        DatatypeOpenClass tableType = new DatatypeOpenClass(module.getSchema(), typeName);

        cxt.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

        // Add new type to internal types of module.
        //
        module.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

        if (parsedHeader.length == 4) {
            return new DatatypeTableMethodBoundNode(tsn, tableType, table, openl, parsedHeader[PARENT_TYPE_INDEX].getIdentifier());
        } else {
            return new DatatypeTableMethodBoundNode(tsn, tableType, table, openl);
        }
    }
    
}

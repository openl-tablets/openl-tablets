/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.datatype.binding;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;

/**
 * @author snshor
 *
 */
public class DatatypeNodeBinder extends AXlsTableBinder implements IXlsTableNames {

    static final int TYPE_INDEX = 1, TABLE_NAME_INDEX = 2;

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module)
            throws Exception {

        ILogicalTable table = LogicalTable.logicalTable(tsn.getTable());

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());

        IdentifierNode[] parsedHeader = TokenizerParser.tokenize(src, " \n\r");

        String errMsg;

        if (parsedHeader.length < 2) {
            errMsg = "Datatype table format: Datatype <typename> [tablename]";
            BoundError err = new BoundError(null, errMsg, null, src);
            throw err;
        }

        String typeName = parsedHeader[TYPE_INDEX].getIdentifier();

        // String tableName =
        // parsedHeader.length > 2
        // ? parsedHeader[TABLE_NAME_INDEX].getIdentifier()
        // : null;

        if (cxt.findType(ISyntaxConstants.THIS_NAMESPACE, typeName) != null) {
            errMsg = "Duplicated Type Definition: " + typeName;

            BoundError err = new BoundError(parsedHeader[TYPE_INDEX], errMsg, null);
            throw err;
        }

        ModuleOpenClass tableType = new ModuleOpenClass(module.getSchema(), typeName, cxt.getOpenL());

        cxt.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

        return new DatatypeTableMethodBoundNode(tsn, tableType, table, openl);
    }

}

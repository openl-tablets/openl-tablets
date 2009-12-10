/*
 * Created on Mar 8, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicObjectField;

/**
 * @author snshor
 *
 */
public class DatatypeTableMethodBoundNode implements IMemberBoundNode {

    TableSyntaxNode xlsDatatypeNode;

    OpenL openl;

    ModuleOpenClass module;

    ILogicalTable table;

    /**
     * @param xlsDataNode
     * @param class1
     * @param table
     */
    public DatatypeTableMethodBoundNode(TableSyntaxNode xlsDatatypeNode, ModuleOpenClass module, ILogicalTable table,
            OpenL openl) {

        this.xlsDatatypeNode = xlsDatatypeNode;
        this.module = module;
        this.table = table;
        this.openl = openl;
    }

    void addFields(IBindingContext cxt) throws Exception {

        ILogicalTable dataTable = findOrientation(cxt);

        int h = dataTable.getLogicalHeight();

        for (int i = 0; i < h; i++) {

            ILogicalTable row = dataTable.getLogicalRow(i);

            GridCellSourceCodeModule src = new GridCellSourceCodeModule(row.getGridTable());

            String srcCode = src.getCode().trim();

            if (srcCode.length() == 0 || srcCode.startsWith("//")) {
                continue;
            }

            IOpenClass fieldType = OpenLManager.makeType(openl, src, (IBindingContextDelegator) cxt);

            if (fieldType == null) {
                BoundError err = new BoundError(null, "Type " + src.getCode() + " not found", null, src);
                throw err;
            }

            if (row.getLogicalWidth() < 2) {
                BoundError err = new BoundError(null, "Bad table structure: must be {header} / {type | name}", null,
                        src);
                throw err;
            }

            src = new GridCellSourceCodeModule(row.getLogicalColumn(1).getGridTable());

            IdentifierNode[] idn = TokenizerParser.tokenize(src, " \r\n");
            if (idn.length != 1) {
                BoundError err = new BoundError(null, "Bad field name: \"" + src.getCode() + "\"", null, src);
                throw err;
            }

            // String fieldName =
            // row.getLogicalColumn(1).getGridTable().getStringValue(0,0);
            String fieldName = idn[0].getIdentifier();

            IOpenField field = new DynamicObjectField(module, fieldName, fieldType);

            try {
                module.addField(field);
                if (i == 0) {
                    module.setIndexField(field);
                }
            } catch (Throwable t) {
                BoundError err = new BoundError(null, "Can not add field " + fieldName + ": " + t.getMessage(), null,
                        src);
                throw err;
            }

        }
    }

    /**
     *
     */

    public void addTo(ModuleOpenClass openClass) {
        // TODO Auto-generated method stub

    }

    int countTypes(ILogicalTable dataPart, IBindingContext cxt) {
        int h = dataPart.getLogicalHeight();
        int cnt = 0;
        for (int i = 0; i < h; ++i) {

            try {
                IOpenClass type = findType(dataPart.getLogicalRow(i), cxt);
                cnt += type == null ? 0 : 1;
            } catch (Throwable t) {
            }
        }
        return cnt;
    }

    /**
     *
     */

    public void finalizeBind(IBindingContext cxt) throws Exception {

        addFields(cxt);

    }

    ILogicalTable findOrientation(IBindingContext cxt) {
        ILogicalTable dataPart = table.rows(1);

        // TODO optimize
        int cntVertical = countTypes(dataPart, cxt);

        int cntHorizontal = countTypes(dataPart.transpose(), cxt);

        return cntVertical < cntHorizontal ? dataPart.transpose() : dataPart;

    }

    IOpenClass findType(ILogicalTable table, IBindingContext cxt) {
        GridCellSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());

        IOpenClass fieldType = OpenLManager.makeType(openl, src, (IBindingContextDelegator) cxt);
        return fieldType;

    }

}

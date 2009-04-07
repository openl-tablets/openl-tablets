package org.openl.rules.structure;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.binding.MethodTableBoundNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.SubTextSourceCodeModule;
import org.openl.types.IOpenMethodHeader;

public class StructureTableNodeBinder extends AXlsTableBinder implements IXlsTableNames {
    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module) {

        IGridTable table = tsn.getTable().getGridTable();

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table);

        IOpenMethodHeader header = OpenlTool.getMethodHeader(new SubTextSourceCodeModule(src, tsn.getHeader()
                .getHeaderToken().getIdentifier().length()), openl, (IBindingContextDelegator) cxt);

        return new MethodTableBoundNode(tsn, openl, header, module);
    }

}

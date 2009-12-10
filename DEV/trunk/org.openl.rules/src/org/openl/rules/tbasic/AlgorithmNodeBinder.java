package org.openl.rules.tbasic;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.SubTextSourceCodeModule;
import org.openl.types.impl.OpenMethodHeader;

public class AlgorithmNodeBinder extends AXlsTableBinder {

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module)
            throws Exception {
        IGridTable table = tsn.getTable().getGridTable();

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table);

        int headerTokenLength = tsn.getHeader().getHeaderToken().getIdentifier().length();
        SubTextSourceCodeModule codeModule = new SubTextSourceCodeModule(src, headerTokenLength);
        OpenMethodHeader header = (OpenMethodHeader) OpenLManager.makeMethodHeader(openl, codeModule,
                (IBindingContextDelegator) cxt);

        header.setDeclaringClass(module);

        return new AlgorithmBoundNode(tsn, openl, header, module);
    }
}

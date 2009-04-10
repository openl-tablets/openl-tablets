package org.openl.rules.calc;


import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.SubTextSourceCodeModule;
import org.openl.types.impl.OpenMethodHeader;

public class SSheetNodeBinder extends AXlsTableBinder {

	@Override
	public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl,
			IBindingContext cxt, XlsModuleOpenClass module) throws Exception {
		IGridTable table = tsn.getTable().getGridTable();

		IOpenSourceCodeModule src = new GridCellSourceCodeModule(table);

		OpenMethodHeader header = (OpenMethodHeader) OpenlTool.getMethodHeader(
				new SubTextSourceCodeModule(src, tsn.getHeader()
						.getHeaderToken().getIdentifier().length()), openl,
				(IBindingContextDelegator) cxt);

		header.setDeclaringClass(module);

		return new SSheetBoundNode(tsn, openl, header, module);
	}

}

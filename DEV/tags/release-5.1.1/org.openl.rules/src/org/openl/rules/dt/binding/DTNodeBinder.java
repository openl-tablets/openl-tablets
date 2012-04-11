/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.binding;

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
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.SubTextSourceCodeModule;
import org.openl.types.impl.OpenMethodHeader;


/**
 * @author snshor
 *
 */
public class DTNodeBinder extends AXlsTableBinder implements IXlsTableNames
{

	public IMemberBoundNode preBind(
		TableSyntaxNode tsn,
		OpenL openl,
		IBindingContext cxt,
		XlsModuleOpenClass module)
	{


		IGridTable table = tsn.getTable().getGridTable();

		//		String methodHeaderSrc =
		//			table.getStringValue(0, 0).substring(dtNode.getHeader().length());

		IOpenSourceCodeModule src = new GridCellSourceCodeModule(table);

		//		IOpenSourceCodeModule src =
		//			new StringSourceCodeModule(
		//				methodHeaderSrc,
		//				syntaxNode.getModule().getUri(0)
		//					+ new GridLocation(table, 0, 0));

		//		IOpenMethodHeader header =
		//		  OpenlTool.getMethodHeader(
		//			new StringSourceCodeModule(
		//			  methodHeaderSrc,
		//			  syntaxNode.getModule().getUri(0)
		//				+ new GridLocation(table, 0, 0)),
		//			openl);

		OpenMethodHeader header = (OpenMethodHeader)
			OpenlTool.getMethodHeader(
				new SubTextSourceCodeModule(src, tsn.getHeader().getHeaderToken(). getIdentifier().length()),
				openl, (IBindingContextDelegator)cxt);
		
		header.setDeclaringClass(module);

		return new DecisionTableMethodBoundNode(tsn, openl, header, module);
	}

}

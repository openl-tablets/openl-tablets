package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.engine.OpenLManager;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.util.ArrayTool;

public class DatatypeHelper {

	public static IDomain<?> getTypeDomain(ILogicalTable table, IOpenClass type, OpenL openl, IBindingContext cxt) throws SyntaxNodeException {
		Object values = loadAliasDatatypeValues(table, type, openl, cxt);
		
		if (values != null) {
			return new EnumDomain(ArrayTool.toArray(values));
		}
		
		return new EnumDomain<Object>(new Object[]{}); 
	}
	
	public static Object loadAliasDatatypeValues(ILogicalTable table, IOpenClass type, OpenL openl, IBindingContext cxt) throws SyntaxNodeException {

		OpenlToolAdaptor openlAdaptor = new OpenlToolAdaptor(openl, cxt);
		
		return RuleRowHelper.loadParam(table, type, "Values", "", openlAdaptor, true);
	}
	
	public static boolean isAliasDatatype(ILogicalTable table, OpenL openl, IBindingContext cxt) {
		
		ILogicalTable dataPart = getNormalizedDataPartTable(table, openl, cxt);
		
		int height = dataPart.getLogicalHeight();
		int typesCount1 = countTypes(dataPart, openl, cxt);
		int typesCount2 = countTypes(dataPart.transpose(), openl, cxt);
		int width = dataPart.getLogicalWidth();
		
		if (typesCount1 == 0 
				&& typesCount2 == 0 
				&& (height == 0 // values are not provided 
						|| width == 1
						|| height == 1)) {
			return true;
		}
			
		return false;
	}

	public static ILogicalTable getNormalizedDataPartTable(ILogicalTable table, OpenL openl, IBindingContext cxt) {

		ILogicalTable dataPart = table.rows(1);

        //if datatype table has only one row
		if (dataPart.getLogicalHeight() == 1) {
            return dataPart;
        } else if (dataPart.getLogicalWidth() == 1) {
            return dataPart.transpose();
        }
		
		int verticalCount = countTypes(dataPart, openl, cxt);
		int horizontalCount = countTypes(dataPart.transpose(), openl, cxt);

		if (verticalCount < horizontalCount) {
			return dataPart.transpose();
		}
		
		return dataPart;
	}
	
	private static int countTypes(ILogicalTable table, OpenL openl, IBindingContext cxt) {
		
		int height = table.getLogicalHeight();
		int count = 0;

		for (int i = 0; i < height; ++i) {
			try {
				IOpenClass type = findType(table.getLogicalRow(i), openl, cxt);
				if (type != null) {
					count += 1;
				}
			} catch (Throwable t) {
				// Ignore exception.
			}
		}
		
		return count;
	}
	
	private static IOpenClass findType(ILogicalTable table, OpenL openl, IBindingContext cxt) {
	
		GridCellSourceCodeModule source = new GridCellSourceCodeModule(table.getGridTable());
		return OpenLManager.makeType(openl, source, (IBindingContextDelegator) cxt);
	}

}

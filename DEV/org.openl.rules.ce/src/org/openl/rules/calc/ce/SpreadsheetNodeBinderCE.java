package org.openl.rules.calc.ce;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.calc.SpreadsheetNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;

public class SpreadsheetNodeBinderCE extends SpreadsheetNodeBinder {

	@Override
	protected IMemberBoundNode createNode(TableSyntaxNode tableSyntaxNode,
			OpenL openl, OpenMethodHeader header, XlsModuleOpenClass module) {

		return new SpreadsheetBoundNodeCE(tableSyntaxNode, openl, header, module);
	}
	
	

}

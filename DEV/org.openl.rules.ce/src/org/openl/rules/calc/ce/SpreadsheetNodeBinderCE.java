package org.openl.rules.calc.ce;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.calc.SpreadsheetNodeBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.util.ce.conf.ServiceMTConfiguration;
import org.openl.util.ce.impl.ServiceMT;

public class SpreadsheetNodeBinderCE extends SpreadsheetNodeBinder {

	@Override
	protected IMemberBoundNode createNode(TableSyntaxNode tableSyntaxNode,
			OpenL openl, OpenMethodHeader header, XlsModuleOpenClass module) {
		
		
		String methodName = header.getName();
		
		ServiceMTConfiguration config = ServiceMT.getService().getConfig();

		if (config.isExecuteComponentUsingMT(methodName))
			return new SpreadsheetBoundNodeCE(tableSyntaxNode, openl, header, module);
		else
			return new SpreadsheetBoundNode(tableSyntaxNode, openl, header, module);
		
		
		
	}
	
	

}

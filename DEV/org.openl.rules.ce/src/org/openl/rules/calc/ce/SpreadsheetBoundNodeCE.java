package org.openl.rules.calc.ce;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;

public class SpreadsheetBoundNodeCE   extends SpreadsheetBoundNode {

	public SpreadsheetBoundNodeCE(TableSyntaxNode tableSyntaxNode, OpenL openl,
			IOpenMethodHeader header, ModuleOpenClass module) {
		super(tableSyntaxNode, openl, header, module);
	}

	@Override
	protected Spreadsheet createSpreadsheet() {
		return new SpreadsheetCE(getHeader(), this);
	}

	

}

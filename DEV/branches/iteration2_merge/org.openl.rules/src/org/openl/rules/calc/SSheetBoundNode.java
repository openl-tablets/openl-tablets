package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

public class SSheetBoundNode   extends AMethodBasedNode
		implements IMemberBoundNode
{


	 public SSheetBoundNode(TableSyntaxNode tsn, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module)
	  {
	    super(tsn, openl, header, module);
	  }

	

	public void finalizeBind(IBindingContext cxt) throws Exception {
		// TODO Auto-generated method stub

	}



	@Override
	protected IOpenMethod createMethodShell() {
		return Spreadsheet.createSpreadsheet(header);
	}

}

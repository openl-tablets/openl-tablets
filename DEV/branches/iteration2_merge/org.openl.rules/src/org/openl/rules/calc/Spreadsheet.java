package org.openl.rules.calc;

import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class Spreadsheet extends AMethod 
{
	
	public Spreadsheet(IOpenMethodHeader header) {
		super(header);
	}

	static public Spreadsheet createSpreadsheet(IOpenMethodHeader header)
	{
		return new Spreadsheet(header);
	}

	public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	SCell[][] cells;
	
//	SHeader[] horizontalHeaders;
//	SHeader[] verticalheaders;
	
	
	
}

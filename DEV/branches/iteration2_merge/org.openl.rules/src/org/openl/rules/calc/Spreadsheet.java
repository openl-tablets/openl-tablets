package org.openl.rules.calc;

import org.openl.meta.StringValue;
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



	public void addRowName(int row, StringValue sv, int cxtLevel) 
	{
		
		
		// TODO Auto-generated method stub
		
	}
	
//	SHeader[] horizontalHeaders;
//	SHeader[] verticalheaders;
	
	
	
}

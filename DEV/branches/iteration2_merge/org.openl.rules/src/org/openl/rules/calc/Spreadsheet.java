package org.openl.rules.calc;

import org.openl.types.IDynamicObject;
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

		return new SpreadsheetResult(this, (IDynamicObject)target, params, env);
	}
	
	
	
	SCell[][] cells;

	
	
	SpreadsheetType spreadsheetType; 
	


	public SCell[][] getCells() {
		return cells;
	}

	public void setCells(SCell[][] cells) {
		this.cells = cells;
	}

	public SpreadsheetType getSpreadsheetType() {
		return spreadsheetType;
	}

	public void setSpreadsheetType(SpreadsheetType spreadsheetType) {
		this.spreadsheetType = spreadsheetType;
	}
	
//	SHeader[] horizontalHeaders;
//	SHeader[] verticalheaders;
	
	
	
}

package org.openl.rules.calc;

public class SCellRow extends SCellArray
{
	int row;
	Spreadsheet spreadsheet;
	
	public SCell get(int i)
	{
		return spreadsheet.getCells()[row][i];
	}
}

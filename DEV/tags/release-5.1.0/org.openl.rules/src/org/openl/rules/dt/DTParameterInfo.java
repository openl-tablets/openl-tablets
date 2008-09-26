package org.openl.rules.dt;

import org.openl.types.IParameterDeclaration;

public class DTParameterInfo
{
	
	IDecisionRow row;
	int index;
	
	public DTParameterInfo(int index, IDecisionRow row)
	{
		this.index = index;
		this.row = row;
	}

	public IParameterDeclaration getParameterDeclaration()
	{
		return row.getParams()[index];
	}
	
	public String getPresentation()
	{
		return row.getParamPresentation()[index];
	}

	public int getIndex()
	{
		return index;
	}

	public IDecisionRow getRow()
	{
		return row;
	}

	public Object getValue(int i)
	{
		Object[][] pvalues = row.getParamValues();
		if (pvalues == null)
			return null;
		
		Object[]  rowValue = row.getParamValues()[i];
		
		return rowValue == null ? null : rowValue[index];
	}
	
}

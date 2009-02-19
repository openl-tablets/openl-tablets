package org.openl.rules.lookup;

import org.openl.types.IOpenClass;

public class LookupKeyStructure
{
	String   rowOrColumn; //row | column
	IOpenClass keyType;
	String name;
	String methodBody;
	public IOpenClass getKeyType()
	{
		return keyType;
	}
	public void setKeyType(IOpenClass keyType)
	{
		this.keyType = keyType;
	}
	public String getMethodBody()
	{
		return methodBody;
	}
	public void setMethodBody(String methodBody)
	{
		this.methodBody = methodBody;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getRowOrColumn()
	{
		return rowOrColumn;
	}
	public void setRowOrColumn(String rowOrColumn)
	{
		this.rowOrColumn = rowOrColumn;
	}
	
}

package org.openl.rules.lang.xls.types;

import org.openl.types.IOpenClass;

public class CellMetaInfo
{

	public static enum Type {TABLE_HEADER, DT_CA_HEADER, DT_CA_CODE, DT_CA_DISPLAY,DT_DATA_CELL};
	
	
	Type type;
	IOpenClass dataType;
	String paramName;
	
	public CellMetaInfo(Type type, String paramName, IOpenClass dataType)
	{
		this.type = type;
		this.dataType = dataType;
		this.paramName =paramName;
	}

	public Type getType()
	{
		return type;
	}
	
	public IOpenClass getDataType()
	{
		return dataType;
	}

	public String getParamName()
	{
		return paramName;
	}
	
}

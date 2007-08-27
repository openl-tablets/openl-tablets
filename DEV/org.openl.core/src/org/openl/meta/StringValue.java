package org.openl.meta;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.impl.StringSourceCodeModule;

public class StringValue implements IMetaHolder
{
	ValueMetaInfo metaInfo;
	String value;

	public IMetaInfo getMetaInfo()
	{
		return metaInfo;
	}

	public void setMetaInfo(IMetaInfo metaInfo)
	{
		this.metaInfo = (ValueMetaInfo)metaInfo;
	}

	public StringValue(String value)
	{
		this.value = value;
	}
	
	
	public StringValue(String value, String shortName, String fullName, String sourceUrl)
	{
		this.value = value;
		metaInfo = new ValueMetaInfo(shortName, fullName, sourceUrl);
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	
	public IOpenSourceCodeModule asSourceCodeModule()
	{
		return new StringSourceCodeModule(value, getMetaInfo().getSourceUrl());
	}

	public String toString()
	{
		return value;
	}
	
	

	
}

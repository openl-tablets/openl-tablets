package org.openl.rules.lang.xls.binding;

import org.openl.meta.StringValue;
import org.openl.rules.table.ILogicalTable;

public class TableProperties
{
	ILogicalTable table;
	
	Property[] properties;
	
	
	
	public TableProperties(ILogicalTable table, Property[] properties)
	{
		this.table = table;
		this.properties = properties;
	}
	
	
	public Property getProperty(String key)
	{
		for (int i = 0; i < properties.length; i++)
		{
			Property p = properties[i];
			
			if (p.key.getValue().equals(key))
				return p;
		}
		return null;
	}
	
	public String getPropertyValue(String key)
	{
		Property p = getProperty(key);
		return p == null ? null : p.getValue().getValue();
	}
	
	static public class Property
	{
		StringValue key;
		StringValue value;
		
		public Property(StringValue key, StringValue value)
		{
			this.key = key;
			this.value = value;
		}
		
		public StringValue getKey()
		{
			return key;
		}
		
		public StringValue getValue()
		{
			return value;
		}
	}

	public ILogicalTable getTable()
	{
		return table;
	}


	public void setTable(ILogicalTable table)
	{
		this.table = table;
	}


	public Property[] getProperties()
	{
		return this.properties;
	}


	public void setProperties(Property[] properties)
	{
		this.properties = properties;
	}
}

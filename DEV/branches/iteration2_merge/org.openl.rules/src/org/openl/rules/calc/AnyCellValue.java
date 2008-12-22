package org.openl.rules.calc;

import org.openl.meta.DoubleValue;
import org.openl.meta.StringValue;

public class AnyCellValue 
{
	
	static public DoubleValue autocast(AnyCellValue x, DoubleValue y)
	{
		return x.getDoubleValue();
	}

	private DoubleValue doubleValue;
	private StringValue stringValue;

	public DoubleValue getDoubleValue() {
		return doubleValue;
	}
	
	static public StringValue autocast(AnyCellValue x, StringValue y)
	{
		return x.getStringValue();
	}

	private StringValue getStringValue() {
		return stringValue;
	}
	

}

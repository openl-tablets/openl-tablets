package org.openl.rules.calc;

import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.StringValue;
import org.openl.rules.data.String2DataConvertorFactory;

public class AnyCellValue implements IMetaHolder
{
	
	
	public AnyCellValue(String src)
	{
		try
		{
			double dx = (Double)new String2DataConvertorFactory.String2DoubleConvertor().parse(src, null, null);
			doubleValue = new DoubleValue(dx);
			return;
		}
		catch(Throwable t)
		{
			
		}
		
		
		stringValue = new StringValue(src);
	}
	
	public AnyCellValue(DoubleValue x) 
	{
		this.doubleValue = x;
	}

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

	public IMetaInfo getMetaInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMetaInfo(IMetaInfo info) {
		if (doubleValue != null)
			doubleValue.setMetaInfo(info);
		else if (stringValue != null)
		{
			stringValue.setMetaInfo(info);
		}	
		
		
	}

	static public AnyCellValue autocast(DoubleValue x, AnyCellValue y)
	{
		return new AnyCellValue(x);
	}
	
	
	@Override
	public String toString() {
		return doubleValue != null ? doubleValue.toString() : String.valueOf(stringValue);
	}
	
	public DoubleValue multiply(AnyCellValue cv)
	{
		return DoubleValue. multiply(getDoubleValue(),  cv.getDoubleValue());
	}
	
	
	public Object getValue()
	{
		return stringValue == null ? doubleValue : stringValue;
	}

}

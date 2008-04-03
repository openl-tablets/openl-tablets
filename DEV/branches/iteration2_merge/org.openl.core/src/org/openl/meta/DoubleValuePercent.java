package org.openl.meta;

import java.text.DecimalFormat;

public class DoubleValuePercent extends DoubleValue
{
  /**
	 * 
	 */
	private static final long serialVersionUID = 6543033363886217906L;


	public DoubleValuePercent()
  {}
  public DoubleValuePercent(double d)
  {super(d);}

	
	public DoubleValuePercent(String valueStr)
   {
  	 super(valueStr);
   }


	public String printValue()
	{
		return new DecimalFormat(PERCENT_FORMAT).format(getValue());
	}

	
	static public final String PERCENT_FORMAT = "#.####%";
	
//	public DoubleValue add(DoubleValue dv)
//	{
//		DoubleValue res = super.add(dv);
//		res.format = PERCENT_FORMAT;
//		return res;
//	}

}

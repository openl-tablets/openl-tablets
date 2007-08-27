/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.ui;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * @author snshor
 *
 */
public interface ITextFormatter
{
	public String format(Object obj);
	
	static public class NumberTextFormatter implements ITextFormatter
	{
		DecimalFormat format;
		String fmtStr;
		
		public NumberTextFormatter(String fmt)
		{
			this.format = new DecimalFormat(fmt);
		}

		public NumberTextFormatter(DecimalFormat fmt, String fmtStr)
		{
			this.format = fmt;
			this.fmtStr = fmtStr;
		}
		
		public String format(Object obj)
		{
			return format.format(obj);
		}
	}
	

	static public class DateTextFormatter implements ITextFormatter
	{
		SimpleDateFormat format;
		
		public DateTextFormatter(String fmt)
		{
			this.format = new SimpleDateFormat(fmt);
		}

		public String format(Object obj)
		{
			return format.format(obj);
		}
	}
	
	
	
	static public class ConstTextFormatter implements ITextFormatter 
	{
		String format;

		public ConstTextFormatter(String format)
		{
			this.format = format;
		}

		public String format(Object obj)
		{
			return format;
		}
	}
}

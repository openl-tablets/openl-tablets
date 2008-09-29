/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004 	
 */
package org.openl.rules.helpers;

/**
 * @author snshor
 */

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class Util
{
	static public void out(String output)
	{
		System.out.println(output);	
	}
	
	static public void error(String msg)
	{
		throw new RuntimeException(msg);
	}

	static public void error(Throwable t) throws Throwable
	{
		throw t;
	}
	
	
	static public boolean contains(Object[] ary, Object obj)
	{
		if (obj == null)
		  return false;
		
		for (int i = 0; i < ary.length; i++)
		{
			if (ary[i].equals(obj))
			  return true;
		}
		
		return false;
		  
	}
	
	static public boolean contains(String[] ary1, String[] ary2)
	{
		for (int i = 0; i < ary2.length; i++)
		{
			if (!contains(ary1,ary2[i]))
			  return false;
		}
	
		return true;  
	}

	static public String[] intersection(String[] ary1, String[] ary2)
	{
		Vector v = new Vector();
		for(int j = 0; j < ary2.length; ++j)
		{
		  if (contains(ary1,ary2[j]))
			v.add(ary2[j]);
		}
		return (String[]) v.toArray(new String[v.size()]);
	}


	static public String format(double d, String fmt)
	{
		DecimalFormat df = new DecimalFormat(fmt);
		return df.format(d);
	}
	
	static public String format(double d)
	{
		return format(d,DEFAULT_DOUBLE_FORMAT);
	}
	
	static final String DEFAULT_DOUBLE_FORMAT = "#,##0.00"; 
	
	static public double parseFormattedDouble(String s, String fmt) throws ParseException
	{
		DecimalFormat df = new DecimalFormat(fmt);
		return df.parse(s).doubleValue();		
	}	

	static public double parseFormattedDouble(String s) throws ParseException
	{
		return parseFormattedDouble(s, DEFAULT_DOUBLE_FORMAT);		
	}	

	static public String format(Date date)
	{
			return format(date, null);
	}

	static public String format(Date date, String format)
	{
			DateFormat df = format == null ? DateFormat.getDateInstance(DateFormat.SHORT): new SimpleDateFormat(format);
			return df.format(date);
	}


/*

	public static boolean eval(String code)
	{
		OpenL language = OpenL.getInstance("org.openl.j");//?
		Object result = language. evaluate(new StringSourceCodeModule(code, null));
		return result;	???
	}
*/
}

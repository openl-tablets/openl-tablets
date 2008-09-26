package org.openl.rules.helpers;

import java.util.Calendar;
import java.util.Date;

public class DateTool
{
	
	public static final int MONTHS_IN_YEAR = 12;
	public static final int QUARTERS_IN_YEAR = 4;
	public static final int MONTHS_IN_QUARTER = 3;
	public static final int SECONDS_IN_DAY = 60 * 60 * 24;
	
	public static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
	

	public static int year(Date d)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}
	
	public static int month(Date d)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MONTH);
	}
	
	
	public static int dayDiff(Date d1, Date d2)
	{
		return (int)(d1.getTime() / MILLISECONDS_IN_DAY  - d2.getTime()/MILLISECONDS_IN_DAY) ;
	}
	
	
	public static int dayOfMonth(Date d)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.DAY_OF_MONTH);
	}
	
	static public int monthDiff(Date d1, Date d2)
	{
		int y1 = year(d1);
		int m1 = month(d1);
		
		int y2 = year(d2);
		int m2 = month(d2);
		
		int intraMonthCorr = 0;
		if (m1 == m2)
		{ 
			int dm1 = dayOfMonth(d1);
			int dm2 = dayOfMonth(d2);
			
		  intraMonthCorr = dm1 > dm2 ? 1 : (dm1 < dm2 ? -1 : 0); 	
		}
		
		return (y1-y2) * 12 + (m1 - m2) + intraMonthCorr; 
		
	}

	
	/**
	 * 
	 * @param d
	 * @return year quarter from 0 to 3
	 */
	public static int quarter(Date d)
	{
		return month(d)/3;
	}
	
	
	
	public static int absQuarter(Date d)
	{
		return year(d) * QUARTERS_IN_YEAR + quarter(d);
	}
	
	public static int absMonth(Date d)
	{
		return year(d) * MONTHS_IN_YEAR + month(d);
	}
	
	
	
	public static int lastDayOfMonth(Date d)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	
	public static Date firstDateOfQuarter(int absQuarter)
	{
		Calendar c = Calendar.getInstance();
		c.set(absQuarter/QUARTERS_IN_YEAR, (absQuarter%QUARTERS_IN_YEAR)*QUARTERS_IN_YEAR, 1);
		return c.getTime();
	}
	
	public static Date lastDateOfQuarter(int absQuarter)
	{
		Calendar c = Calendar.getInstance();
		c.set(absQuarter/QUARTERS_IN_YEAR, (absQuarter%QUARTERS_IN_YEAR)*MONTHS_IN_QUARTER + 2 , 1);
		
		int lastDay =  lastDayOfMonth(c.getTime());
		
		c.set(Calendar.DAY_OF_MONTH, lastDay);
		return c.getTime();
	}
	
	public static void main(String[] args)
	{
		Date d1 = new Date();
		
		int absQ = absQuarter(d1);
		
		for (int i = 0; i < 20; i++)
		{
			System.out.println("" + i + ".  " +  lastDateOfQuarter(absQ+i));
		}
	}
}

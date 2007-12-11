/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import java.util.Date;

import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class Operators
{
	
	static public int add(int x, int y)
	{
		return x + y;
	}
	
	static public int multiply(int x, int y)
	{
		return x * y;
	}
	
	static public long multiply(long x, long y)
	{
		return x * y;
	}
	
	static public double multiply(double x, double y)
	{
		return x * y;
	}
	
	
	
/*************************** Simple Date arithmetics,  will switch to Joda later**/	
	
	
	static final long SECONDS_IN_DAY = 1000L * 3600 * 24;

	static public Date add(Date d, int days)
	{
	    return new Date(d.getTime() + SECONDS_IN_DAY * days);
	}
	
	static public Date subtract(Date d, int days)
	{
	    return new Date(d.getTime() - SECONDS_IN_DAY * days);
	}
	
	
	static public int subtract(Date d1, Date d2)
	{
	    return (int) ((d1.getTime() / SECONDS_IN_DAY) - (d2.getTime() / SECONDS_IN_DAY)) ;
	}
	


	static public double add(double x, double y)
	{
		return x + y;
	}
	
	static public long add(long x, long y)
	{
		return x + y;
	}
	


	static public double autocast(int x, double y)
	{
		return x;
	}

	static public long autocast(int x, long y)
	{
		return x;
	}

	static public int autocast(char x, int y)
	{
		return x;
	}
	
	static public double autocast(Double D, double d)
	{
		return D.doubleValue();
	}
	
	static public Double autocast(double d, Double D)
	{
		return new Double(d);
	}
	
	static public int autocast(Integer I, int i)
	{
		return I.intValue();
	}
	
	static public Long autocast(long l, Long L)
	{
		return new Long(l);
	}
	
	static public long autocast(Long L, long l)
	{
		return L.longValue();
	}
	
	static public Integer autocast(int i, Integer I)
	{
		return new Integer(i);
	}
	
	static public boolean autocast(Boolean B, boolean b)
	{
		return B.booleanValue();
	}
	
	static public Boolean autocast(boolean b, Boolean B)
	{
		return b ? Boolean.TRUE : Boolean.FALSE;
	}

	
	static public int cast(long x, int y)
	{
		return (int)x;
	}

	static public int cast(double x, int y)
	{
		return (int)x;
	}



	static public boolean lt(double x, double y)
	{
		return x < y;
	}


	static public boolean gt(double x, double y)
	{
		return x > y;
	}
	
	static public boolean gt(int x, int y)
	{
		return x > y;
	}
	
	static public boolean gt(long x, long y)
	{
		return x > y;
	}
	
	static public boolean ge(double x, double y)
	{
		return x >= y;
	}

	static public boolean ge(int x, int y)
	{
		return x >= y;
	}

	static public boolean ge(long x, long y)
	{
		return x >= y;
	}



	static public boolean lt(long x, long y)
	{
		return x < y;
	}
	

	static public int subtract(int x, int y)
	{
		return x - y;
	}
	
	static public double subtract(double x, double y)
	{
		return x - y;
	}
	
	static public long subtract(long x, long y)
	{
		return x - y;
	}
	
	static public int negative(int x)
	{
		return -x;
	}

	static public double negative(double x)
	{
		return -x;
	}


	static public long negative(long x)
	{
		return -x;
	}
	
	static public double divide(double x, double y)
	{
		return x/y;
	}
	
	static public int divide(int x, int y)
	{
		return x/y;
	}
	
	static public long divide(long x, long y)
	{
		return x/y;
	}
	
	
	public static boolean lt(int x, int y)
	{
		return x < y;
	}

	public static long inc(long x)
	{
		return x + 1;
	}

	public static long dec(long x)
	{
		return x - 1;
	}



	public static int inc(int x)
	{
		return x + 1;
	}

	public static int dec(int x)
	{
		return x - 1;
	}

	public static double inc(double x)
	{
		return x + 1;
	}

	public static double dec(double x)
	{
		return x - 1;
	}


	public static String add(String x, String y)
	{
		return x + y;
	}

	public static String add(String x, boolean y)
	{
		return x + y;
	}


	public static String add(Object x, String y)
	{
		return x + y;
	}
	
	
	
	public static String add(String x, Object y)
	{
		return x + y;
	}
	

	public static String add(String x, int y)
	{
		return x + y;
	}
	
	public static String add(String x, double y)
	{
		return x + y;
	}
	
	public static String add(String x, Double y)
	{
		return x + y;
	}
	
	public static String add(String x, long y)
	{
		return x + y;
	}
	
	public static String add(String x, char y)
	{
		return x + y;
	}
	


	
	public static boolean ne(Object x, Object y)
	{
		return x != y;
	}
	
	public static boolean and(boolean x, boolean y)
	{
		return x && y;
	}
	
	public static boolean or(boolean x, boolean y)
	{
		return x || y;
	}
	
	public static boolean xor(boolean x, boolean y)
	{
		return x ^ y;
	}
	
	public static boolean le(int x, int y)
	{
		return x <= y;
	}
	
	public static boolean le(double x, double y)
	{
		return x <= y;
	}
	
	public static boolean le(long x, long y)
	{
		return x <= y;
	}
	
	
	public static boolean le(String x, String[] y)
	{
		return ArrayTool.contains(y, x);
	}
	
	public static boolean eq(boolean x, boolean y)
	{
		return x == y;
	}
	
	public static boolean eq(int x, int y)
	{
		return x == y;
	}
	
	public static boolean eq(long x, long y)
	{
		return x == y;
	}
	

	public static boolean ne(long x, long y)
	{
		return x == y;
	}
	


	public static boolean ne(int x, int y)
	{
		return x != y;
	}
	

	public static boolean eq(double x, double y)
	{
		return x == y;
	}
	
	public static boolean ne(double x, double y)
	{
		return x == y;
	}
	


	public static boolean eq(Object x, Object y)
	{
		return x == y;
	}
	

	public static boolean eq(String x, String y)
	{
		if (x == null)
			return x == y;
		return x.equals(y);
	}
	
	
	
	public static boolean not(boolean x)
	{
		return !x;
	}
	
	static public boolean lt(Comparable c1, Comparable c2)
	{
		return c1.compareTo(c2) < 0;
	}

	static public boolean le(Comparable c1, Comparable c2)
	{
		return c1.compareTo(c2) <= 0;
	}
	
	static public boolean gt(Comparable c1, Comparable c2)
	{
		return c1.compareTo(c2) > 0;
	}

	static public boolean ge(Comparable c1, Comparable c2)
	{
		return c1.compareTo(c2) >= 0;
	}

	
}

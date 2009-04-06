/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
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
	static public int autocast(byte x, int y)
	{
		return x;
	}


	static public char cast(byte x, char y)
	{
		return (char)x;
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
	
	static public Long autocast(long l, double d)
	{
		return l;
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
	
	public static String add(boolean x, String y)
	{
		return x + y;
	}

	public static String add(byte x, String y)
	{
		return x + y;
	}
	
	public static String add(short x, String y)
	{
		return x + y;
	}

	public static String add(char x, String y)
	{
		return x + y;
	}

	public static String add(int x, String y)
	{
		return x + y;
	}

	public static String add(long x, String y)
	{
		return x + y;
	}
	
	public static String add(double x, String y)
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
		return x != y;
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
		return x != y;
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
	

	public static int abs(int x)
	{
		return Math.abs(x);
	}

	public static long abs(long x)
	{
		return Math.abs(x);
	}
	
	public static float abs(float x)
	{
		return Math.abs(x);
	}
	
	public static double abs(double x)
	{
		return Math.abs(x);
	}

	public static int bitnot(int x)
	{
		return ~x;
	}
	
	public static long bitnot(long x)
	{
		return ~x;
	}
	
	
	
	public static boolean not(boolean x)
	{
		return !x;
	}
	
	static public <T> boolean lt(Comparable<T> c1, T c2)
	{
		return c1.compareTo(c2) < 0;
	}

	static public <T> boolean le(Comparable<T> c1, T c2)
	{
		return c1.compareTo(c2) <= 0;
	}
	
	static public <T> boolean gt(Comparable<T> c1, T c2)
	{
		return c1.compareTo(c2) > 0;
	}

	static public <T extends Comparable<?>> boolean ge(Comparable<T> c1, T c2)
	{
		return c1.compareTo(c2) >= 0;
	}

	
	
	static public int bitand(int x, int y)
	{
		return x & y;
	}

	
	static public int bitor(int x, int y)
	{
		return x | y;
	}
	
	static public int bitxor(int x, int y)
	{
		return x ^ y;
	}

	static public boolean bitxor(boolean x, boolean y)
	{
		return x ^ y;
	}
	
	
	static public long bitand(long x, long y)
	{
		return x & y;
	}

	
	static public long bitor(long x, long y)
	{
		return x | y;
	}
	
	static public long bitxor(long x, long y)
	{
		return x ^ y;
	}
	
	
	
	
	
	static public int lshift(int x, int y)
	{
		return x << y;
	}
	
	static public int rshift(int x, int y)
	{
		return x >> y;
	}
	
	static public int rshiftu(int x, int y)
	{
		return x >>> y;
	}
	

	static public long lshift(long x, int y)
	{
		return x << y;
	}
	
	static public long rshift(long x, int y)
	{
		return x >> y;
	}
	
	static public long rshiftu(long x, int y)
	{
		return x >>> y;
	}
	
	
	static public PrintStream lshift(PrintStream p, Object x)
	{
		p.print(x);
		return p;
	}
	
	static public PrintStream lshift(PrintStream p, long x)
	{
		p.print(x);
		return p;
	}
	
	static public BigDecimal autocast(int x, BigDecimal y)
	{
		return new BigDecimal(x);
	}
	
	static public BigDecimal autocast(double x, BigDecimal y)
	{
		return new BigDecimal(x);
	}


	static public BigDecimal autocast(long x, BigDecimal y)
	{
		return new BigDecimal(x);
	}

	static public BigDecimal autocast(BigInteger x, BigDecimal y)
	{
		return new BigDecimal(x);
	}

	
	static public boolean eq(BigDecimal x, BigDecimal y)
	{
		return x.equals(y);
	}

	static public boolean ne(BigDecimal x, BigDecimal y)
	{
		return !x.equals(y);
	}
	
	static public boolean lt(BigDecimal x, BigDecimal y)
	{
		return x.compareTo(y) < 0;
	}

	static public BigInteger autocast(int x, BigInteger y)
	{
		return new BigInteger(String.valueOf(x), 10);
	}
	
	static public BigInteger autocast(long x, BigInteger y)
	{
		return new BigInteger(String.valueOf(x), 10);
	}
	
	static public Number autocast(double d, Number N)
	{
		return (Double)d;
	}

	static public Number autocast(int i, Number N)
	{
		return (Integer)i;
	}
	
	
	
}

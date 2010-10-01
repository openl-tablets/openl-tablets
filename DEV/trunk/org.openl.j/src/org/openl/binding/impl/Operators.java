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

import org.openl.meta.StringValue;
import org.openl.util.ArrayTool;
import org.openl.util.DateDifference;
import org.openl.util.math.MathUtils;

/**
 * @author snshor
 *
 */
public class Operators {

	public static final long SECONDS_IN_DAY = 1000L * 3600 * 24;

    // Add

    public static String add(boolean x, String y) {
        return x + y;
    }

    public static String add(byte x, String y) {
        return x + y;
    }

    public static String add(char x, String y) {
        return x + y;
    }

    public static Date add(Date d, int days) {
        return new Date(d.getTime() + SECONDS_IN_DAY * days);
    }

    public static String add(double x, String y) {
        return x + y;
    }

    public static int add(short x, short y) {
        return x + y;
    }

    public static int add(int x, int y) {
        return x + y;
    }

    public static long add(long x, long y) {
        return x + y;
    }

    public static float add(float x, float y) {
        return x + y;
    }

    public static double add(double x, double y) {
        return x + y;
    }

    public static String add(int x, String y) {
        return x + y;
    }

    public static String add(long x, String y) {
        return x + y;
    }

    public static String add(Object x, String y) {
        return x + y;
    }

    public static String add(short x, String y) {
        return x + y;
    }

    public static String add(String x, boolean y) {
        return x + y;
    }

    public static String add(String x, char y) {
        return x + y;
    }

    public static String add(String x, double y) {
        return x + y;
    }

    public static String add(String x, Double y) {
        return x + y;
    }

    public static String add(String x, int y) {
        return x + y;
    }

    public static String add(String x, long y) {
        return x + y;
    }

    public static String add(String x, Object y) {
        return x + y;
    }

    public static String add(String x, String y) {
        return x + y;
    }

    // Subtract

    public static int subtract(short x, short y) {
        return x - y;
    }

    public static int subtract(int x, int y) {
        return x - y;
    }

    public static long subtract(long x, long y) {
        return x - y;
    }

    public static float subtract(float x, float y) {
        return x - y;
    }

    public static double subtract(double x, double y) {
        return x - y;
    }

    public static boolean xor(boolean x, boolean y) {
        return x ^ y;
    }

    // Multiply

    public static int multiply(short x, short y) {
        return x * y;
    }

    public static int multiply(int x, int y) {
        return x * y;
    }

    public static long multiply(long x, long y) {
        return x * y;
    }

    public static float multiply(float x, float y) {
        return x * y;
    }

    public static double multiply(double x, double y) {
        return x * y;
    }

    // Divide

    public static int divide(short x, short y) {
        return x / y;
    }

    public static int divide(int x, int y) {
        return x / y;
    }

    public static long divide(long x, long y) {
        return x / y;
    }

    public static float divide(float x, float y) {
        return x / y;
    }

    public static double divide(double x, double y) {
        return x / y;
    }

    // Equals
    public static boolean eq(short x, short y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(short x, short y) {
        return x == y;
    }

    public static boolean eq(int x, int y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(int x, int y) {
        return x == y;
    }

    public static boolean eq(long x, long y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(long x, long y) {
        return x == y;
    }

    public static boolean eq(float x, float y) {
    	return MathUtils.eq(x, y);
    }

    public static boolean strict_eq(float x, float y) {
    	return x == y;
    }
    
    public static boolean eq(double x, double y) {
        return MathUtils.eq(x,y);
    }

    public static boolean strict_eq(double x, double y) {
    	return x == y;
    }

    public static boolean eq(Short x, Short y) {
        return strict_eq(x, y);
    }

    public static boolean strict_eq(Short x, Short y) {
        if (x == y) {
            return false;
        }
        
        if (x != null && y != null) {
            return x.equals(y);
        }
        
        return false;
    }

    public static boolean eq(Integer x, Integer y) {
        return strict_eq(x, y);
    }
    
    public static boolean strict_eq(Integer x, Integer y) {
        if (x == y) {
            return true;
        }
        
        if (x != null && y != null) {
            return eq(x.intValue(), y.intValue());
        }
        
        return false; 
    }

    public static boolean eq(Long x, Long y) {
        return strict_eq(x, y);
    }

    public static boolean strict_eq(Long x, Long y) {
        if (x == y) {
            return true;
        }
        
        if (x != null && y != null) {
            return eq(x.longValue(), y.longValue());
        }
        
        return false;
    }

    public static boolean eq(Float x, Float y) {
        if (x == y) {
            return true;
        }
        
        if (x != null && y != null) {
            return eq(x.floatValue(), y.floatValue());
        }
        
        return false;
    }

    public static boolean strict_eq(Float x, Float y) {
        if (x == y) {
            return true;
        }

        if (x != null && y != null) {
            return strict_eq(x.floatValue(), y.floatValue());
        }
        
        return false;
    }

    public static boolean eq(Double x, Double y) {
        if (x == y) {
            return true;
        }

        if (x != null && y != null) {
            return eq(x.doubleValue(), y.doubleValue());
        }
        
        return false;
    }

    public static boolean strict_eq(Double x, Double y) {
        if (x == y) {
            return true;
        }

        if (x != null && y != null) {
            return strict_eq(x.doubleValue(), y.doubleValue());
        }
        
        return false;
    }

    public static boolean eq(Object x, Object y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(Object x, Object y) {
        return x == y;
    }

    public static boolean eq(String x, String y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(String x, String y) {
        if (x == y) {
            return true;
        }

        if (x != null && y != null) {
            return x.equals(y);
        }
        
        return false;    
    }

    public static boolean eq(BigDecimal x, BigDecimal y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(BigDecimal x, BigDecimal y) {
        if (x == y) {
            return true;
        }

        if (x != null && y != null) {
            return x.compareTo(y) == 0;
        }
        
        return false;
    }

    public static boolean eq(boolean x, boolean y) {
    	return strict_eq(x, y);
    }
    
    public static boolean strict_eq(boolean x, boolean y) {
        return x == y;
    }

    public static boolean eq(Boolean x, Boolean y) {
        return strict_eq(x, y);
    }
    
    public static boolean strict_eq(Boolean x, Boolean y) {
        if (x == y) {
            return true;
        }

        if (x != null && y != null) {
            return x.booleanValue() == y.booleanValue();
        }
        
        return false;
    }

    // Not Equals
    public static boolean ne(short x, short y) {
    	return strict_ne(x, y);
    }
    
    public static boolean strict_ne(short x, short y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(int x, int y) {
    	return strict_ne(x, y);
    }
    
    public static boolean strict_ne(int x, int y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(long x, long y) {
    	return strict_ne(x, y);
    }
    
    public static boolean strict_ne(long x, long y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(float x, float y) {
        return MathUtils.ne(x, y);
    }

    public static boolean strict_ne(float x, float y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(double x, double y) {
    	return MathUtils.ne(x, y);
    }

    public static boolean strict_ne(double x, double y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Short x, Short y) {
        return !eq(x, y);
    }
    
    public static boolean strict_ne(Short x, Short y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Integer x, Integer y) {
        return !eq(x, y);
    }
    
    public static boolean strict_ne(Integer x, Integer y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Long x, Long y) {
        return !eq(x, y);
    }
    
    public static boolean strict_ne(Long x, Long y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Float x, Float y) {
        return !eq(x, y);
    }

    public static boolean strict_ne(Float x, Float y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Double x, Double y) {
        return !eq(x, y);
    }

    public static boolean strict_ne(Double x, Double y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Object x, Object y) {
    	return strict_ne(x, y);
    }

    public static boolean strict_ne(Object x, Object y) {
        return x != y;
    }

    public static boolean ne(boolean x, boolean y) {
    	return !eq(x, y);
    }

    public static boolean strict_ne(boolean x, boolean y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(Boolean x, Boolean y) {
        return !eq(x, y);
    }

    public static boolean strict_ne(Boolean x, Boolean y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(BigDecimal x, BigDecimal y) {
        return !eq(x, y);
    }

    public static boolean strict_ne(BigDecimal x, BigDecimal y) {
        return !strict_eq(x, y);
    }

    public static boolean ne(String x, String y) {
        return !eq(x, y);
    }

    public static boolean strict_ne(String x, String y) {
        return !strict_eq(x, y);
    }

    // Greater Than
    public static boolean gt(short x, short y) {
        return strict_gt(x, y);
    }

    public static boolean strict_gt(short x, short y) {
        return x > y;
    }

    public static boolean gt(int x, int y) {
    	return strict_gt(x, y);
    }

    public static boolean strict_gt(int x, int y) {
        return x > y;
    }

    public static boolean gt(long x, long y) {
    	return strict_gt(x, y);
    }

    public static boolean strict_gt(long x, long y) {
        return x > y;
    }

    public static boolean gt(float x, float y) {
    	return MathUtils.gt(x, y);
    }

    public static boolean strict_gt(float x, float y) {
    	return x > y;
    }

    public static boolean gt(double x, double y) {
    	return MathUtils.gt(x, y);
    }

    public static boolean strict_gt(double x, double y) {
    	return x > y;
    }

    public static <T> boolean gt(Comparable<T> c1, T c2) {
    	return strict_gt(c1, c2);
    }

    public static <T> boolean strict_gt(Comparable<T> c1, T c2) {
        return c1.compareTo(c2) > 0;
    }

    // Greater or Equals Than
    public static boolean ge(short x, short y) {
        return strict_ge(x, y);
    }

    public static boolean strict_ge(short x, short y) {
        return x >= y;
    }

    public static boolean ge(int x, int y) {
    	return strict_ge(x, y);
    }

    public static boolean strict_ge(int x, int y) {
        return x >= y;
    }

    public static boolean ge(long x, long y) {
    	return strict_ge(x, y);
    }

    public static boolean strict_ge(long x, long y) {
        return x >= y;
    }

    public static boolean ge(float x, float y) {
    	return MathUtils.ge(x, y);
    }

    public static boolean strict_ge(float x, float y) {
    	return x >= y;
    }

    public static boolean ge(double x, double y) {
    	return MathUtils.ge(x, y);
    }

    public static boolean strict_ge(double x, double y) {
    	return x >= y;
    }

    public static <T extends Comparable<?>> boolean ge(Comparable<T> c1, T c2) {
        return c1.compareTo(c2) >= 0;
    }

    public static <T extends Comparable<?>> boolean strict_ge(Comparable<T> c1, T c2) {
        return c1.compareTo(c2) >= 0;
    }

    // Less Than
    public static boolean lt(short x, short y) {
        return strict_lt(x, y);
    }

    public static boolean strict_lt(short x, short y) {
        return x < y;
    }

    public static boolean lt(int x, int y) {
    	return strict_lt(x, y);
    }

    public static boolean strict_lt(int x, int y) {
        return x < y;
    }

    public static boolean lt(long x, long y) {
    	return strict_lt(x, y);
    }

    public static boolean strict_lt(long x, long y) {
        return x < y;
    }

    public static boolean lt(float x, float y) {
    	return MathUtils.lt(x, y);
    }

    public static boolean strict_lt(float x, float y) {
    	return x < y;
    }

    public static boolean lt(double x, double y) {
    	return MathUtils.lt(x, y);
    }

    public static boolean strict_lt(double x, double y) {
    	return x < y;
    }

    public static boolean lt(BigDecimal x, BigDecimal y) {
    	return strict_lt(x, y);
    }

    public static boolean strict_lt(BigDecimal x, BigDecimal y) {
        return x.compareTo(y) < 0;
    }

    public static <T> boolean lt(Comparable<T> c1, T c2) {
    	return strict_lt(c1, c2);
    }

    public static <T> boolean strict_lt(Comparable<T> c1, T c2) {
        return c1.compareTo(c2) < 0;
    }

    // Less or Equals Than

    public static boolean le(short x, short y) {
        return strict_le(x, y);
    }

    public static boolean strict_le(short x, short y) {
        return x <= y;
    }

    public static boolean le(int x, int y) {
    	return strict_le(x, y);
    }

    public static boolean strict_le(int x, int y) {
        return x <= y;
    }

    public static boolean le(long x, long y) {
    	return strict_le(x, y);
    }

    public static boolean strict_le(long x, long y) {
        return x <= y;
    }

    public static boolean le(float x, float y) {
    	return MathUtils.le(x, y);
    }

    public static boolean strict_le(float x, float y) {
    	return x <= y;
    }

    public static boolean le(double x, double y) {
    	return MathUtils.le(x, y);
    }

    public static boolean strict_le(double x, double y) {
    	return x <= y;
    }

    public static <T> boolean le(Comparable<T> c1, T c2) {
    	return strict_le(c1, c2);
    }

    public static <T> boolean strict_le(Comparable<T> c1, T c2) {
        return c1.compareTo(c2) <= 0;
    }

    public static boolean le(String x, String[] y) {
    	return strict_le(x, y);
    }

    public static boolean strict_le(String x, String[] y) {
        return ArrayTool.contains(y, x);
    }

    // Abs

    public static int abs(int x) {
        return Math.abs(x);
    }

    public static long abs(long x) {
        return Math.abs(x);
    }

    public static float abs(float x) {
        return Math.abs(x);
    }

    public static double abs(double x) {
        return Math.abs(x);
    }

    public static boolean and(boolean x, boolean y) {
        return x && y;
    }

    // Autocast

    public static BigDecimal autocast(BigInteger x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static Boolean autocast(boolean b, Boolean B) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }

    public static boolean autocast(Boolean B, boolean b) {
        return B.booleanValue();
    }

    public static int autocast(byte x, int y) {
        return x;
    }

    public static int autocast(char x, int y) {
        return x;
    }

    public static BigDecimal autocast(double x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static BigDecimal autocast(Double x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static Double autocast(double d, Double D) {
        return new Double(d);
    }

    public static Number autocast(double d, Number N) {
        return d;
    }

    public static double autocast(Double D, double d) {
        return D.doubleValue();
    }

    public static BigDecimal autocast(int x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static BigInteger autocast(int x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigInteger autocast(long x, BigInteger y) {
        return BigInteger.valueOf(x);
    }
    
    public static BigInteger autocast(Integer x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigInteger autocast(Long x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static double autocast(int x, double y) {
        return x;
    }
    
    public static Integer autocast(int i, Integer I) {
        return new Integer(i);
    }

    public static long autocast(int x, long y) {
        return x;
    }

    public static Number autocast(int i, Number N) {
        return i;
    }

    public static int autocast(Integer I, int i) {
        return I.intValue();
    }

    public static BigDecimal autocast(long x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static Long autocast(long l, double d) {
        return l;
    }

    public static Long autocast(long l, Long L) {
        return new Long(l);
    }

    public static long autocast(Long L, long l) {
        return L.longValue();
    }

    public static String autocast(StringValue x, String y) {
        return x.getValue();
    }
    
    public static int bitand(int x, int y) {
        return x & y;
    }

    public static long bitand(long x, long y) {
        return x & y;
    }

    public static int bitnot(int x) {
        return ~x;
    }

    public static long bitnot(long x) {
        return ~x;
    }

    public static int bitor(int x, int y) {
        return x | y;
    }

    public static long bitor(long x, long y) {
        return x | y;
    }

    public static boolean bitxor(boolean x, boolean y) {
        return x ^ y;
    }

    public static int bitxor(int x, int y) {
        return x ^ y;
    }

    public static long bitxor(long x, long y) {
        return x ^ y;
    }

    public static char cast(byte x, char y) {
        return (char) x;
    }

    public static int cast(double x, int y) {
        return (int) x;
    }

    public static int cast(long x, int y) {
        return (int) x;
    }

    public static int cast(String x, int y) {
        return Integer.parseInt(x);
    }

    public static double cast(String x, double y) {
        return Double.parseDouble(x);
    }

    public static long cast(String x, long y) {
        return Long.parseLong(x);
    }

    public static BigDecimal cast(String x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static double dec(double x) {
        return x - 1;
    }

    public static int dec(int x) {
        return x - 1;
    }

    public static long dec(long x) {
        return x - 1;
    }
    
    public static double inc(double x) {
        return x + 1;
    }

    public static int inc(int x) {
        return x + 1;
    }

    public static long inc(long x) {
        return x + 1;
    }

    public static int lshift(int x, int y) {
        return x << y;
    }

    public static long lshift(long x, int y) {
        return x << y;
    }

    public static PrintStream lshift(PrintStream p, long x) {
        p.print(x);
        return p;
    }

    public static PrintStream lshift(PrintStream p, Object x) {
        p.print(x);
        return p;
    }

    public static int pow(int x, int y) {
        return (int)Math.pow(x, y);
    }
    
    public static long pow(long x, long y) {
        return (long)Math.pow(x, y);
    }

    public static double pow(double x, double y) {
        return Math.pow(x, y);
    }

    // Negative

    public static double negative(double x) {
        return -x;
    }

    public static int negative(int x) {
        return -x;
    }

    public static long negative(long x) {
        return -x;
    }

    public static boolean not(boolean x) {
        return !x;
    }

    public static boolean or(boolean x, boolean y) {
        return x || y;
    }

    public static int rshift(int x, int y) {
        return x >> y;
    }

    public static long rshift(long x, int y) {
        return x >> y;
    }

    public static int rshiftu(int x, int y) {
        return x >>> y;
    }

    public static long rshiftu(long x, int y) {
        return x >>> y;
    }

    public static int subtract(Date d1, Date d2) {        
        return DateDifference.getDifferenceInDays(d1, d2);
    }

    public static Date subtract(Date d, int days) {
        return new Date(d.getTime() - SECONDS_IN_DAY * days);
    }

}

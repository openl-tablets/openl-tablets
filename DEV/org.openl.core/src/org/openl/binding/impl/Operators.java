/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Calendar;
import java.util.Date;

import org.openl.util.DateDifference;

/**
 * @author snshor
 * 
 */
public class Operators {

    public static final long SECONDS_IN_DAY = 1000L * 3600 * 24;

    // Add

    public static String add(Object x, String y) {
        return x + y;
    }

    public static String add(String x, Object y) {
        return x + y;
    }

    public static String add(String x, String y) {
        return x + y;
    }

    public static String add(boolean x, String y) {
        return x + y;
    }

    public static String add(String x, boolean y) {
        return x + y;
    }

    public static String add(char x, String y) {
        return x + y;
    }

    public static String add(String y, char x) {
        return y + x;
    }

    public static Date add(Date d, int days) {
        // We should use calendar to take into account daylight saving
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }

    public static byte add(byte x, byte y) {
        return (byte) (x + y);
    }

    public static short add(short x, short y) {
        return (short) (x + y);
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

    public static Byte add(Byte x, Byte y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return (byte) (x + y);
    }

    public static Short add(Short x, Short y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return (short) (x + y);
    }

    public static Integer add(Integer x, Integer y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x + y;
    }

    public static Long add(Long x, Long y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x + y;
    }

    public static Float add(Float x, Float y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x + y;
    }

    public static Double add(Double x, Double y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x + y;
    }

    public static BigInteger add(BigInteger x, BigInteger y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x.add(y);
    }

    public static BigDecimal add(BigDecimal x, BigDecimal y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x.add(y);
    }

    // Subtract
    public static byte subtract(byte x, byte y) {
        return (byte) (x - y);
    }

    public static short subtract(short x, short y) {
        return (short) (x - y);
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

    public static Byte subtract(Byte x, Byte y) {
        if (x == null && y != null) {
            return (byte) -y;
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return (byte) (x - y);
    }

    public static Short subtract(Short x, Short y) {
        if (x == null && y != null) {
            return (short) -y;
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return (short) (x - y);
    }

    public static Integer subtract(Integer x, Integer y) {
        if (x == null && y != null) {
            return -y;
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return x - y;
    }

    public static Long subtract(Long x, Long y) {
        if (x == null && y != null) {
            return -y;
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return x - y;
    }

    public static Float subtract(Float x, Float y) {
        if (x == null && y != null) {
            return -y;
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return x - y;
    }

    public static Double subtract(Double x, Double y) {
        if (x == null && y != null) {
            return -y;
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return x - y;
    }

    public static BigInteger subtract(BigInteger x, BigInteger y) {
        if (x == null && y != null) {
            return y.negate();
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return x.subtract(y);
    }

    public static BigDecimal subtract(BigDecimal x, BigDecimal y) {
        if (x == null && y != null) {
            return y.negate();
        } else if (x == null) {
            return null;
        } else if (y == null) {
            return x;
        }
        return x.subtract(y);
    }

    public static boolean xor(boolean x, boolean y) {
        return x ^ y;
    }

    // Multiply
    public static byte multiply(byte x, byte y) {
        return (byte) (x * y);
    }

    public static short multiply(short x, short y) {
        return (short) (x * y);
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

    public static Byte multiply(Byte x, Byte y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return (byte) (x * y);
    }

    public static Short multiply(Short x, Short y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return (short) (x * y);
    }

    public static Integer multiply(Integer x, Integer y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static Long multiply(Long x, Long y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static Float multiply(Float x, Float y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static Double multiply(Double x, Double y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x * y;
    }

    public static BigInteger multiply(BigInteger x, BigInteger y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x.multiply(y);
    }

    public static BigDecimal multiply(BigDecimal x, BigDecimal y) {
        if (x == null) {
            return y;
        } else if (y == null) {
            return x;
        }
        return x.multiply(y);
    }

    // Divide
    public static double divide(byte x, byte y) {
        return ((double) x) / y;
    }

    public static double divide(short x, short y) {
        return ((double) x) / y;
    }

    public static double divide(int x, int y) {
        return ((double) x) / y;
    }

    public static double divide(long x, long y) {
        return ((double) x) / y;
    }

    public static float divide(float x, float y) {
        return x / y;
    }

    public static double divide(double x, double y) {
        return x / y;
    }

    public static Double divide(Byte x, Byte y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1;
        }
        return ((double) x / y);
    }

    public static Double divide(Short x, Short y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1;
        }
        return ((double) x / y);
    }

    public static Double divide(Integer x, Integer y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1;
        }
        return ((double) x / y);
    }

    public static Double divide(Long x, Long y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return Double.valueOf(x);
        } else if (x == null) {
            x = 1l;
        }
        return ((double) x / y);
    }

    public static Float divide(Float x, Float y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1.0f;
        }
        return x / y;
    }

    public static Double divide(Double x, Double y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = 1.0;
        }
        return x / y;
    }

    public static BigDecimal divide(BigInteger x, BigInteger y) {
        if (x == null && y == null) {
            return null;
        } else if (y == null) {
            return new BigDecimal(x);
        } else if (x == null) {
            x = BigInteger.ONE;
        }

        return new BigDecimal(x).divide(new BigDecimal(y), MathContext.DECIMAL128);
    }

    public static BigDecimal divide(BigDecimal x, BigDecimal y) {
        if (y == null) {
            return x;
        } else if (x == null) {
            x = BigDecimal.ONE;
        }
        return x.divide(y, MathContext.DECIMAL128);
    }

    // Less or Equals Than

    // Abs
    public static byte abs(byte x) {
        return (byte) Math.abs(x);
    }

    public static short abs(short x) {
        return (short) Math.abs(x);
    }

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

    public static BigInteger abs(BigInteger x) {
        return x.abs();
    }

    public static BigDecimal abs(BigDecimal x) {
        return x.abs();
    }

    // Bitwise operators
    //
    public static byte bitand(byte x, byte y) {
        return (byte) (x & y);
    }

    public static short bitand(short x, short y) {
        return (short) (x & y);
    }

    public static int bitand(int x, int y) {
        return x & y;
    }

    public static long bitand(long x, long y) {
        return x & y;
    }

    public static byte bitnot(byte x) {
        return (byte) ~x;
    }

    public static short bitnot(short x) {
        return (short) ~x;
    }

    public static int bitnot(int x) {
        return ~x;
    }

    public static long bitnot(long x) {
        return ~x;
    }

    public static byte bitor(byte x, byte y) {
        return (byte) (x | y);
    }

    public static short bitor(short x, short y) {
        return (short) (x | y);
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

    public static byte bitxor(byte x, byte y) {
        return (byte) (x ^ y);
    }

    public static short bitxor(short x, short y) {
        return (short) (x ^ y);
    }

    public static int bitxor(int x, int y) {
        return x ^ y;
    }

    public static long bitxor(long x, long y) {
        return x ^ y;
    }

    public static float dec(float x) {
        return x - 1;
    }

    public static double dec(double x) {
        return x - 1;
    }

    public static byte dec(byte x) {
        return (byte) (x - 1);
    }

    public static short dec(short x) {
        return (short) (x - 1);
    }

    public static int dec(int x) {
        return x - 1;
    }

    public static long dec(long x) {
        return x - 1;
    }

    public static BigInteger dec(BigInteger x) {
        return x.subtract(BigInteger.ONE);
    }

    public static BigDecimal dec(BigDecimal x) {
        return x.subtract(BigDecimal.ONE);
    }

    public static byte inc(byte x) {
        return (byte) (x + 1);
    }

    public static short inc(short x) {
        return (short) (x + 1);
    }

    public static float inc(float x) {
        return x + 1;
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

    public static BigInteger inc(BigInteger x) {
        return x.add(BigInteger.ONE);
    }

    public static BigDecimal inc(BigDecimal x) {
        return x.add(BigDecimal.ONE);
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

    public static byte pow(byte x, byte y) {
        return (byte) Math.pow(x, y);
    }

    public static short pow(short x, short y) {
        return (short) Math.pow(x, y);
    }

    public static int pow(int x, int y) {
        return (int) Math.pow(x, y);
    }

    public static long pow(long x, long y) {
        return (long) Math.pow(x, y);
    }

    public static float pow(float x, float y) {
        return (float) Math.pow(x, y);
    }

    public static double pow(double x, double y) {
        return Math.pow(x, y);
    }

    public static Byte pow(Byte x, Byte y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return 1;
        }
        return (byte) Math.pow(x, y);
    }

    public static Short pow(Short x, Short y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return 1;
        }
        return (short) Math.pow(x, y);
    }

    public static Integer pow(Integer x, Integer y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return 1;
        }
        return (int) Math.pow(x, y);
    }

    public static Long pow(Long x, Long y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return 1L;
        }
        return (long) Math.pow(x, y);
    }

    public static Float pow(Float x, Float y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return 1.0f;
        }
        return (float) Math.pow(x, y);
    }

    public static Double pow(Double x, Double y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return 1.0;
        }
        return Math.pow(x, y);
    }

    public static BigInteger pow(BigInteger x, BigInteger y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return BigInteger.ONE;
        }
        return x.pow(y.intValue());
    }

    public static BigDecimal pow(BigDecimal x, BigDecimal y) {
        if (x == null) {
            return null;
        } else if (y == null) {
            return BigDecimal.ONE;
        }
        return x.pow(y.intValue());
    }

    // Negative

    public static byte negative(byte x) {
        return (byte) -x;
    }

    public static short negative(short x) {
        return (short) -x;
    }

    public static float negative(float x) {
        return -x;
    }

    public static double negative(double x) {
        return -x;
    }

    public static int negative(int x) {
        return -x;
    }

    public static long negative(long x) {
        return -x;
    }

    public static Byte negative(Byte x) {
        return x == null ? null : (byte) -x;
    }

    public static Short negative(Short x) {
        return x == null ? null : (short) -x;
    }

    public static Float negative(Float x) {
        return x == null ? null : -x;
    }

    public static Double negative(Double x) {
        return x == null ? null : -x;
    }

    public static Integer negative(Integer x) {
        return x == null ? null : -x;
    }

    public static Long negative(Long x) {
        return x == null ? null : -x;
    }

    public static BigInteger negative(BigInteger x) {
        return x != null ? x.negate() : null;
    }

    public static BigDecimal negative(BigDecimal x) {
        return x != null ? x.negate() : null;
    }

    public static float positive(float x) {
        return x;
    }

    public static double positive(double x) {
        return x;
    }

    public static byte positive(byte x) {
        return x;
    }

    public static short positive(short x) {
        return x;
    }

    public static int positive(int x) {
        return x;
    }

    public static long positive(long x) {
        return x;
    }

    public static Float positive(Float x) {
        return x;
    }

    public static Double positive(Double x) {
        return x;
    }

    public static Byte positive(Byte x) {
        return x;
    }

    public static Short positive(Short x) {
        return x;
    }

    public static Integer positive(Integer x) {
        return x;
    }

    public static Long positive(Long x) {
        return x;
    }

    public static BigInteger positive(BigInteger x) {
        return x;
    }

    public static BigDecimal positive(BigDecimal x) {
        return x;
    }

    public static Boolean not(Boolean x) {
        return x == null ? null : !x;
    }

    public static Boolean or(Boolean x, Boolean y) {
        return Boolean.TRUE.equals(
            x) ? Boolean.TRUE : Boolean.TRUE.equals(y) ? Boolean.TRUE : (x == null || y == null) ? null : Boolean.FALSE;
    }

    public static Boolean and(Boolean x, Boolean y) {
        return Boolean.FALSE.equals(x) ? Boolean.FALSE
                                       : Boolean.FALSE.equals(y) ? Boolean.FALSE
                                                                 : (x == null || y == null) ? null : Boolean.TRUE;
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

    // operator '%' implementations

    public static byte rem(byte x, byte y) {
        return (byte) (x % y);
    }

    public static short rem(short x, short y) {
        return (short) (x % y);
    }

    public static int rem(int x, int y) {
        return x % y;
    }

    public static long rem(long x, long y) {
        return x % y;
    }

    public static float rem(float x, float y) {
        return x % y;
    }

    public static double rem(double x, double y) {
        return x % y;
    }

    public static BigInteger rem(BigInteger x, BigInteger y) {
        return x.remainder(y);
    }

    public static BigDecimal rem(BigDecimal x, BigDecimal y) {
        return x.remainder(y);
    }

    public static int subtract(Date d1, Date d2) {
        return DateDifference.getDifferenceInDays(d1, d2);
    }

    public static Date subtract(Date d, int days) {
        return add(d, -days);
    }

}

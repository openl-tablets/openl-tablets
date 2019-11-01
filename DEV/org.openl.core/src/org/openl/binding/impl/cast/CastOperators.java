package org.openl.binding.impl.cast;

import java.math.BigDecimal;
import java.math.BigInteger;

public class CastOperators {
    // Autocast

    // Widening primitive conversions:
    // # byte to short, int, long, float, or double
    // # short to int, long, float, or double
    // # char to int, long, float, or double
    // # int to long, float, or double
    // # long to float or double
    // # float to double

    public static short autocast(byte x, short y) {
        return x;
    }

    public static int autocast(byte x, int y) {
        return x;
    }

    public static long autocast(byte x, long y) {
        return x;
    }

    public static float autocast(byte x, float y) {
        return x;
    }

    public static double autocast(byte x, double y) {
        return x;
    }

    public static BigInteger autocast(byte x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal autocast(byte x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static int autocast(short x, int y) {
        return x;
    }

    public static long autocast(short x, long y) {
        return x;
    }

    public static float autocast(short x, float y) {
        return x;
    }

    public static double autocast(short x, double y) {
        return x;
    }

    public static BigInteger autocast(short x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal autocast(short x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static int autocast(char x, int y) {
        return x;
    }

    public static long autocast(char x, long y) {
        return x;
    }

    public static float autocast(char x, float y) {
        return x;
    }

    public static double autocast(char x, double y) {
        return x;
    }

    public static BigInteger autocast(char x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal autocast(char x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static long autocast(int x, long y) {
        return x;
    }

    public static float autocast(int x, float y) {
        return x;
    }

    public static double autocast(int x, double y) {
        return x;
    }

    public static BigInteger autocast(int x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal autocast(int x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static float autocast(long x, float y) {
        return x;
    }

    public static double autocast(long x, double y) {
        return x;
    }

    public static BigInteger autocast(long x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal autocast(long x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static double autocast(float x, double y) {
        return Double.valueOf(Float.toString(x));
    }

    public static BigDecimal autocast(float x, BigDecimal y) {
        return new BigDecimal(Float.toString(x));
    }

    public static BigDecimal autocast(double x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static BigDecimal autocast(BigInteger x, BigDecimal y) {
        return new BigDecimal(x);
    }

    // Narrowing primitive conversions:
    //
    // * byte to char
    // * short to byte or char
    // * char to byte or short
    // * int to byte, short, or char
    // * long to byte, short, char, or int
    // * float to byte, short, char, int, or long
    // * double to byte, short, char, int, long, or float
    // * BigInteger to

    public static char cast(byte x, char y) {
        return (char) x;
    }

    public static BigInteger cast(byte x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal cast(byte x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static char cast(short x, char y) {
        return (char) x;
    }

    public static byte cast(short x, byte y) {
        return (byte) x;
    }

    public static BigInteger cast(short x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal cast(short x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static byte cast(char x, byte y) {
        return (byte) x;
    }

    public static short cast(char x, short y) {
        return (short) x;
    }

    public static BigInteger cast(char x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal cast(char x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static byte cast(int x, byte y) {
        return (byte) x;
    }

    public static short cast(int x, short y) {
        return (short) x;
    }

    public static char cast(int x, char y) {
        return (char) x;
    }

    public static BigInteger cast(int x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal cast(int x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static byte cast(long x, byte y) {
        return (byte) x;
    }

    public static short cast(long x, short y) {
        return (short) x;
    }

    public static char cast(long x, char y) {
        return (char) x;
    }

    public static int cast(long x, int y) {
        return (int) x;
    }

    public static BigInteger cast(long x, BigInteger y) {
        return BigInteger.valueOf(x);
    }

    public static BigDecimal cast(long x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static byte cast(float x, byte y) {
        return (byte) x;
    }

    public static short cast(float x, short y) {
        return (short) x;
    }

    public static char cast(float x, char y) {
        return (char) x;
    }

    public static int cast(float x, int y) {
        return (int) x;
    }

    public static long cast(float x, long y) {
        return (long) x;
    }

    public static BigInteger cast(float x, BigInteger y) {
        return new BigDecimal(String.valueOf(x)).toBigInteger();
    }

    public static BigDecimal cast(float x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static byte cast(double x, byte y) {
        return (byte) x;
    }

    public static short cast(double x, short y) {
        return (short) x;
    }

    public static char cast(double x, char y) {
        return (char) x;
    }

    public static int cast(double x, int y) {
        return (int) x;
    }

    public static long cast(double x, long y) {
        return (long) x;
    }

    public static float cast(double x, float y) {
        return (float) x;
    }

    public static BigInteger cast(double x, BigInteger y) {
        return BigDecimal.valueOf(x).toBigInteger();
    }

    public static BigDecimal cast(double x, BigDecimal y) {
        return BigDecimal.valueOf(x);
    }

    public static byte cast(BigInteger x, byte y) {
        return x.byteValue();
    }

    public static short cast(BigInteger x, short y) {
        return x.shortValue();
    }

    public static int cast(BigInteger x, int y) {
        return x.intValue();
    }

    public static char cast(BigInteger x, char y) {
        return (char) x.intValue();
    }

    public static long cast(BigInteger x, long y) {
        return x.longValue();
    }

    public static float cast(BigInteger x, float y) {
        return x.floatValue();
    }

    public static double cast(BigInteger x, double y) {
        return x.doubleValue();
    }

    public static byte cast(BigDecimal x, byte y) {
        return x.byteValue();
    }

    public static short cast(BigDecimal x, short y) {
        return x.shortValue();
    }

    public static int cast(BigDecimal x, int y) {
        return x.intValue();
    }

    public static char cast(BigDecimal x, char y) {
        return (char) x.intValue();
    }

    public static long cast(BigDecimal x, long y) {
        return x.longValue();
    }

    public static float cast(BigDecimal x, float y) {
        return x.floatValue();
    }

    public static double cast(BigDecimal x, double y) {
        return x.doubleValue();
    }

    public static BigInteger cast(BigDecimal x, BigInteger y) {
        return x.toBigInteger();
    }
}

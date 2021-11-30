package org.openl.rules.binding;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openl.conf.OperatorsNamespace;

@OperatorsNamespace
@Deprecated
public class StringOperators {

    private static final int STRING_TO_TYPE_DISTANCE = 40;
    private static final int TYPE_TO_STRING_DISTANCE = 41;

    public static Byte add(Byte x, String y) {
        return (byte) (x + Byte.parseByte(y));
    }

    public static Byte add(String x, Byte y) {
        return (byte) (y + Byte.parseByte(x));
    }

    public static Short add(Short x, String y) {
        return (short) (x + Short.parseShort(y));
    }

    public static Short add(String x, Short y) {
        return (short) (y + Short.parseShort(x));
    }

    public static Integer add(Integer x, String y) {
        return x + Integer.parseInt(y);
    }

    public static Integer add(String x, Integer y) {
        return y + Integer.parseInt(x);
    }

    public static Long add(Long x, String y) {
        return x + Long.parseLong(y);
    }

    public static Long add(String x, Long y) {
        return y + Long.parseLong(x);
    }

    public static Float add(Float x, String y) {
        return x + Float.parseFloat(y);
    }

    public static Float add(String x, Float y) {
        return y + Float.parseFloat(x);
    }

    public static Double add(Double x, String y) {
        return x + Double.parseDouble(y);
    }

    public static Double add(String x, Double y) {
        return y + Double.parseDouble(x);
    }

    public static BigInteger add(String x, BigInteger y) {
        return new BigInteger(x).add(y);
    }

    public static BigInteger add(BigInteger x, String y) {
        return new BigInteger(y).add(x);
    }

    public static BigDecimal add(String x, BigDecimal y) {
        return new BigDecimal(x).add(y);
    }

    public static BigDecimal add(BigDecimal x, String y) {
        return new BigDecimal(y).add(x);
    }

    // AutoCasts to String
    public static String autocast(byte x, String y) {
        return Byte.toString(x);
    }

    public static Integer distance(byte x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static byte autocast(String x, byte y) {
        return Byte.parseByte(x);
    }

    public static Integer distance(String x, byte y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(short x, String y) {
        return Short.toString(x);
    }

    public static Integer distance(short x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static short autocast(String x, short y) {
        return Short.parseShort(x);
    }

    public static Integer distance(String x, short y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(int x, String y) {
        return Integer.toString(x);
    }

    public static Integer distance(int x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static int autocast(String x, int y) {
        return Integer.parseInt(x);
    }

    public static Integer distance(String x, int y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(long x, String y) {
        return Long.toString(x);
    }

    public static Integer distance(long x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static long autocast(String x, long y) {
        return Long.parseLong(x);
    }

    public static Integer distance(String x, long y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(float x, String y) {
        return Float.toString(x);
    }

    public static Integer distance(float x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static float autocast(String x, float y) {
        return Float.parseFloat(x);
    }

    public static Integer distance(String x, float y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(double x, String y) {
        return Double.toString(x);
    }

    public static Integer distance(double x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static double autocast(String x, double y) {
        return Double.parseDouble(x);
    }

    public static Integer distance(String x, double y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    // AutoCasts to String
    public static String autocast(Byte x, String y) {
        return Byte.toString(x);
    }

    public static Integer distance(Byte x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Byte autocast(String x, Byte y) {
        return x == null ? null : Byte.parseByte(x);
    }

    public static Integer distance(String x, Byte y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Short x, String y) {
        return Short.toString(x);
    }

    public static Integer distance(Short x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Short autocast(String x, Short y) {
        return x == null ? null : Short.parseShort(x);
    }

    public static Integer distance(String x, Short y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Integer x, String y) {
        return Integer.toString(x);
    }

    public static Integer distance(Integer x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Integer autocast(String x, Integer y) {
        return x == null ? null : Integer.parseInt(x);
    }

    public static Integer distance(String x, Integer y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Long x, String y) {
        return Long.toString(x);
    }

    public static Integer distance(Long x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Long autocast(String x, Long y) {
        return x == null ? null : Long.parseLong(x);
    }

    public static Integer distance(String x, Long y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Float x, String y) {
        return Float.toString(x);
    }

    public static Integer distance(Float x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Float autocast(String x, Float y) {
        return x == null ? null : Float.parseFloat(x);
    }

    public static Integer distance(String x, Float y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(Double x, String y) {
        return Double.toString(x);
    }

    public static Integer distance(Double x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static Double autocast(String x, Double y) {
        return x == null ? null : Double.parseDouble(x);
    }

    public static Integer distance(String x, Double y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(BigDecimal x, String y) {
        return x.toString();
    }

    public static Integer distance(BigDecimal x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static BigDecimal autocast(String x, BigDecimal y) {
        return new BigDecimal(x);
    }

    public static Integer distance(String x, BigDecimal y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static String autocast(BigInteger x, String y) {
        return x.toString();
    }

    public static Integer distance(BigInteger x, String y) {
        return TYPE_TO_STRING_DISTANCE;
    }

    public static BigInteger autocast(String x, BigInteger y) {
        return new BigInteger(x);
    }

    public static Integer distance(String x, BigInteger y) {
        return STRING_TO_TYPE_DISTANCE;
    }

    public static boolean gt(String x, Integer y) {
        return x != null && y != null && Integer.parseInt(x) > y;
    }

    public static boolean ge(String x, Integer y) {
        return x != null && y != null && Integer.parseInt(x) >= y;
    }

    public static boolean lt(String x, Integer y) {
        return x != null && y != null && Integer.parseInt(x) < y;
    }

    public static boolean le(String x, Integer y) {
        return x != null && y != null && Integer.parseInt(x) <= y;
    }

    public static boolean eq(String x, Integer y) {
        return x == null && y == null || x != null && y != null && Integer.parseInt(x) == y;
    }

    public static boolean ne(String x, Integer y) {
        return !eq(x, y);
    }

    public static boolean gt(Integer x, String y) {
        return x != null && y != null && Integer.parseInt(y) < x;
    }

    public static boolean ge(Integer x, String y) {
        return x != null && y != null && Integer.parseInt(y) <= x;
    }

    public static boolean lt(Integer x, String y) {
        return x != null && y != null && Integer.parseInt(y) > x;
    }

    public static boolean le(Integer x, String y) {
        return x != null && y != null && Integer.parseInt(y) >= x;
    }

    public static boolean eq(Integer x, String y) {
        return eq(y, x);
    }

    public static boolean ne(Integer x, String y) {
        return ne(y, x);
    }
}

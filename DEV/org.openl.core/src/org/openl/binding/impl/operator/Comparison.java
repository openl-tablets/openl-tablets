package org.openl.binding.impl.operator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

import org.openl.binding.impl.NumericComparableString;

/**
 * Contains comparison operators for:
 * <ul>
 * <li>== - equals</li>
 * <li>!= - not equals</li>
 * <li>&gt; - great then</li>
 * <li>&lt; - less than</li>
 * <li>&gt;= - great or equals then</li>
 * <li>&lt;= - less or equals than</li>
 * <li>==== - strict equals</li>
 * <li>!=== - strict not equals</li>
 * <li>&gt;== - strict great then</li>
 * <li>&lt;== - strict less than</li>
 * <li>&gt;=== - strict great or equals then</li>
 * <li>&lt;=== - strict less or equals than</li>
 * </ul>
 * The difference between the strict and the not strict is that not strict comparison is usual (more human) comparison,
 * where 10.0 and 10 are equals. In the strict comparison such numbers can be not equals.
 *
 * @author Yury Molchan
 */
public class Comparison {

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal MAX_ULP = new BigDecimal("0.000001");

    // Equals
    public static boolean eq(boolean x, boolean y) {
        return x == y; 
    }
    
    public static boolean eq(byte x, byte y) {
        return x == y;
    }

    public static boolean eq(char x, char y) {
        return x == y;
    }

    public static boolean eq(short x, short y) {
        return x == y;
    }

    public static boolean eq(int x, int y) {
        return x == y;
    }

    public static boolean eq(long x, long y) {
        return x == y;
    }

    public static boolean eq(float x, float y) {
        if (x == y) {
            return true;
        } else if (Float.isInfinite(x) || Float.isInfinite(y) || Float.isNaN(x) || Float.isNaN(y)) {
            return Float.isNaN(x) && Float.isNaN(y);
        }
        return Math.abs(x - y) <= Math.ulp(x);
    }

    public static boolean eq(double x, double y) {
        if (x == y) {
            return true;
        } else if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(x) || Double.isNaN(y)) {
            return Double.isNaN(x) && Double.isNaN(y);
        }
        return Math.abs(x - y) <= Math.ulp(x);
    }

    public static boolean eq(Float x, Float y) {
        return x == null && y == null || x != null && y != null && eq(x.floatValue(), y.floatValue());
    }

    public static boolean eq(Double x, Double y) {
        return x == null && y == null || x != null && y != null && eq(x.doubleValue(), y.doubleValue());
    }

    public static boolean eq(BigDecimal x, BigDecimal y) {
        if (x != null && y != null) {
            BigDecimal xUlp = x.ulp();
            BigDecimal yUlp = y.ulp();
            BigDecimal ulp;
            if (xUlp.compareTo(yUlp) > 0) {
                ulp = xUlp;
            } else {
                ulp = yUlp;
            }
            if (MAX_ULP.compareTo(ulp) < 0) {
                ulp = MAX_ULP;
            }
            ulp = ulp.multiply(HALF);

            return x.subtract(y).abs().compareTo(ulp) < 0;
        } else {
            return x == null && y == null;
        }
    }

    public static <T> boolean eq(T x, T y) {
        return equals(x, y);
    }

    public static <T> boolean eq(T[] x, T[] y) {
        return Arrays.deepEquals(x, y);
    }

    // Not Equals
    public static boolean ne(boolean x, boolean y) {
        return !eq(x, y);
    }

    public static boolean ne(byte x, byte y) {
        return !eq(x, y);
    }

    public static boolean ne(char x, char y) {
        return !eq(x, y);
    }

    public static boolean ne(short x, short y) {
        return !eq(x, y);
    }

    public static boolean ne(int x, int y) {
        return !eq(x, y);
    }

    public static boolean ne(long x, long y) {
        return !eq(x, y);
    }

    public static boolean ne(float x, float y) {
        return !eq(x, y);
    }

    public static boolean ne(double x, double y) {
        return !eq(x, y);
    }

    public static boolean ne(Float x, Float y) {
        return !eq(x, y);
    }

    public static boolean ne(Double x, Double y) {
        return !eq(x, y);
    }

    public static boolean ne(BigDecimal x, BigDecimal y) {
        return !eq(x, y);
    }

    public static <T> boolean ne(T x, T y) {
        return !eq(x, y);
    }

    public static <T> boolean ne(T[] x, T[] y) {
        return !eq(x, y);
    }

    // Greater Than
    public static boolean gt(boolean x, boolean y) {
        return x && !y;
    }

    public static boolean gt(byte x, byte y) {
        return x > y;
    }

    public static boolean gt(char x, char y) {
        return x > y;
    }
    
    public static boolean gt(short x, short y) {
        return x > y;
    }

    public static boolean gt(int x, int y) {
        return x > y;
    }

    public static boolean gt(long x, long y) {
        return x > y;
    }

    public static Boolean gt(float x, float y) {
        if (Float.isNaN(x) || Float.isNaN(y)) {
            return null;
        }
        return x > y && (Float.isInfinite(x) || (x - y) > Math.ulp(x));
    }

    public static Boolean gt(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return x > y && (Double.isInfinite(x) || (x - y) > Math.ulp(x));
    }

    public static Boolean gt(Float x, Float y) {
        Boolean res = null;
        if (x == y) {
            return false;
        } else if (x != null && y != null) {
            res = gt(x.floatValue(), y.floatValue());
        }
        return res;
    }

    public static Boolean gt(Double x, Double y) {
        Boolean res = null;
        if (x == y) {
            return false;
        } else if (x != null && y != null) {
            res = gt(x.doubleValue(), y.doubleValue());
        }
        return res;
    }

    public static <T extends Comparable<T>> Boolean gt(T x, T y) {
        return greatThan(x, y);
    }

    // Less Than
    public static boolean lt(boolean x, boolean y) {
        return gt(y, x);
    }

    public static boolean lt(byte x, byte y) {
        return gt(y, x);
    }

    public static boolean lt(char x, char y) {
        return gt(y, x);
    }

    public static boolean lt(short x, short y) {
        return gt(y, x);
    }

    public static boolean lt(int x, int y) {
        return gt(y, x);
    }

    public static boolean lt(long x, long y) {
        return gt(y, x);
    }

    public static boolean lt(float x, float y) {
        return gt(y, x);
    }

    public static boolean lt(double x, double y) {
        return gt(y, x);
    }

    public static Boolean lt(Float x, Float y) {
        return gt(y, x);
    }

    public static Boolean lt(Double x, Double y) {
        return gt(y, x);
    }

    public static <T extends Comparable<T>> Boolean lt(T x, T y) {
        return gt(y, x);
    }

    // Greater or Equals Than
    public static boolean ge(boolean x, boolean y) {
        return x || !y;
    }

    public static boolean ge(byte x, byte y) {
        return x >= y;
    }

    public static boolean ge(char x, char y) {
        return x >= y;
    }
    
    public static boolean ge(short x, short y) {
        return x >= y;
    }

    public static boolean ge(int x, int y) {
        return x >= y;
    }

    public static boolean ge(long x, long y) {
        return x >= y;
    }

    public static Boolean ge(float x, float y) {
        if (eq(x, y)) {
            return true;
        } else {
            return gt(x, y);
        }
    }

    public static Boolean ge(double x, double y) {
        if (eq(x, y)) {
            return true;
        } else {
            return gt(x, y);
        }
    }

    public static Boolean ge(Float x, Float y) {
        Boolean res = null;
        if (x == null && y == null) {
            res = true;
        } else if (x != null && y != null) {
            res = ge(x.floatValue(), y.floatValue());
        }
        return res;
    }

    public static Boolean ge(Double x, Double y) {
        Boolean res = null;
        if (x == null && y == null) {
            res = true;
        } else if (x != null && y != null) {
            res = ge(x.doubleValue(), y.doubleValue());
        }
        return res;
    }

    public static <T extends Comparable<T>> Boolean ge(T x, T y) {
        return greatOrEquals(x, y);
    }

    public static boolean le(boolean x, boolean y) {
        return ge(y, x);
    }

    public static boolean le(byte x, byte y) {
        return ge(y, x);
    }

    public static boolean le(char x, char y) {
        return ge(y, x);
    }

    public static boolean le(short x, short y) {
        return ge(y, x);
    }

    public static boolean le(int x, int y) {
        return ge(y, x);
    }

    public static boolean le(long x, long y) {
        return ge(y, x);
    }

    public static boolean le(float x, float y) {
        return ge(y, x);
    }

    public static boolean le(double x, double y) {
        return ge(y, x);
    }

    public static Boolean le(Float x, Float y) {
        return ge(y, x);
    }

    public static Boolean le(Double x, Double y) {
        return ge(y, x);
    }

    public static <T extends Comparable<T>> Boolean le(T x, T y) {
        return ge(y, x);
    }

    /* String operators */
    public static boolean string_eq(CharSequence x, CharSequence y) {
        return Objects.equals(x, y);
    }

    public static boolean string_eq(String x, String y) {
        return Objects.equals(x, y);
    }

    public static boolean string_ne(CharSequence x, CharSequence y) {
        return !string_eq(x, y);
    }

    public static boolean string_ne(String x, String y) {
        return !string_eq(x, y);
    }

    public static boolean string_lt(CharSequence x, CharSequence y) {
        if (x == y) {
            return false;
        }
        return x == null || y != null && NumericComparableString.valueOf(x)
            .compareTo(NumericComparableString.valueOf(y)) < 0;
    }

    public static boolean string_lt(String x, String y) {
        if (Objects.equals(x, y)) {
            return false;
        }
        return x == null || y != null && NumericComparableString.valueOf(x)
            .compareTo(NumericComparableString.valueOf(y)) < 0;
    }

    public static boolean string_le(CharSequence x, CharSequence y) {
        if (x == y) {
            return true;
        }
        return x == null || y != null && NumericComparableString.valueOf(x)
            .compareTo(NumericComparableString.valueOf(y)) <= 0;
    }

    public static boolean string_le(String x, String y) {
        if (Objects.equals(x, y)) {
            return true;
        }
        return x == null || y != null && NumericComparableString.valueOf(x)
            .compareTo(NumericComparableString.valueOf(y)) <= 0;
    }

    public static boolean string_ge(CharSequence x, CharSequence y) {
        return string_le(y, x);
    }

    public static boolean string_ge(String x, String y) {
        return string_le(y, x);
    }

    public static boolean string_gt(CharSequence x, CharSequence y) {
        return string_lt(y, x);
    }

    public static boolean string_gt(String x, String y) {
        return string_lt(y, x);
    }

    /* Strict operators */
    public static boolean strict_eq(float x, float y) {
        return x == y;
    }

    public static boolean strict_eq(double x, double y) {
        return x == y;
    }

    public static boolean strict_eq(Float x, Float y) {
        return equals(x, y);
    }

    public static boolean strict_eq(Double x, Double y) {
        return equals(x, y);
    }

    public static boolean strict_eq(BigDecimal x, BigDecimal y) {
        return equals(x, y);
    }

    public static boolean strict_eq(Object x, Object y) {
        return x == y;
    }

    public static boolean strict_ne(float x, float y) {
        return !strict_eq(x, y);
    }

    public static boolean strict_ne(double x, double y) {
        return !strict_eq(x, y);
    }

    public static boolean strict_ne(Float x, Float y) {
        return !strict_eq(x, y);
    }

    public static boolean strict_ne(Double x, Double y) {
        return !strict_eq(x, y);
    }

    public static boolean strict_ne(BigDecimal x, BigDecimal y) {
        return !strict_eq(x, y);
    }

    public static boolean strict_ne(Object x, Object y) {
        return !strict_eq(x, y);
    }

    public static boolean strict_gt(float x, float y) {
        return x > y;
    }

    public static boolean strict_gt(double x, double y) {
        return x > y;
    }

    public static Boolean strict_gt(Float x, Float y) {
        Boolean res = null;
        if (x != null && y != null) {
            res = x > y;
        }
        return res;
    }

    public static Boolean strict_gt(Double x, Double y) {
        Boolean res = null;
        if (x != null && y != null) {
            res = x > y;
        }
        return res;
    }

    public static boolean strict_lt(float x, float y) {
        return strict_gt(y, x);
    }

    public static boolean strict_lt(double x, double y) {
        return strict_gt(y, x);
    }

    public static Boolean strict_lt(Float x, Float y) {
        return strict_gt(y, x);
    }

    public static Boolean strict_lt(Double x, Double y) {
        return strict_gt(y, x);
    }

    public static boolean strict_ge(float x, float y) {
        return x >= y;
    }

    public static boolean strict_ge(double x, double y) {
        return x >= y;
    }

    public static Boolean strict_ge(Float x, Float y) {
        Boolean res = null;
        if (x != null && y != null) {
            res = x >= y;
        }
        return res;
    }

    public static Boolean strict_ge(Double x, Double y) {
        Boolean res = null;
        if (x != null && y != null) {
            res = x >= y;
        }
        return res;
    }

    public static boolean strict_le(float x, float y) {
        return strict_ge(y, x);
    }

    public static boolean strict_le(double x, double y) {
        return strict_ge(y, x);
    }

    public static Boolean strict_le(Float x, Float y) {
        return strict_ge(y, x);
    }

    public static Boolean strict_le(Double x, Double y) {
        return strict_ge(y, x);
    }

    /* Commons */
    private static <T extends Comparable<T>> Boolean greatOrEquals(T x, T y) {
        Boolean res = null;
        if (x == y) {
            res = true;
        } else if (x != null && y != null) {
            res = x.compareTo(y) >= 0;
        }
        return res;
    }

    private static <T extends Comparable<T>> Boolean greatThan(T x, T y) {
        Boolean res = null;
        if (x == y) {
            return false;
        } else if (x != null && y != null) {
            res = x.compareTo(y) > 0;
        }
        return res;
    }

    private static <T> boolean equals(T x, T y) {
        return x == y || x != null && y != null && x.equals(y);
    }

}

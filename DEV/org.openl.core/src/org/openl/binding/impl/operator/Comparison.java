package org.openl.binding.impl.operator;

import java.math.BigDecimal;
import java.math.BigInteger;

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
    // Equals
    public static boolean eq(boolean x, boolean y) {
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
            return false;
        }
        return Math.abs(x - y) <= Math.ulp(x);
    }

    public static boolean eq(double x, double y) {
        if (x == y) {
            return true;
        } else if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(x) || Double.isNaN(y)) {
            return false;
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
        return x == null && y == null || x != null && y != null && x.subtract(y).abs().compareTo(x.ulp()) <= 0;
    }

    public static <T> boolean eq(T x, T y) {
        return equals(x, y);
    }

    // Not Equals
    public static boolean ne(boolean x, boolean y) {
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

    // Greater Than
    public static boolean gt(boolean x, boolean y) {
        return x && !y;
    }

    public static boolean gt(int x, int y) {
        return x > y;
    }

    public static boolean gt(long x, long y) {
        return x > y;
    }

    public static boolean gt(float x, float y) {
        return x > y && (Float.isInfinite(x) || (x - y) > Math.ulp(x));
    }

    public static boolean gt(double x, double y) {
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

    public static boolean ge(int x, int y) {
        return x >= y;
    }

    public static boolean ge(long x, long y) {
        return x >= y;
    }

    public static boolean ge(float x, float y) {
        return eq(x, y) || gt(x, y);
    }

    public static boolean ge(double x, double y) {
        return eq(x, y) || gt(x, y);
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

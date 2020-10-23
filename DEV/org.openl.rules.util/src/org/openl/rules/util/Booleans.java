package org.openl.rules.util;

import java.math.BigInteger;

/**
 * A set of util methods to work with booleans.
 *
 * {@link #and(Boolean[])} and {@link #or(Boolean[])} methods also handle null values:
 * <ul>
 * <li>null & null = null</li>
 * <li>null & true = null</li>
 * <li>null & false = false</li>
 * <li>null | null = null</li>
 * <li>null | true = true</li>
 * <li>null | false = null</li>
 * </ul>
 * where null is unknown value (state).
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Booleans {

    private Booleans() {
        // Utility class
    }

    public static boolean allTrue(boolean[] values) {
        return Boolean.TRUE.equals(and(values));
    }

    public static boolean allTrue(Boolean[] values) {
        return Boolean.TRUE.equals(and(values));
    }

    public static boolean anyTrue(boolean[] values) {
        return Boolean.TRUE.equals(or(values));
    }

    public static boolean anyTrue(Boolean[] values) {
        return Boolean.TRUE.equals(or(values));
    }

    public static boolean allFalse(boolean[] values) {
        return Boolean.FALSE.equals(or(values));
    }

    public static boolean allFalse(Boolean[] values) {
        return Boolean.FALSE.equals(or(values));
    }

    public static boolean anyFalse(boolean[] values) {
        return Boolean.FALSE.equals(and(values));
    }

    public static boolean anyFalse(Boolean[] values) {
        return Boolean.FALSE.equals(and(values));
    }

    /* Synonyms */
    public static boolean allYes(boolean[] values) {
        return allTrue(values);
    }

    public static boolean allYes(Boolean[] values) {
        return allTrue(values);
    }

    public static boolean allNo(boolean[] values) {
        return allFalse(values);
    }

    public static boolean allNo(Boolean[] values) {
        return allFalse(values);
    }

    public static boolean anyYes(boolean[] values) {
        return anyTrue(values);
    }

    public static boolean anyYes(Boolean[] values) {
        return anyTrue(values);
    }

    public static boolean anyNo(boolean[] values) {
        return anyFalse(values);
    }

    public static boolean anyNo(Boolean[] values) {
        return anyFalse(values);
    }

    static Boolean and(boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (boolean value : values) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    static Boolean and(Boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        Boolean result = true;
        for (Boolean value : values) {
            if (value == null) {
                result = null;
            } else if (!value) {
                return false;
            }
        }
        return result;
    }

    static Boolean or(boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (boolean value : values) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    static Boolean or(Boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        Boolean result = false;
        for (Boolean value : values) {
            if (value == null) {
                result = null;
            } else if (value) {
                return true;
            }
        }
        return result;
    }

    /**
     * <p>
     * Converts a Character to a Boolean.
     * </p>
     *
     * <p>
     * {@code '1'}, {@code 'y'} or {@code 'Y'}  will return {@code true}. {@code '0'}, {@code 'n'} or {@code 'N'}
     * will return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   toBoolean('0')     = Boolean.FALSE
     *   toBoolean('1')     = Boolean.TRUE
     *   toBoolean('y')     = Boolean.TRUE // i.e. Y
     *   toBoolean('n')     = Boolean.FALSE // i.e. N
     *   toBoolean(null)    = null
     * </pre>
     *
     * @param ch the Character to check
     * @return the Boolean value of the Character, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(Character ch) {
        if (ch == null) {
            return null;
        }
        return toBoolean(ch.charValue());
    }

    public static Boolean toBoolean(char ch) {
        if (ch == 'y' || ch == 'Y' || ch == '1') {
            return Boolean.TRUE;
        }
        if (ch == 'n' || ch == 'N' || ch == '0') {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * <p>
     * Converts a BigInteger to a Boolean.
     * </p>
     *
     * <p>
     * {@code '1'} will return {@code true}. {@code '0'} will return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   toBoolean(new BigInteger("0"))     = Boolean.FALSE
     *   toBoolean(new BigInteger("1"))     = Boolean.TRUE
     *   toBoolean(null)                    = null
     * </pre>
     *
     * @param i the BigInteger to check
     * @return the Boolean value of the BigInteger, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(BigInteger i) {
        if (i == null) {
            return null;
        }
        if (BigInteger.ZERO.compareTo(i) == 0) {
            return Boolean.FALSE;
        }
        if (BigInteger.ONE.compareTo(i) == 0) {
            return Boolean.TRUE;
        }
        return null;
    }

    /**
     * <p>
     * Converts a byte number to a Boolean.
     * </p>
     *
     * <p>
     * {@code '1'} will return {@code true}. {@code '0'} will return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   toBoolean((byte) 0)     = Boolean.FALSE
     *   toBoolean((byte) 1)     = Boolean.TRUE
     *   toBoolean((byte) 10)    = null
     *   toBoolean(null)         = null
     * </pre>
     *
     * @param b the byte number to check
     * @return the Boolean value of the byte number, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(Byte b) {
        if (b == null) {
            return null;
        }
        return toBoolean(b.byteValue());
    }

    public static Boolean toBoolean(byte b) {
        switch (b) {
            case 0:
                return Boolean.FALSE;
            case 1:
                return Boolean.TRUE;
            default:
                break;
        }
        return null;
    }

    /**
     * <p>
     * Converts a short number to a Boolean.
     * </p>
     *
     * <p>
     * {@code 1} will return {@code true}. {@code 0} will return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   toBoolean((short) 0)     = Boolean.FALSE
     *   toBoolean((short) 1)     = Boolean.TRUE
     *   toBoolean((short) 10)    = null
     *   toBoolean(null)          = null
     * </pre>
     *
     * @param s the short number to check
     * @return the Boolean value of the short number, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(Short s) {
        if (s == null) {
            return null;
        }
        return toBoolean(s.shortValue());
    }

    public static Boolean toBoolean(short s) {
        switch (s) {
            case 0:
                return Boolean.FALSE;
            case 1:
                return Boolean.TRUE;
            default:
                break;
        }
        return null;
    }

    /**
     * <p>
     * Converts a int number to a Boolean.
     * </p>
     *
     * <p>
     * {@code 1} will return {@code true}. {@code 0} will return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   toBoolean(0)       = Boolean.FALSE
     *   toBoolean(1)       = Boolean.TRUE
     *   toBoolean(10)      = null
     *   toBoolean(null)    = null
     * </pre>
     *
     * @param i the int number to check
     * @return the Boolean value of the int number, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(Integer i) {
        if (i == null) {
            return null;
        }
        return toBoolean(i.intValue());
    }

    public static Boolean toBoolean(int i) {
        switch (i) {
            case 0:
                return Boolean.FALSE;
            case 1:
                return Boolean.TRUE;
            default:
                break;
        }
        return null;
    }

    /**
     * <p>
     * Converts a long number to a Boolean.
     * </p>
     *
     * <p>
     * {@code 1} will return {@code true}. {@code 0} will return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   toBoolean(0L)      = Boolean.FALSE
     *   toBoolean(1L)      = Boolean.TRUE
     *   toBoolean(10L)     = null
     *   toBoolean(null)    = null
     * </pre>
     *
     * @param l the long number to check
     * @return the Boolean value of the long number, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(Long l) {
        if (l == null) {
            return null;
        }
        return toBoolean(l.longValue());
    }

    public static Boolean toBoolean(long l) {
        if (l == 0L) {
            return Boolean.FALSE;
        }
        if (l == 1L) {
            return Boolean.TRUE;
        }
        return null;
    }

    /**
     * <p>
     * Converts a String to a Boolean.
     * </p>
     *
     * <p>
     * {@code 'true'}, {@code 'on'}, {@code 'y'}, {@code 't'} or {@code 'yes'} (case insensitive) or {@code '1'}
     * will return {@code true}. <br/>
     * {@code 'false'}, {@code 'off'}, {@code 'n'}, {@code 'f'} or {@code 'no'} (case insensitive) or {@code '0'} will
     * return {@code false}. Otherwise, {@code null} is returned.
     * </p>
     *
     * <pre>
     *   // N.B. case is not significant
     *   toBoolean(null)    = null
     *   toBoolean("true")  = Boolean.TRUE
     *   toBoolean("0")     = Boolean.FALSE
     *   toBoolean("1")     = Boolean.TRUE
     *   toBoolean("T")     = Boolean.TRUE // i.e. T[RUE]
     *   toBoolean("false") = Boolean.FALSE
     *   toBoolean("f")     = Boolean.FALSE // i.e. f[alse]
     *   toBoolean("No")    = Boolean.FALSE
     *   toBoolean("n")     = Boolean.FALSE // i.e. n[o]
     *   toBoolean("on")    = Boolean.TRUE
     *   toBoolean("ON")    = Boolean.TRUE
     *   toBoolean("off")   = Boolean.FALSE
     *   toBoolean("oFf")   = Boolean.FALSE
     *   toBoolean("yes")   = Boolean.TRUE
     *   toBoolean("Y")     = Boolean.TRUE // i.e. Y[ES]
     *   toBoolean("blue")  = null
     *   toBoolean("true ") = null // trailing space (too long)
     *   toBoolean("ono")   = null // does not match on or no
     * </pre>
     *
     * @param str the String to check; upper and lower case are treated as the same
     * @return the Boolean value of the string, {@code null} if no match or {@code null} input
     */
    public static Boolean toBoolean(String str) {
        if (str == null) {
            return null;
        }
        switch (str.length()) {
            case 1: {
                return toBoolean(str.charAt(0));
            }
            case 2: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                if ((ch0 == 'o' || ch0 == 'O')
                    && (ch1 == 'n' || ch1 == 'N')) {
                    return Boolean.TRUE;
                }
                if ((ch0 == 'n' || ch0 == 'N')
                        && (ch1 == 'o' || ch1 == 'O')) {
                    return Boolean.FALSE;
                }
                break;
            }
            case 3: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                if ((ch0 == 'y' || ch0 == 'Y')
                        && (ch1 == 'e' || ch1 == 'E')
                        && (ch2 == 's' || ch2 == 'S')) {
                    return Boolean.TRUE;
                }
                if ((ch0 == 'o' || ch0 == 'O')
                        && (ch1 == 'f' || ch1 == 'F')
                        && (ch2 == 'f' || ch2 == 'F')) {
                    return Boolean.FALSE;
                }
                break;
            }
            case 4: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                final char ch3 = str.charAt(3);
                if ((ch0 == 't' || ch0 == 'T')
                        && (ch1 == 'r' || ch1 == 'R')
                        && (ch2 == 'u' || ch2 == 'U')
                        && (ch3 == 'e' || ch3 == 'E')) {
                    return Boolean.TRUE;
                }
                break;
            }
            case 5: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                final char ch3 = str.charAt(3);
                final char ch4 = str.charAt(4);
                if ((ch0 == 'f' || ch0 == 'F')
                        && (ch1 == 'a' || ch1 == 'A')
                        && (ch2 == 'l' || ch2 == 'L')
                        && (ch3 == 's' || ch3 == 'S')
                        && (ch4 == 'e' || ch4 == 'E')) {
                    return Boolean.FALSE;
                }
                break;
            }
            default:
                break;
        }
        return null;
    }
}

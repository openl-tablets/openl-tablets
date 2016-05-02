package org.openl.util;

/**
 * An utility for manipulating with booleans:
 * - converting an object to a boolean value
 * - logic operation with a boolean array (and, or, xor)
 *
 * @author Yury Molchan
 */
public class BooleanUtils {

    public static final String NULL_ARRAY_MSG = "The Array must not be null";
    public static final String EMPTY_ARRAY_MSG = "Array is empty";
    public static final String NULL_VALUE_IN_ARRAY_MSG = "Array shouldn`t contain null objects";

    /**
     * Converts an Object to a boolean. For String value 'true', 'on', 'yes',
     * 'y' or 't' (case insensitive) will return true. Otherwise, false is
     * returned. For Integer value 0 will return false. Otherwise, true is
     * returned.
     * 
     * @param value Object value
     * @return boolean value
     */
    public static boolean toBoolean(Object value) {
        Boolean bValue = toBooleanObject(value);
        if (bValue == null) {
            return false;
        }
        return bValue;
    }

    /**
     * Converts an Object to a boolean. For String value 'true', 'on', 'yes',
     * 'y' or 't' (case insensitive) will return true. Otherwise, false is
     * returned. For Integer value 0 will return false. Otherwise, true is
     * returned. Returns boolean value or default value if the input value is
     * null or not a Boolean.
     * 
     * @param value Object value
     * @param defaultValue The default boolean value to return if the value is
     *            null or not a Boolean
     * @return boolean value
     */
    public static boolean toBoolean(Object value, boolean defaultValue) {
        return toBooleanObject(value, defaultValue);
    }

    /**
     * Converts an Object to a Boolean. For String value 'true', 'on', 'yes',
     * 'y' or 't' (case insensitive) will return true. 'false', 'off', 'no', 'n'
     * or 'f' (case insensitive) will return false. Otherwise, null is returned.
     * For Integer value 0 will return false, null will return null. Otherwise,
     * true is returned.
     * 
     * @param value Object value
     * @return Boolean value
     */
    public static Boolean toBooleanObject(Object value) {
        Boolean bValue = null;
        if (value instanceof String) {
            bValue = toBooleanObject((String) value);
        } else if (value instanceof Integer) {
            bValue = (Integer) value != 0;
        } else if (value instanceof Boolean) {
            bValue = (Boolean) value;
        }
        return bValue;
    }

    /**
     * <p>Converts a String to a Boolean.</p>
     *
     * <p>{@code 'true'}, {@code 'on'}, {@code 'y'}, {@code 't'} or {@code 'yes'}
     * (case insensitive) will return {@code true}.
     * {@code 'false'}, {@code 'off'}, {@code 'n'}, {@code 'f'} or {@code 'no'}
     * (case insensitive) will return {@code false}.
     * Otherwise, {@code null} is returned.</p>
     *
     * <p>NOTE: This returns null and will throw a NullPointerException if autoboxed to a boolean. </p>
     *
     * <pre>
     *   // N.B. case is not significant
     *   BooleanUtils.toBooleanObject(null)    = null
     *   BooleanUtils.toBooleanObject("true")  = Boolean.TRUE
     *   BooleanUtils.toBooleanObject("T")     = Boolean.TRUE // i.e. T[RUE]
     *   BooleanUtils.toBooleanObject("false") = Boolean.FALSE
     *   BooleanUtils.toBooleanObject("f")     = Boolean.FALSE // i.e. f[alse]
     *   BooleanUtils.toBooleanObject("No")    = Boolean.FALSE
     *   BooleanUtils.toBooleanObject("n")     = Boolean.FALSE // i.e. n[o]
     *   BooleanUtils.toBooleanObject("on")    = Boolean.TRUE
     *   BooleanUtils.toBooleanObject("ON")    = Boolean.TRUE
     *   BooleanUtils.toBooleanObject("off")   = Boolean.FALSE
     *   BooleanUtils.toBooleanObject("oFf")   = Boolean.FALSE
     *   BooleanUtils.toBooleanObject("yes")   = Boolean.TRUE
     *   BooleanUtils.toBooleanObject("Y")     = Boolean.TRUE // i.e. Y[ES]
     *   BooleanUtils.toBooleanObject("blue")  = null
     *   BooleanUtils.toBooleanObject("true ") = null // trailing space (too long)
     *   BooleanUtils.toBooleanObject("ono")   = null // does not match on or no
     * </pre>
     *
     * @param str  the String to check; upper and lower case are treated as the same
     * @return the Boolean value of the string, {@code null} if no match or {@code null} input
     */
    public static Boolean toBooleanObject(final String str) {
        // Previously used equalsIgnoreCase, which was fast for interned 'true'.
        // Non interned 'true' matched 15 times slower.
        //
        // Optimisation provides same performance as before for interned 'true'.
        // Similar performance for null, 'false', and other strings not length 2/3/4.
        // 'true'/'TRUE' match 4 times slower, 'tRUE'/'True' 7 times slower.
        if (str == "true") {
            return Boolean.TRUE;
        }
        if (str == null) {
            return null;
        }
        switch (str.length()) {
            case 1: {
                final char ch0 = str.charAt(0);
                if (ch0 == 'y' || ch0 == 'Y' ||
                        ch0 == 't' || ch0 == 'T') {
                    return Boolean.TRUE;
                }
                if (ch0 == 'n' || ch0 == 'N' ||
                        ch0 == 'f' || ch0 == 'F') {
                    return Boolean.FALSE;
                }
                break;
            }
            case 2: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                if ((ch0 == 'o' || ch0 == 'O') &&
                        (ch1 == 'n' || ch1 == 'N') ) {
                    return Boolean.TRUE;
                }
                if ((ch0 == 'n' || ch0 == 'N') &&
                        (ch1 == 'o' || ch1 == 'O') ) {
                    return Boolean.FALSE;
                }
                break;
            }
            case 3: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                if ((ch0 == 'y' || ch0 == 'Y') &&
                        (ch1 == 'e' || ch1 == 'E') &&
                        (ch2 == 's' || ch2 == 'S') ) {
                    return Boolean.TRUE;
                }
                if ((ch0 == 'o' || ch0 == 'O') &&
                        (ch1 == 'f' || ch1 == 'F') &&
                        (ch2 == 'f' || ch2 == 'F') ) {
                    return Boolean.FALSE;
                }
                break;
            }
            case 4: {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                final char ch3 = str.charAt(3);
                if ((ch0 == 't' || ch0 == 'T') &&
                        (ch1 == 'r' || ch1 == 'R') &&
                        (ch2 == 'u' || ch2 == 'U') &&
                        (ch3 == 'e' || ch3 == 'E') ) {
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
                if ((ch0 == 'f' || ch0 == 'F') &&
                        (ch1 == 'a' || ch1 == 'A') &&
                        (ch2 == 'l' || ch2 == 'L') &&
                        (ch3 == 's' || ch3 == 'S') &&
                        (ch4 == 'e' || ch4 == 'E') ) {
                    return Boolean.FALSE;
                }
                break;
            }
            default:
                break;
        }

        return null;
    }

    /**
     * Converts an Object to a Boolean. For String value 'true', 'on', 'yes',
     * 'y' or 't' (case insensitive) will return true. 'false', 'off', 'no', 'n'
     * or 'f' (case insensitive) will return false. Otherwise, null is returned.
     * For Integer value 0 will return false, null will return null. Otherwise,
     * true is returned. Returns Boolean value or default value if the input
     * value is null or not a Boolean.
     * 
     * @param value Object value
     * @param defaultValue The default Boolean value to return if the value is
     *            null or not a Boolean
     * @return Boolean value
     */
    public static Boolean toBooleanObject(Object value, Boolean defaultValue) {
        Boolean bValue = toBooleanObject(value);
        return bValue == null ? defaultValue : bValue;
    }

    /**
     * Returns true if all elements are true otherwise false.
     */
    public static boolean and(boolean[] values) {
        if (values == null) {
            return false;
        }
        for (boolean value : values) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if all elements are true otherwise false.
     */
    public static boolean and(Boolean[] values) {
        if (values == null) {
            return false;
        }
        if (ArrayTool.contains(values, null)) {
            return false;
        }
        for (Boolean value : values) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if odd number of elements are true otherwise false.
     *
     * @exception java.lang.IllegalArgumentException for null or an empty array.
     */
    public static boolean xor(boolean[] values) {
        checkOnEmptyArray(values);
        boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result ^ values[i];
        }
        return result;
    }

    /**
     * Returns true if odd number of elements are true otherwise false.
     *
     * @exception java.lang.IllegalArgumentException for null or an empty array or null value in the array.
     */
    public static boolean xor(Boolean[] values) {
        checkOnEmptyArray(values);
        Boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result ^ values[i];
        }
        return result;
    }

    /**
     * Returns true if at least one of elements are true otherwise false.
     *
     * @exception java.lang.IllegalArgumentException for null or an empty array.
     */
    public static boolean or(boolean[] values) {
        checkOnEmptyArray(values);
        boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result || values[i];
        }
        return result;
    }

    /**
     * Returns true if at least one of elements are true otherwise false.
     *
     * @exception java.lang.IllegalArgumentException for null or an empty array or null value in the array.
     */
    public static boolean or(Boolean[] values) {
        checkOnEmptyArray(values);
        Boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result || values[i];
        }
        return result;
    }

    private static void checkOnEmptyArray(Boolean[] values) {
        if (values == null) {
            throw new IllegalArgumentException(NULL_ARRAY_MSG);
        }
        if (values.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MSG);
        }
        if (ArrayTool.contains(values, null)) {
            throw new IllegalArgumentException(NULL_VALUE_IN_ARRAY_MSG);
        }
    }

    private static void checkOnEmptyArray(boolean[] values) {
        if (values == null) {
            throw new IllegalArgumentException(NULL_ARRAY_MSG);
        }
        if (values.length == 0) {
            throw new IllegalArgumentException(EMPTY_ARRAY_MSG);
        }
    }
}

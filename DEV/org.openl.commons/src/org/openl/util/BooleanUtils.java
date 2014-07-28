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
            bValue = org.apache.commons.lang3.BooleanUtils.toBooleanObject((String) value);
        } else if (value instanceof Integer) {
            bValue = org.apache.commons.lang3.BooleanUtils.toBoolean((Integer) value);
        } else if (value instanceof Boolean) {
            bValue = (Boolean) value;
        }
        return bValue;
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

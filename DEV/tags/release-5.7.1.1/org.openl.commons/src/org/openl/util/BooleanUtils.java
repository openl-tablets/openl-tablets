package org.openl.util;

public class BooleanUtils {

    /**
     * Converts an Object to a boolean.
     * For String value 'true', 'on' or 'yes' (case insensitive) will return true. Otherwise, false is returned.
     * For Integer value 0 will return false. Otherwise, true is returned.
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
     * Converts an Object to a Boolean.
     * For String value 'true', 'on' or 'yes'  (case insensitive) will return true.
     * 'false', 'off' or 'no'  (case insensitive) will return false. Otherwise, null is returned.
     * For Integer value 0 will return false, null will return null. Otherwise, true is returned.
     *
     * @param value Object value
     * @return Boolean value
     */
    public static Boolean toBooleanObject(Object value) {
        Boolean bValue = null;
        if (value instanceof String) {
            bValue = org.apache.commons.lang.BooleanUtils.toBooleanObject((String) value);
        } else if (value instanceof Integer) {
            bValue = org.apache.commons.lang.BooleanUtils.toBoolean((Integer) value);
        } else if (value instanceof Boolean) {
            bValue = (Boolean) value;
        }
        return bValue;
    }

}

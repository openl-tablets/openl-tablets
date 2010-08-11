package org.openl.util;

import org.apache.commons.lang.ArrayUtils;

public class BooleanUtils {
    
    // additional for Openl values that represents boolean values
    private static String[] additionalTrueValues = new String[] {"y", "t"};    
    private static String[] additionalFalseValues = new String[] {"n", "f"};

    /**
     * Converts an Object to a boolean.
     * For String value 'true', 'on', 'yes', 'y' or 't' (case insensitive) will return true. Otherwise, false is returned.
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
     * For String value 'true', 'on', 'yes', 'y' or 't'  (case insensitive) will return true.
     * 'false', 'off', 'no', 'n' or 'f'  (case insensitive) will return false. Otherwise, null is returned.
     * For Integer value 0 will return false, null will return null. Otherwise, true is returned.
     *
     * @param value Object value
     * @return Boolean value
     */
    public static Boolean toBooleanObject(Object value) {
        Boolean bValue = null;
        if (value instanceof String) {
            if (isAdditionalValue((String)value)) {
                bValue = getBooleanFromAdditionalValue((String)value);
            } else {
                bValue = org.apache.commons.lang.BooleanUtils.toBooleanObject((String) value);
            }           
        } else if (value instanceof Integer) {
            bValue = org.apache.commons.lang.BooleanUtils.toBoolean((Integer) value);
        } else if (value instanceof Boolean) {
            bValue = (Boolean) value;
        }
        return bValue;
    }
    
    private static Boolean getBooleanFromAdditionalValue(String value) {
        Boolean result = null;
        String lcase = ((String)value).toLowerCase().intern();
        if (ArrayUtils.contains(additionalTrueValues, lcase)) {            
            result = Boolean.TRUE;
        } 
        if (ArrayUtils.contains(additionalFalseValues, lcase)) {
            result = Boolean.FALSE;
        }
        return result;
    }
    
    /**
     * Checks if value is represented as a special for Openl boolean symbols. 
     * 
     * @param value string representation of the value that is considered to be a boolean one
     * @return true is value is a special for Openl boolean value
     */
    private static boolean isAdditionalValue(String value) {
        String lcaseValue = ((String)value).toLowerCase().intern();
        if (ArrayUtils.contains(additionalTrueValues, lcaseValue) || ArrayUtils.contains(additionalFalseValues, lcaseValue)) {
            return true;
        }
        return false;
    }

}

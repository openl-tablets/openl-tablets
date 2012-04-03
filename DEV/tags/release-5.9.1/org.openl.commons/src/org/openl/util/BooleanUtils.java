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
     * Converts an Object to a boolean.
     * For String value 'true', 'on', 'yes', 'y' or 't' (case insensitive) will return true. Otherwise, false is returned.
     * For Integer value 0 will return false. Otherwise, true is returned.
     * Returns boolean value or default value if the input value is null or not a Boolean.
     *
     * @param value Object value
     * @param defaultValue The default boolean value to return if the value is null or not a Boolean
     * @return boolean value
     */
    public static boolean toBoolean(Object value, boolean defaultValue) {
        return toBooleanObject(value, defaultValue);
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

    /**
     * Converts an Object to a Boolean.
     * For String value 'true', 'on', 'yes', 'y' or 't'  (case insensitive) will return true.
     * 'false', 'off', 'no', 'n' or 'f'  (case insensitive) will return false. Otherwise, null is returned.
     * For Integer value 0 will return false, null will return null. Otherwise, true is returned.
     * Returns Boolean value or default value if the input value is null or not a Boolean.
     *
     * @param value Object value
     * @param defaultValue The default Boolean value to return if the value is null or not a Boolean
     * @return Boolean value
     */
    public static Boolean toBooleanObject(Object value, Boolean defaultValue) {
        Boolean bValue = toBooleanObject(value);
        return bValue == null ? defaultValue : bValue;
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
    
    // logical AND
    /**
     * returns true if all elements are true
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
    
    // Exclusive or
    public static boolean xor(boolean[] values) {
        if(values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }            
        if(values.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }            
        boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result ^ values[i];
        }
        return result;
    }
    
    public static boolean xor(Boolean[] values) {
        if(values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }            
        if(values.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }         
        if (ArrayTool.contains(values, null)) {
            throw new IllegalArgumentException("Array shouldn`t contain null objects");
        }
        Boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result ^ values[i];
        }
        return result;
    }
    
    // or function
    public static boolean or(boolean[] values) {
        if(values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }            
        if(values.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }            
        boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result || values[i];
        }
        return result;
    }
    
    public static boolean or(Boolean[] values) {
        if(values == null) {
            throw new IllegalArgumentException("The Array must not be null");
        }            
        if(values.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        } 
        if (ArrayTool.contains(values, null)) {
            throw new IllegalArgumentException("Array shouldn`t contain null objects");
        }        
        Boolean result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result || values[i];
        }
        return result;
    }

}

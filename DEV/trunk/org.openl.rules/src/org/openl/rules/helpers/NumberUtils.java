package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.openl.meta.BigDecimalValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;

public class NumberUtils {

    public static boolean isFloatPointNumber(Object object) {

        if (object != null) {
            return isFloatPointType(object.getClass());
        }

        return false;
    }

    public static boolean isFloatPointType(Class<?> clazz) {
        if (float.class.equals(clazz)
                || double.class.equals(clazz)
                || Float.class.equals(clazz)
                || FloatValue.class.isAssignableFrom(clazz)
                || Double.class.equals(clazz)
                || DoubleValue.class.isAssignableFrom(clazz)
                || BigDecimal.class.equals(clazz)
                || BigDecimalValue.class.equals(clazz)) {
            return true;
        }
        return false;
    }

    public static Double convertToDouble(Object object) {

        if (float.class.equals(object.getClass())
                || Float.class.equals(object.getClass())) {
            return Double.valueOf(((Float) object).doubleValue());
        }
        
        if (FloatValue.class.isAssignableFrom(object.getClass())) {
            return Double.valueOf(((FloatValue) object).doubleValue());
        }

        if (double.class.equals(object.getClass())
                || Double.class.equals(object.getClass())) {
            return (Double) object;
        }

        if (DoubleValue.class.isAssignableFrom(object.getClass())) {
            return ((DoubleValue) object).doubleValue();
        }

        if (BigDecimal.class.equals(object.getClass())) {
            return ((BigDecimal) object).doubleValue();
        }
        
        if (BigDecimalValue.class.equals(object.getClass())) {
            return ((BigDecimalValue) object).doubleValue();
        }

        return null;
    }

    public static Double roundValue(Double value, int scale) {

        if (value != null) {
            BigDecimal roundedValue = new BigDecimal(value);
            roundedValue = roundedValue.setScale(scale, RoundingMode.HALF_UP);

            return roundedValue.doubleValue();
        }

        return null;
    }
    
    /**
     * Gets the scale of the double value.
     * 
     * @param value to get the scale
     * @return number of values after the comma
     * 
     * @throws {@link NullPointerException} if the income is <code>null</code>
     */
	public static int getScale(Double value) {
		if (value == null) {
			throw new NullPointerException("Null value is not supported");
		}
		
 		if (!value.equals(Double.NaN) && !value.equals(Double.NEGATIVE_INFINITY)) {
			BigDecimal decimal = BigDecimal.valueOf(value);

			return decimal.scale();
		}
		return 0;
	}
    
	/**
	 * Gets the scale of the income value
	 * 
	 * @param value
	 * @return number of values after the comma
	 * 
	 * @throws {@link NullPointerException} if the income is <code>null</code>
	 */
    public static int getScale(Number value) {
    	if (value == null) {
			throw new NullPointerException("Null value is not supported");
		}
    	
    	if (value instanceof Double) {
    		return getScale((Double)value);
    	}
    	
    	BigDecimal decimal = new BigDecimal(String.valueOf(value));

        return decimal.scale();
    }
    
    public static Class<?> getNumericPrimitive(Class<?> wrapperClass) {
        if (Byte.class.equals(wrapperClass)) {
            return byte.class;
        } else if (Short.class.equals(wrapperClass)) {
            return short.class;
        } else if (Integer.class.equals(wrapperClass)) {
            return int.class;
        } else if (Long.class.equals(wrapperClass)) {
            return long.class;
        } else if (Float.class.equals(wrapperClass)) {
            return float.class;
        } else if (Double.class.equals(wrapperClass)) {
            return double.class;
        }
        return null;
    }
    
}

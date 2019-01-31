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
        if (clazz != null) {
            if (float.class.equals(clazz) || double.class.equals(clazz) || Float.class.equals(clazz) || FloatValue.class
                .isAssignableFrom(clazz) || Double.class.equals(clazz) || DoubleValue.class
                    .isAssignableFrom(clazz) || BigDecimal.class.equals(clazz) || BigDecimalValue.class.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static Double convertToDouble(Object object) {

        if (Float.class.equals(object.getClass())) {
            return Double.valueOf(object.toString());
        }
        
        if (FloatValue.class.isAssignableFrom(object.getClass())) {
            return Double.valueOf(object.toString());
        }

        if  (Double.class.equals(object.getClass())) {
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

    public static DoubleValue convertToDoubleValue(Object object) {
        if (FloatValue.class.isAssignableFrom(object.getClass())) {
            return FloatValue.autocast((FloatValue) object, (DoubleValue) null);
        }
        if (DoubleValue.class.isAssignableFrom(object.getClass())) {
            return (DoubleValue) object;
        }

        if (BigDecimalValue.class.isAssignableFrom(object.getClass())) {
            return BigDecimalValue.cast((BigDecimalValue) object, (DoubleValue) null);
        }
        
        return new DoubleValue(convertToDouble(object));
    }

    public static Double roundValue(Double value, int scale) {

        if (value != null) {
            if (value.isInfinite() || value.isNaN()) {
                return value;
            }
            BigDecimal roundedValue = BigDecimal.valueOf(value);
            roundedValue = roundedValue.setScale(scale, RoundingMode.HALF_UP);

            return roundedValue.doubleValue();
        }

        return null;
    }
    
	/**
	 * Gets the scale of the income value.
	 * Note that if the value will be of type {@link Float} or {@link FloatValue}, the scale will be 
	 * defined via value.doubleValue() method call.
	 * And the scale will differ from the income.
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
    	
    	if (value instanceof BigDecimal) {
    		/**
    		 * If BigDecimal the scale can be taken directly
    		 */
    		return ((BigDecimal)value).scale();
    	}
    	
    	if (value instanceof BigDecimalValue) {
    		/**
    		 * If BigDecimalValue the scale can be taken directly
    		 */
    		return ((BigDecimalValue)value).getValue().scale();
    	}
    	
    	if (isFloatPointNumber(value)) {
    		/**
    		 * Process as float point value
    		 */
    		return getScale(convertToDouble(value).doubleValue()); 
    	} else {
    		/**
    		 * Process as integer value
    		 */
    		return BigDecimal.valueOf(value.longValue()).scale();
    	}
    }
    
    public static int getScale(double value) {
    	if (!Double.isNaN(value) && !Double.isInfinite(value)) {
    		BigDecimal decimal = BigDecimal.valueOf(value);

			return decimal.scale();
    	}
    	return 0;
    }
    
    public static int getScale(float value) {
        return getScale(Double.valueOf(Float.toString(value)));
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

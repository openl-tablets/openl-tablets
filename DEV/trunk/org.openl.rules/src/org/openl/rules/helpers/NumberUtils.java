package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.openl.meta.DoubleValue;

public class NumberUtils {

	public static boolean isFloatPointNumber(Object object) {

		if (object != null) {
			if (float.class.equals(object.getClass())
					|| double.class.equals(object.getClass())
					|| Float.class.equals(object.getClass())
					|| Double.class.equals(object.getClass())
					|| DoubleValue.class.isAssignableFrom(object.getClass())
					|| BigDecimal.class.equals(object.getClass())) {
				return true;
			}
		}

		return false;
	}

	public static Double convertToDouble(Object object) {

		if (float.class.equals(object.getClass())
				|| Float.class.equals(object.getClass())) {
			return Double.valueOf(((Float) object).doubleValue());
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

	public static int getScale(Double value) {

		BigDecimal decimal = BigDecimal.valueOf(value);

		return decimal.scale();
	}
}

package org.openl.util.math;

import java.math.BigDecimal;

public class MathUtils {

	public static boolean eq(float x, float y) {
		return Math.abs(x - y) <= Math.ulp(x);
	}

	public static boolean ne(float x, float y) {
		return !eq(x, y);
	}

	public static boolean gt(float x, float y) {
		return Math.abs(x - y) > Math.ulp(x) && x > y;
	}

	public static boolean ge(float x, float y) {
		return eq(x, y) || gt(x, y);
	}

	public static boolean lt(float x, float y) {
		return Math.abs(x - y) > Math.ulp(x) && x < y;
	}

	public static boolean le(float x, float y) {
		return eq(x, y) || lt(x, y);
	}

	public static boolean eq(double x, double y) {
		return Math.abs(x - y) <= Math.ulp(x);
	}

	public static boolean ne(double x, double y) {
		return !eq(x, y);
	}

	public static boolean gt(double x, double y) {
		return Math.abs(x - y) > Math.ulp(x) && x > y;
	}

	public static boolean ge(double x, double y) {
		return eq(x, y) || gt(x, y);
	}

	public static boolean lt(double x, double y) {
		return Math.abs(x - y) > Math.ulp(x) && x < y;
	}

	public static boolean le(double x, double y) {
		return eq(x, y) || lt(x, y);
	}
	
	public static boolean eq(BigDecimal x, BigDecimal y) {
        return x.subtract(y).abs().compareTo(x.ulp()) <= 0 ;
    }

}

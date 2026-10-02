package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class MathsTest {

    @Test
    void absOfWholeNumbers() {
        assertEquals(5, Maths.abs(-5));
        assertEquals(5, Maths.abs(5));
        assertEquals(0, Maths.abs(0));
        assertEquals(Integer.MIN_VALUE, Maths.abs(Integer.MIN_VALUE));
        assertEquals(5L, Maths.abs(-5L));
        assertEquals(0L, Maths.abs(0L));
        assertEquals(Long.MIN_VALUE, Maths.abs(Long.MIN_VALUE));
    }

    @Test
    void absOfFractions() {
        assertEquals(5.5, Maths.abs(-5.5));
        assertEquals(5.9, Maths.abs(-5.9));
        assertEquals(5.5, Maths.abs(5.5));
        assertEquals(0.0, Maths.abs(-0.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.abs(Double.NEGATIVE_INFINITY));
        assertEquals(Double.NaN, Maths.abs(Double.NaN));
        assertEquals(5.5f, Maths.abs(-5.5f));
        assertEquals(5.9f, Maths.abs(-5.9f));
        assertEquals(0.0f, Maths.abs(-0.0f));
    }

    @Test
    void absOfBigNumbersIsExact() {
        assertEquals(new BigInteger("85070591730234615847396907784232501249"),
                Maths.abs(new BigInteger("-85070591730234615847396907784232501249")));
        assertEquals(new BigInteger("9223372036854775808"), Maths.abs(BigInteger.valueOf(Long.MIN_VALUE)));
        assertEquals(BigInteger.TWO, Maths.abs(BigInteger.TWO));
        assertEquals(new BigDecimal("12345678901234567890.123456789"),
                Maths.abs(new BigDecimal("-12345678901234567890.123456789")));
        assertEquals(new BigDecimal("2.50"), Maths.abs(new BigDecimal("-2.50")));
        assertEquals(new BigDecimal("0.1"), Maths.abs(new BigDecimal("0.1")));
    }

    @Test
    void inverseTrigonometry() {
        assertEquals(1.5707963267948966, Maths.acos(0.0));
        assertEquals(0.0, Maths.acos(1.0));
        assertEquals(1.0471975511965979, Maths.acos(0.5));
        assertEquals(2.0943951023931957, Maths.acos(-0.5));
        assertEquals(Math.PI, Maths.acos(-1.0));
        assertEquals(Double.NaN, Maths.acos(-2.0));
        assertEquals(1.5707963267948966, Maths.asin(1.0));
        assertEquals(0.5235987755982989, Maths.asin(0.5));
        assertEquals(-0.0, Maths.asin(-0.0));
        assertEquals(-1.5707963267948966, Maths.asin(-1.0));
        assertEquals(Double.NaN, Maths.asin(2.0));
        assertEquals(0.7853981633974483, Maths.atan(1.0));
        assertEquals(0.4636476090008061, Maths.atan(0.5));
        assertEquals(-0.0, Maths.atan(-0.0));
        assertEquals(-0.7853981633974483, Maths.atan(-1.0));
    }

    @Test
    void angleOfPoint() {
        assertEquals(0.0, Maths.atan2(0.0, 0.0));
        assertEquals(0.0, Maths.atan2(0.0, 1.0));
        assertEquals(1.5707963267948966, Maths.atan2(1.0, 0.0));
        assertEquals(-1.5707963267948966, Maths.atan2(-1.0, 0.0));
        assertEquals(1.5707963267948966, Maths.atan2(1.0, -0.0));
        assertEquals(-0.0, Maths.atan2(-0.0, 1.0));
        assertEquals(Math.PI, Maths.atan2(0.0, -1.0));
        assertEquals(-Math.PI, Maths.atan2(-0.0, -1.0));
    }

    @Test
    void trigonometry() {
        assertEquals(1.0, Maths.sin(Math.PI / 2));
        assertEquals(-0.0, Maths.sin(-0.0));
        assertEquals(Double.NaN, Maths.sin(Double.POSITIVE_INFINITY));
        assertEquals(1.0, Maths.cos(0.0));
        assertEquals(Math.cos(-0.5), Maths.cos(-0.5));
        assertEquals(Double.NaN, Maths.cos(Double.NaN));
        assertEquals(1.0, Maths.tan(Math.PI / 4), 1e-15);
        assertEquals(-0.0, Maths.tan(-0.0));
        assertEquals(0.0, Maths.sinh(0.0));
        assertEquals(Double.NEGATIVE_INFINITY, Maths.sinh(Double.NEGATIVE_INFINITY));
        assertEquals(1.0, Maths.cosh(0.0));
        assertEquals(Math.cosh(1.0), Maths.cosh(1.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.cosh(Double.NEGATIVE_INFINITY));
        assertEquals(-1.0, Maths.tanh(Double.NEGATIVE_INFINITY));
        assertEquals(-0.0, Maths.tanh(-0.0));
    }

    @Test
    void angleConversion() {
        assertEquals(0.0, Maths.toDegrees(0.0));
        assertEquals(572.9577951308232, Maths.toDegrees(10.0));
        assertEquals(180.0, Maths.toDegrees(Math.PI));
        assertEquals(0.0, Maths.toRadians(0.0));
        assertEquals(0.17453292519943295, Maths.toRadians(10.0));
        assertEquals(Math.PI, Maths.toRadians(180.0), 1e-15);
    }

    @Test
    void roots() {
        assertEquals(4.0, Maths.sqrt(16.0));
        assertEquals(0.1, Maths.sqrt(0.01));
        assertEquals(-0.0, Maths.sqrt(-0.0));
        assertEquals(Double.NaN, Maths.sqrt(-1.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.sqrt(Double.POSITIVE_INFINITY));
        assertEquals(0.0, Maths.cbrt(0.0));
        assertEquals(1.2599210498948732, Maths.cbrt(2.0));
        assertEquals(-3.0, Maths.cbrt(-27.0), 1e-15);
        assertEquals(Double.NEGATIVE_INFINITY, Maths.cbrt(Double.NEGATIVE_INFINITY));
    }

    @Test
    void powers() {
        assertEquals(1024.0, Maths.pow(2.0, 10.0));
        assertEquals(0.5, Maths.pow(2.0, -1.0));
        assertEquals(3.0, Maths.pow(9.0, 0.5), 1e-15);
        assertEquals(-8.0, Maths.pow(-2.0, 3.0));
        assertEquals(1.0, Maths.pow(0.0, 0.0));
        assertEquals(1.0, Maths.pow(Double.NaN, 0.0));
        assertEquals(Double.NaN, Maths.pow(-8.0, 1.0 / 3));
        assertEquals(Double.POSITIVE_INFINITY, Maths.pow(0.0, -1.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.pow(10.0, 309.0));
    }

    @Test
    void exponentials() {
        assertEquals(Math.exp(1.0), Maths.exp(1.0));
        assertEquals(Math.exp(111), Maths.exp(111.0));
        assertEquals(0.0, Maths.exp(-1000.0));
        assertEquals(0.0, Maths.exp(Double.NEGATIVE_INFINITY));
        assertEquals(0.0, Maths.expm1(0.0));
        assertEquals(Math.expm1(111), Maths.expm1(111.0));
        assertEquals(-1.0, Maths.expm1(-180.0));
    }

    @Test
    void logarithms() {
        assertEquals(1.0, Maths.log(Math.E));
        assertEquals(Double.NEGATIVE_INFINITY, Maths.log(0.0));
        assertEquals(Double.NaN, Maths.log(-1.0));
        assertEquals(3.0, Maths.log10(1000.0));
        assertEquals(-2.0, Maths.log10(0.01), 1e-15);
        assertEquals(Double.NEGATIVE_INFINITY, Maths.log10(-0.0));
        assertEquals(0.0, Maths.log1p(0.0));
        assertEquals(-0.0, Maths.log1p(-0.0));
        assertEquals(Double.NEGATIVE_INFINITY, Maths.log1p(-1.0));
        assertEquals(Double.NaN, Maths.log1p(-2.0));
    }

    @Test
    void wholeParts() {
        assertEquals(0.0, Maths.ceil(0.0));
        assertEquals(2.0, Maths.ceil(1.3));
        assertEquals(-0.0, Maths.ceil(-0.8));
        assertEquals(-1.0, Maths.ceil(-1.3));
        assertEquals(100.0, Maths.ceil(99.99));
        assertEquals(1.0, Maths.floor(1.3));
        assertEquals(-3.0, Maths.floor(-2.1));
        assertEquals(-0.0, Maths.floor(-0.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.floor(Double.POSITIVE_INFINITY));
        assertEquals(2.0, Maths.rint(2.5));
        assertEquals(4.0, Maths.rint(3.5));
        assertEquals(-2.0, Maths.rint(-1.5));
    }

    @Test
    void sign() {
        assertEquals(-1.0, Maths.signum(-3.2));
        assertEquals(1.0, Maths.signum(0.01));
        assertEquals(-0.0, Maths.signum(-0.0));
        assertEquals(Double.NaN, Maths.signum(Double.NaN));
        assertEquals(0.0, Maths.copySign(0.0, 1.0));
        assertEquals(1.0, Maths.copySign(1.0, 2.0));
        assertEquals(2.4, Maths.copySign(-2.4, 0.2));
        assertEquals(-2.4, Maths.copySign(2.4, -0.2));
        assertEquals(-0.0, Maths.copySign(0.0, -1.125));
        assertEquals(1.0f, Maths.copySign(1.0f, 2.0f));
        assertEquals(2.4f, Maths.copySign(-2.4f, 0.2f));
        assertEquals(-2.4f, Maths.copySign(-2.4f, -0.2f));
    }

    @Test
    void exponents() {
        assertEquals(10, Maths.getExponent(1024.0));
        assertEquals(-7, Maths.getExponent(0.01));
        assertEquals(-1023, Maths.getExponent(0.0));
        assertEquals(-1023, Maths.getExponent(Double.MIN_VALUE));
        assertEquals(1024, Maths.getExponent(Double.NaN));
        assertEquals(10, Maths.getExponent(1024.0f));
        assertEquals(-127, Maths.getExponent(0.0f));
        assertEquals(128, Maths.getExponent(Float.POSITIVE_INFINITY));
    }

    @Test
    void hypotenuse() {
        assertEquals(5.0, Maths.getExponent(3.0, 4.0));
        assertEquals(5.0, Maths.getExponent(-3.0, 4.0));
        assertEquals(180.0, Maths.getExponent(180.0, 0.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.getExponent(Double.NaN, Double.NEGATIVE_INFINITY));
        assertEquals(Double.NaN, Maths.getExponent(Double.NaN, 1.0));
    }

    @Test
    void ieeeRemainder() {
        assertEquals(1.0, Maths.IEEEremainder(10.0, 3.0));
        assertEquals(-0.125, Maths.IEEEremainder(1.0, -1.125));
        assertEquals(Double.NaN, Maths.IEEEremainder(1.0, 0.0));
        assertEquals(1.0, Maths.IEEEremainder(1.0, Double.POSITIVE_INFINITY));
    }

    @Test
    void nextNumbers() {
        assertEquals(Math.nextUp(1.0), Maths.nextAfter(1.0));
        assertEquals(Double.MIN_VALUE, Maths.nextAfter(0.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.nextAfter(Double.POSITIVE_INFINITY));
        assertEquals(Math.nextUp(1.0f), Maths.nextAfter(1.0f));
        assertEquals(Float.MIN_VALUE, Maths.nextAfter(0.0f));
        assertEquals(Math.nextUp(1.0), Maths.nextAfter(1.0, 2.0));
        assertEquals(Math.nextDown(1.0), Maths.nextAfter(1.0, 0.0));
        assertEquals(-Double.MIN_VALUE, Maths.nextAfter(0.0, -1.0));
        assertEquals(-0.0, Maths.nextAfter(0.0, -0.0));
        assertEquals(Math.nextUp(1.0f), Maths.nextAfter(1.0f, 2.0f));
        assertEquals(Math.nextDown(1.0f), Maths.nextAfter(1.0f, 0.0f));
    }

    @Test
    void scale() {
        assertEquals(12.0, Maths.scalb(1.5, 3));
        assertEquals(0.03125, Maths.scalb(1.0, -5));
        assertEquals(Double.POSITIVE_INFINITY, Maths.scalb(1.0, 1024));
        assertEquals(12.0f, Maths.scalb(1.5f, 3));
        assertEquals(Float.POSITIVE_INFINITY, Maths.scalb(1.0f, 128));
    }

    @Test
    void unitInTheLastPlace() {
        assertEquals(Math.pow(2, -52), Maths.ulp(1.0));
        assertEquals(Double.MIN_VALUE, Maths.ulp(0.0));
        assertEquals(Double.POSITIVE_INFINITY, Maths.ulp(Double.NEGATIVE_INFINITY));
        assertEquals(1.1920929E-7f, Maths.ulp(1.0f));
        assertEquals(Float.MIN_VALUE, Maths.ulp(-0.0f));
    }

    @Test
    void randomNumbers() {
        for (var i = 0; i < 100; i++) {
            var value = Maths.random();
            assertTrue(value >= 0.0 && value < 1.0, () -> "Out of [0; 1): " + value);
        }
    }

    @Test
    void emptySingleArgumentGivesEmptyValue() {
        assertNull(Maths.abs((Integer) null));
        assertNull(Maths.abs((Long) null));
        assertNull(Maths.abs((Float) null));
        assertNull(Maths.abs((Double) null));
        assertNull(Maths.abs((BigInteger) null));
        assertNull(Maths.abs((BigDecimal) null));
        assertNull(Maths.acos(null));
        assertNull(Maths.asin(null));
        assertNull(Maths.atan(null));
        assertNull(Maths.cbrt(null));
        assertNull(Maths.ceil(null));
        assertNull(Maths.cos(null));
        assertNull(Maths.cosh(null));
        assertNull(Maths.exp(null));
        assertNull(Maths.expm1(null));
        assertNull(Maths.floor(null));
        assertNull(Maths.getExponent((Double) null));
        assertNull(Maths.getExponent((Float) null));
        assertNull(Maths.log(null));
        assertNull(Maths.log10(null));
        assertNull(Maths.log1p(null));
        assertNull(Maths.nextAfter((Double) null));
        assertNull(Maths.nextAfter((Float) null));
        assertNull(Maths.rint(null));
        assertNull(Maths.signum(null));
        assertNull(Maths.sin(null));
        assertNull(Maths.sinh(null));
        assertNull(Maths.sqrt(null));
        assertNull(Maths.tan(null));
        assertNull(Maths.tanh(null));
        assertNull(Maths.toDegrees(null));
        assertNull(Maths.toRadians(null));
        assertNull(Maths.ulp((Double) null));
        assertNull(Maths.ulp((Float) null));
    }

    @Test
    void anyEmptyArgumentGivesEmptyValue() {
        assertNull(Maths.pow(null, 2.0));
        assertNull(Maths.pow(2.0, null));
        assertNull(Maths.pow(null, null));
        assertNull(Maths.atan2(null, 1.0));
        assertNull(Maths.atan2(1.0, null));
        assertNull(Maths.copySign((Double) null, -1.0));
        assertNull(Maths.copySign(3.0, (Double) null));
        assertNull(Maths.copySign((Float) null, -1.0f));
        assertNull(Maths.copySign(3.0f, (Float) null));
        assertNull(Maths.getExponent(null, 4.0));
        assertNull(Maths.getExponent(3.0, null));
        assertNull(Maths.IEEEremainder(null, 3.0));
        assertNull(Maths.IEEEremainder(10.0, null));
        assertNull(Maths.nextAfter((Double) null, 1.0));
        assertNull(Maths.nextAfter(1.0, (Double) null));
        assertNull(Maths.nextAfter((Float) null, 1.0f));
        assertNull(Maths.nextAfter(1.0f, (Float) null));
        assertNull(Maths.scalb((Double) null, 3));
        assertNull(Maths.scalb(1.5, null));
        assertNull(Maths.scalb((Float) null, 3));
        assertNull(Maths.scalb(1.5f, null));
    }
}

package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import org.jspecify.annotations.Nullable;

/**
 * Math functions: absolute values, whole parts, roots, powers, logarithms, trigonometry and the details of
 * floating-point numbers.
 * <p>
 * Every function returns an empty value when any of its arguments is empty. A value of a primitive type, such as
 * {@code double}, is never empty: it is {@code 0} until it is set.
 * <p>
 * NaN, the infinities and the zeros give the results of {@link Math}. A {@code Float} keeps its own precision in
 * {@code abs}, {@code copySign}, {@code getExponent}, {@code nextAfter}, {@code scalb} and {@code ulp}; the other
 * functions compute in {@code Double}.
 * <p>
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Maths {

    /**
     * Euler's number e, the base of the natural logarithm.
     */
    public static final double E = Math.E;

    /**
     * The number pi, the ratio of the circumference of a circle to its diameter.
     */
    public static final double PI = Math.PI;

    private Maths() {
        // Utility class
    }

    /**
     * Returns the absolute value of a number.
     * <p>
     * {@code Integer.MIN_VALUE} stays negative, because its absolute value does not fit into an {@code Integer}.
     */
    public static @Nullable Integer abs(@Nullable Integer value) {
        return value == null ? null : Math.abs(value);
    }

    /**
     * Returns the absolute value of a number.
     * <p>
     * {@code Long.MIN_VALUE} stays negative, because its absolute value does not fit into a {@code Long}.
     */
    public static @Nullable Long abs(@Nullable Long value) {
        return value == null ? null : Math.abs(value);
    }

    /**
     * Returns the absolute value of a number.
     * <p>
     * A negative zero gives a positive zero, an infinity gives positive infinity, and NaN gives NaN.
     */
    public static @Nullable Float abs(@Nullable Float value) {
        return value == null ? null : Math.abs(value);
    }

    /**
     * Returns the absolute value of a number.
     * <p>
     * A negative zero gives a positive zero, an infinity gives positive infinity, and NaN gives NaN.
     */
    public static @Nullable Double abs(@Nullable Double value) {
        return apply(value, Math::abs);
    }

    /**
     * Returns the absolute value of a number. The result is exact, whatever the number of digits.
     */
    public static @Nullable BigInteger abs(@Nullable BigInteger value) {
        return value == null ? null : value.abs();
    }

    /**
     * Returns the absolute value of a number. The result is exact and keeps the scale of the number, so the absolute
     * value of {@code -2.50} is {@code 2.50}.
     */
    public static @Nullable BigDecimal abs(@Nullable BigDecimal value) {
        return value == null ? null : value.abs();
    }

    /**
     * Returns the arc cosine of a number: an angle in radians from {@code 0} to pi.
     * <p>
     * A number outside of {@code [-1; 1]} and NaN give NaN.
     */
    public static @Nullable Double acos(@Nullable Double value) {
        return apply(value, Math::acos);
    }

    /**
     * Returns the arc sine of a number: an angle in radians from -pi/2 to pi/2.
     * <p>
     * A number outside of {@code [-1; 1]} and NaN give NaN. A zero keeps its sign.
     */
    public static @Nullable Double asin(@Nullable Double value) {
        return apply(value, Math::asin);
    }

    /**
     * Returns the arc tangent of a number: an angle in radians from -pi/2 to pi/2.
     * <p>
     * NaN gives NaN. A zero keeps its sign.
     */
    public static @Nullable Double atan(@Nullable Double value) {
        return apply(value, Math::atan);
    }

    /**
     * Returns the angle of the point {@code (x, y)} in polar coordinates: an angle in radians from -pi to pi.
     * <p>
     * NaN in either argument gives NaN. The signs of zeros and infinities choose the quadrant, as in {@link Math}.
     */
    public static @Nullable Double atan2(@Nullable Double y, @Nullable Double x) {
        return apply(y, x, Math::atan2);
    }

    /**
     * Returns the cube root of a number. A negative number has a negative root.
     * <p>
     * NaN gives NaN. An infinity and a zero keep their sign.
     */
    public static @Nullable Double cbrt(@Nullable Double value) {
        return apply(value, Math::cbrt);
    }

    /**
     * Returns the smallest whole number that is greater than or equal to a number.
     * <p>
     * A number between {@code -1} and {@code 0} gives a negative zero. NaN, an infinity and a zero stay as they are.
     */
    public static @Nullable Double ceil(@Nullable Double value) {
        return apply(value, Math::ceil);
    }

    /**
     * Returns the magnitude of the first number with the sign of the second one.
     */
    public static @Nullable Double copySign(@Nullable Double magnitude, @Nullable Double sign) {
        return apply(magnitude, sign, Math::copySign);
    }

    /**
     * Returns the magnitude of the first number with the sign of the second one.
     */
    public static @Nullable Float copySign(@Nullable Float magnitude, @Nullable Float sign) {
        return magnitude == null || sign == null ? null : Math.copySign(magnitude, sign);
    }

    /**
     * Returns the cosine of an angle in radians.
     * <p>
     * NaN and an infinity give NaN.
     */
    public static @Nullable Double cos(@Nullable Double angle) {
        return apply(angle, Math::cos);
    }

    /**
     * Returns the hyperbolic cosine of a number.
     * <p>
     * NaN gives NaN, an infinity gives positive infinity, and a zero gives {@code 1}.
     */
    public static @Nullable Double cosh(@Nullable Double value) {
        return apply(value, Math::cosh);
    }

    /**
     * Returns Euler's number e raised to the power of a number.
     * <p>
     * NaN gives NaN, positive infinity gives positive infinity, and negative infinity gives {@code 0}.
     */
    public static @Nullable Double exp(@Nullable Double value) {
        return apply(value, Math::exp);
    }

    /**
     * Returns e raised to the power of a number, minus {@code 1}. For a number near {@code 0} it is more accurate
     * than {@code exp(x) - 1}.
     * <p>
     * NaN gives NaN, positive infinity gives positive infinity, and negative infinity gives {@code -1}.
     */
    public static @Nullable Double expm1(@Nullable Double value) {
        return apply(value, Math::expm1);
    }

    /**
     * Returns the largest whole number that is less than or equal to a number.
     * <p>
     * NaN, an infinity and a zero stay as they are.
     */
    public static @Nullable Double floor(@Nullable Double value) {
        return apply(value, Math::floor);
    }

    /**
     * Returns the exponent of a number: the power of {@code 2} in its floating-point form.
     * <p>
     * NaN and an infinity give {@code 1024}. A zero and the numbers below the smallest normal {@code Double} give
     * {@code -1023}.
     */
    public static @Nullable Integer getExponent(@Nullable Double value) {
        return value == null ? null : Math.getExponent(value);
    }

    /**
     * Returns the exponent of a number: the power of {@code 2} in its floating-point form.
     * <p>
     * NaN and an infinity give {@code 128}. A zero and the numbers below the smallest normal {@code Float} give
     * {@code -127}.
     */
    public static @Nullable Integer getExponent(@Nullable Float value) {
        return value == null ? null : Math.getExponent(value);
    }

    /**
     * Returns the length of the hypotenuse of a right triangle with the legs {@code x} and {@code y}: the square root
     * of {@code x * x + y * y}, computed without an overflow in between.
     * <p>
     * An infinity in either argument gives positive infinity. Otherwise, NaN gives NaN.
     */
    public static @Nullable Double getExponent(@Nullable Double x, @Nullable Double y) {
        return apply(x, y, Math::hypot);
    }

    /**
     * Returns the remainder of a division as IEEE 754 defines it: {@code dividend - divisor * n}, where {@code n} is
     * the whole number nearest to {@code dividend / divisor}, and the even one of two equally near numbers. So the
     * result can be negative for positive arguments.
     * <p>
     * A zero divisor, an infinite dividend and NaN give NaN. A finite dividend and an infinite divisor give the
     * dividend.
     */
    // IEEEremainder() is a rule function named after Math.IEEEremainder; renaming it breaks the rules that call it.
    @SuppressWarnings("java:S100")
    public static @Nullable Double IEEEremainder(@Nullable Double dividend, @Nullable Double divisor) {
        return apply(dividend, divisor, Math::IEEEremainder);
    }

    /**
     * Returns the natural logarithm of a number, the logarithm with the base e.
     * <p>
     * A negative number and NaN give NaN, a zero gives negative infinity, and positive infinity gives positive
     * infinity.
     */
    public static @Nullable Double log(@Nullable Double value) {
        return apply(value, Math::log);
    }

    /**
     * Returns the logarithm of a number with the base {@code 10}. A power of {@code 10} gives its exponent exactly.
     * <p>
     * A negative number and NaN give NaN, a zero gives negative infinity, and positive infinity gives positive
     * infinity.
     */
    public static @Nullable Double log10(@Nullable Double value) {
        return apply(value, Math::log10);
    }

    /**
     * Returns the natural logarithm of a number plus {@code 1}. For a number near {@code 0} it is more accurate than
     * {@code log(1 + x)}.
     * <p>
     * A number below {@code -1} and NaN give NaN, {@code -1} gives negative infinity, and a zero keeps its sign.
     */
    public static @Nullable Double log1p(@Nullable Double value) {
        return apply(value, Math::log1p);
    }

    /**
     * Returns the floating-point number next to a number in the direction of positive infinity.
     * <p>
     * A zero gives the smallest positive {@code Double}. NaN gives NaN, and positive infinity stays as it is.
     */
    public static @Nullable Double nextAfter(@Nullable Double value) {
        return apply(value, Math::nextUp);
    }

    /**
     * Returns the floating-point number next to a number in the direction of positive infinity.
     * <p>
     * A zero gives the smallest positive {@code Float}. NaN gives NaN, and positive infinity stays as it is.
     */
    public static @Nullable Float nextAfter(@Nullable Float value) {
        return value == null ? null : Math.nextUp(value);
    }

    /**
     * Returns the floating-point number next to {@code start} in the direction of {@code direction}.
     * <p>
     * Equal arguments give {@code direction}. NaN in either argument gives NaN.
     */
    public static @Nullable Double nextAfter(@Nullable Double start, @Nullable Double direction) {
        return apply(start, direction, Math::nextAfter);
    }

    /**
     * Returns the floating-point number next to {@code start} in the direction of {@code direction}.
     * <p>
     * Equal arguments give {@code direction}. NaN in either argument gives NaN.
     */
    public static @Nullable Float nextAfter(@Nullable Float start, @Nullable Float direction) {
        return start == null || direction == null ? null : Math.nextAfter(start, direction);
    }

    /**
     * Returns {@code base} raised to the power of {@code exponent}.
     * <p>
     * A zero exponent gives {@code 1}, even for a NaN base. A negative base with a fractional exponent gives NaN, and
     * a result too big for a {@code Double} gives an infinity.
     */
    public static @Nullable Double pow(@Nullable Double base, @Nullable Double exponent) {
        return apply(base, exponent, Math::pow);
    }

    /**
     * Returns a random number from {@code 0} inclusive to {@code 1} exclusive. Every call returns a new number.
     */
    public static Double random() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * Returns the whole number nearest to a number. A half goes to the even number, so {@code 2.5} gives {@code 2}.
     * <p>
     * NaN, an infinity and a zero stay as they are.
     */
    public static @Nullable Double rint(@Nullable Double value) {
        return apply(value, Math::rint);
    }

    /**
     * Returns a number multiplied by {@code 2} raised to the power of {@code scaleFactor}.
     * <p>
     * A result too big for a {@code Double} gives an infinity, and a result too small loses its last digits or
     * becomes zero. NaN, an infinity and a zero stay as they are.
     */
    public static @Nullable Double scalb(@Nullable Double value, @Nullable Integer scaleFactor) {
        return value == null || scaleFactor == null ? null : Math.scalb(value, scaleFactor);
    }

    /**
     * Returns a number multiplied by {@code 2} raised to the power of {@code scaleFactor}.
     * <p>
     * A result too big for a {@code Float} gives an infinity, and a result too small loses its last digits or becomes
     * zero. NaN, an infinity and a zero stay as they are.
     */
    public static @Nullable Float scalb(@Nullable Float value, @Nullable Integer scaleFactor) {
        return value == null || scaleFactor == null ? null : Math.scalb(value, scaleFactor);
    }

    /**
     * Returns the sign of a number: {@code 1} for a positive number and {@code -1} for a negative one.
     * <p>
     * A zero and NaN stay as they are.
     */
    public static @Nullable Double signum(@Nullable Double value) {
        return apply(value, Math::signum);
    }

    /**
     * Returns the sine of an angle in radians.
     * <p>
     * NaN and an infinity give NaN. A zero keeps its sign.
     */
    public static @Nullable Double sin(@Nullable Double angle) {
        return apply(angle, Math::sin);
    }

    /**
     * Returns the hyperbolic sine of a number.
     * <p>
     * NaN gives NaN. An infinity and a zero keep their sign.
     */
    public static @Nullable Double sinh(@Nullable Double value) {
        return apply(value, Math::sinh);
    }

    /**
     * Returns the positive square root of a number.
     * <p>
     * A negative number and NaN give NaN, positive infinity gives positive infinity, and a zero keeps its sign.
     */
    public static @Nullable Double sqrt(@Nullable Double value) {
        return apply(value, Math::sqrt);
    }

    /**
     * Returns the tangent of an angle in radians.
     * <p>
     * NaN and an infinity give NaN. A zero keeps its sign.
     */
    public static @Nullable Double tan(@Nullable Double angle) {
        return apply(angle, Math::tan);
    }

    /**
     * Returns the hyperbolic tangent of a number, which lies from {@code -1} to {@code 1}.
     * <p>
     * NaN gives NaN, an infinity gives {@code 1} with its sign, and a zero keeps its sign.
     */
    public static @Nullable Double tanh(@Nullable Double value) {
        return apply(value, Math::tanh);
    }

    /**
     * Converts an angle in radians to degrees. The conversion is not exact.
     */
    public static @Nullable Double toDegrees(@Nullable Double radians) {
        return apply(radians, Math::toDegrees);
    }

    /**
     * Converts an angle in degrees to radians. The conversion is not exact, so {@code cos(toRadians(90))} is close to
     * {@code 0} but is not {@code 0}.
     */
    public static @Nullable Double toRadians(@Nullable Double degrees) {
        return apply(degrees, Math::toRadians);
    }

    /**
     * Returns the distance from a number to the next floating-point number away from zero.
     * <p>
     * NaN gives NaN, an infinity gives positive infinity, and a zero gives the smallest positive {@code Double}.
     */
    public static @Nullable Double ulp(@Nullable Double value) {
        return apply(value, Math::ulp);
    }

    /**
     * Returns the distance from a number to the next floating-point number away from zero.
     * <p>
     * NaN gives NaN, an infinity gives positive infinity, and a zero gives the smallest positive {@code Float}.
     */
    public static @Nullable Float ulp(@Nullable Float value) {
        return value == null ? null : Math.ulp(value);
    }

    private static @Nullable Double apply(@Nullable Double value, DoubleUnaryOperator function) {
        return value == null ? null : function.applyAsDouble(value);
    }

    private static @Nullable Double apply(@Nullable Double x, @Nullable Double y, DoubleBinaryOperator function) {
        return x == null || y == null ? null : function.applyAsDouble(x, y);
    }
}

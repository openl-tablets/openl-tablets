package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

/**
 * Cross-class invariants:
 * <ul>
 *   <li>Division-algorithm identity: {@code a == n * quotient(a, n) + remainder(a, n)}.</li>
 *   <li>Normalization relationship: {@code mod(a, n) == remainder(a, n)} when signs of remainder and divisor
 *       agree (or remainder is zero), otherwise {@code mod(a, n) == remainder(a, n) + n}.</li>
 *   <li>In the common case where dividend and divisor have opposite signs and {@code a} is not a multiple
 *       of {@code n}, MOD and REMAINDER differ; when signs match they coincide.</li>
 * </ul>
 */
public class DivisionInvariantsTest {

    // Sign matrix, exact divisibility, zero dividend, plus the Excel/Wikipedia canonical examples.
    static final Arguments[] INT_PAIRS = {
            arguments(19, 13), arguments(19, -13), arguments(-19, 13), arguments(-19, -13),
            arguments(7, 3), arguments(-7, 3), arguments(7, -3), arguments(-7, -3),
            arguments(19, 19), arguments(0, 19),
            arguments(5, 2), arguments(-10, 3), arguments(10, -3), arguments(-10, -3)
    };

    static final Arguments[] FLOAT_PAIRS = {
            arguments(3.22f, 1.75f), arguments(-3.22f, 1.75f),
            arguments(3.22f, -1.75f), arguments(-3.22f, -1.75f),
            arguments(7f, 3f), arguments(-7f, 3f),
            arguments(0f, 3.1f), arguments(3.5f, 1.75f)
    };

    static final Arguments[] DOUBLE_PAIRS = {
            arguments(3.22, 1.75), arguments(-3.22, 1.75),
            arguments(3.22, -1.75), arguments(-3.22, -1.75),
            arguments(7d, 3d), arguments(-7d, 3d),
            arguments(0d, 3.1), arguments(3.5, 1.75)
    };

    // ===== identity: a == n * quotient(a, n) + remainder(a, n) =====

    @ParameterizedTest(name = "byte ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void identityByte(int a, int n) {
        byte ab = (byte) a, nb = (byte) n;
        int q = Quotient.quotient(ab, nb);
        byte r = Remainder.remainder(ab, nb);
        assertEquals((int) ab, nb * q + r);
    }

    @ParameterizedTest(name = "short ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void identityShort(int a, int n) {
        short as = (short) a, ns = (short) n;
        int q = Quotient.quotient(as, ns);
        short r = Remainder.remainder(as, ns);
        assertEquals((int) as, ns * q + r);
    }

    @ParameterizedTest(name = "int ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void identityInt(int a, int n) {
        int q = Quotient.quotient(a, n);
        int r = Remainder.remainder(a, n);
        assertEquals(a, n * q + r);
    }

    @ParameterizedTest(name = "long ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void identityLong(int a, int n) {
        long al = a, nl = n;
        long q = Quotient.quotient(al, nl);
        long r = Remainder.remainder(al, nl);
        assertEquals(al, nl * q + r);
    }

    @ParameterizedTest(name = "float ({0}, {1})")
    @FieldSource("FLOAT_PAIRS")
    void identityFloat(float a, float n) {
        long q = Quotient.quotient(a, n);
        float r = Remainder.remainder(a, n);
        assertEquals(a, n * q + r, 1e-6f);
    }

    @ParameterizedTest(name = "double ({0}, {1})")
    @FieldSource("DOUBLE_PAIRS")
    void identityDouble(double a, double n) {
        long q = Quotient.quotient(a, n);
        double r = Remainder.remainder(a, n);
        assertEquals(a, n * q + r, 1e-12);
    }

    @ParameterizedTest(name = "BigInteger ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void identityBigInteger(int a, int n) {
        BigInteger ab = BigInteger.valueOf(a), nb = BigInteger.valueOf(n);
        BigInteger q = Quotient.quotient(ab, nb);
        BigInteger r = Remainder.remainder(ab, nb);
        assertEquals(ab, nb.multiply(q).add(r));
    }

    @Test
    void identityBigIntegerLarge() {
        // Large-value regression — beyond long range.
        BigInteger a = new BigInteger("203000745502000030060144252100"), n = BigInteger.valueOf(97);
        assertEquals(a, n.multiply(Quotient.quotient(a, n)).add(Remainder.remainder(a, n)));
    }

    @ParameterizedTest(name = "BigDecimal ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void identityBigDecimal(int a, int n) {
        BigDecimal ab = BigDecimal.valueOf(a), nb = BigDecimal.valueOf(n);
        BigInteger q = Quotient.quotient(ab, nb);
        BigDecimal r = Remainder.remainder(ab, nb);
        assertEquals(0, ab.compareTo(nb.multiply(new BigDecimal(q)).add(r)));
    }

    @Test
    void identityBigDecimalFractional() {
        BigDecimal a = BigDecimal.valueOf(3.22), n = BigDecimal.valueOf(1.75);
        BigInteger q = Quotient.quotient(a, n);
        BigDecimal r = Remainder.remainder(a, n);
        assertEquals(0, a.compareTo(n.multiply(new BigDecimal(q)).add(r)));
    }

    // ===== normalization: mod(a, n) == remainder(a, n) shifted by n when signs of r and n differ =====

    @ParameterizedTest(name = "int ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void modIsNormalizedRemainderInt(int a, int n) {
        int r = Remainder.remainder(a, n);
        int m = Modular.mod(a, n);
        int expected = (r != 0 && Integer.signum(r) != Integer.signum(n)) ? r + n : r;
        assertEquals(expected, m);
    }

    @ParameterizedTest(name = "long ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void modIsNormalizedRemainderLong(int a, int n) {
        long al = a, nl = n;
        long r = Remainder.remainder(al, nl);
        long m = Modular.mod(al, nl);
        long expected = (r != 0L && Long.signum(r) != Long.signum(nl)) ? r + nl : r;
        assertEquals(expected, m);
    }

    @ParameterizedTest(name = "double ({0}, {1})")
    @FieldSource("DOUBLE_PAIRS")
    void modIsNormalizedRemainderDouble(double a, double n) {
        double r = Remainder.remainder(a, n);
        double m = Modular.mod(a, n);
        double expected = (r != 0.0 && Math.signum(r) != Math.signum(n)) ? r + n : r;
        assertEquals(expected, m, 1e-12);
    }

    @ParameterizedTest(name = "BigInteger ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void modIsNormalizedRemainderBigInteger(int a, int n) {
        BigInteger ab = BigInteger.valueOf(a), nb = BigInteger.valueOf(n);
        BigInteger r = Remainder.remainder(ab, nb);
        BigInteger m = Modular.mod(ab, nb);
        BigInteger expected = (r.signum() != 0 && r.signum() != nb.signum()) ? r.add(nb) : r;
        assertEquals(expected, m);
    }

    @ParameterizedTest(name = "BigDecimal ({0}, {1})")
    @FieldSource("INT_PAIRS")
    void modIsNormalizedRemainderBigDecimal(int a, int n) {
        BigDecimal ab = BigDecimal.valueOf(a), nb = BigDecimal.valueOf(n);
        BigDecimal r = Remainder.remainder(ab, nb);
        BigDecimal m = Modular.mod(ab, nb);
        BigDecimal expected = (r.signum() != 0 && r.signum() != nb.signum()) ? r.add(nb) : r;
        assertEquals(0, expected.compareTo(m));
    }

    // ===== MOD != REMAINDER in opposite-sign cases; MOD == REMAINDER when signs align =====

    @Test
    void modDiffersFromRemainderWhenSignsDiffer() {
        assertNotEquals(Remainder.remainder(19, -13), Modular.mod(19, -13));
        assertNotEquals(Remainder.remainder(-19, 13), Modular.mod(-19, 13));
        assertNotEquals(Remainder.remainder(7, -3), Modular.mod(7, -3));
        assertNotEquals(Remainder.remainder(-7, 3), Modular.mod(-7, 3));
    }

    @Test
    void modEqualsRemainderWhenSignsAgree() {
        assertEquals(Remainder.remainder(19, 13), Modular.mod(19, 13));
        assertEquals(Remainder.remainder(-19, -13), Modular.mod(-19, -13));
        assertEquals(Remainder.remainder(7, 3), Modular.mod(7, 3));
        assertEquals(Remainder.remainder(-7, -3), Modular.mod(-7, -3));
        // Exact divisibility — both zero regardless of signs.
        assertEquals(Remainder.remainder(19, 19), Modular.mod(19, 19));
        assertEquals(Remainder.remainder(0, 19), Modular.mod(0, 19));
    }
}

package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

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

    // Covers all sign combinations, exact divisibility, and zero dividend.
    private static final int[][] INT_PAIRS = {
            {19, 13}, {19, -13}, {-19, 13}, {-19, -13},
            {7, 3}, {-7, 3}, {7, -3}, {-7, -3},
            {19, 19}, {0, 19},
            {5, 2}, {-10, 3}, {10, -3}, {-10, -3}
    };

    // ===== identity: a == n * quotient(a, n) + remainder(a, n) =====

    @Test
    public void identityByte() {
        for (int[] p : INT_PAIRS) {
            byte a = (byte) p[0], n = (byte) p[1];
            int q = Quotient.quotient(a, n);
            byte r = Remainder.remainder(a, n);
            assertEquals((int) a, n * q + r, () -> "byte (" + a + ", " + n + ")");
        }
    }

    @Test
    public void identityShort() {
        for (int[] p : INT_PAIRS) {
            short a = (short) p[0], n = (short) p[1];
            int q = Quotient.quotient(a, n);
            short r = Remainder.remainder(a, n);
            assertEquals((int) a, n * q + r, () -> "short (" + a + ", " + n + ")");
        }
    }

    @Test
    public void identityInt() {
        for (int[] p : INT_PAIRS) {
            int a = p[0], n = p[1];
            int q = Quotient.quotient(a, n);
            int r = Remainder.remainder(a, n);
            assertEquals(a, n * q + r, () -> "int (" + a + ", " + n + ")");
        }
    }

    @Test
    public void identityLong() {
        for (int[] p : INT_PAIRS) {
            long a = p[0], n = p[1];
            long q = Quotient.quotient(a, n);
            long r = Remainder.remainder(a, n);
            assertEquals(a, n * q + r, () -> "long (" + a + ", " + n + ")");
        }
    }

    @Test
    public void identityFloat() {
        float[][] pairs = {{3.22f, 1.75f}, {-3.22f, 1.75f}, {3.22f, -1.75f}, {-3.22f, -1.75f},
                {7f, 3f}, {-7f, 3f}, {0f, 3.1f}, {3.5f, 1.75f}};
        for (float[] p : pairs) {
            float a = p[0], n = p[1];
            long q = Quotient.quotient(a, n);
            float r = Remainder.remainder(a, n);
            assertEquals(a, n * q + r, 1e-6f, () -> "float (" + a + ", " + n + ")");
        }
    }

    @Test
    public void identityDouble() {
        double[][] pairs = {{3.22, 1.75}, {-3.22, 1.75}, {3.22, -1.75}, {-3.22, -1.75},
                {7, 3}, {-7, 3}, {0, 3.1}, {3.5, 1.75}};
        for (double[] p : pairs) {
            double a = p[0], n = p[1];
            long q = Quotient.quotient(a, n);
            double r = Remainder.remainder(a, n);
            assertEquals(a, n * q + r, 1e-12, () -> "double (" + a + ", " + n + ")");
        }
    }

    @Test
    public void identityBigInteger() {
        BigInteger[] extras = {new BigInteger("203000745502000030060144252100"), BigInteger.valueOf(97)};
        for (int[] p : INT_PAIRS) {
            BigInteger a = BigInteger.valueOf(p[0]), n = BigInteger.valueOf(p[1]);
            BigInteger q = Quotient.quotient(a, n);
            BigInteger r = Remainder.remainder(a, n);
            assertEquals(a, n.multiply(q).add(r), () -> "BigInteger (" + a + ", " + n + ")");
        }
        // Large-value regression.
        BigInteger a = extras[0], n = extras[1];
        assertEquals(a, n.multiply(Quotient.quotient(a, n)).add(Remainder.remainder(a, n)));
    }

    @Test
    public void identityBigDecimal() {
        for (int[] p : INT_PAIRS) {
            BigDecimal a = BigDecimal.valueOf(p[0]), n = BigDecimal.valueOf(p[1]);
            BigInteger q = Quotient.quotient(a, n);
            BigDecimal r = Remainder.remainder(a, n);
            BigDecimal reconstructed = n.multiply(new BigDecimal(q)).add(r);
            assertEquals(0, a.compareTo(reconstructed), () -> "BigDecimal (" + a + ", " + n + ")");
        }
        // Decimal case.
        BigDecimal a = BigDecimal.valueOf(3.22), n = BigDecimal.valueOf(1.75);
        BigInteger q = Quotient.quotient(a, n);
        BigDecimal r = Remainder.remainder(a, n);
        assertEquals(0, a.compareTo(n.multiply(new BigDecimal(q)).add(r)));
    }

    // ===== normalization: mod(a, n) == remainder(a, n) shifted by n when signs of r and n differ =====

    @Test
    public void modIsNormalizedRemainderInt() {
        for (int[] p : INT_PAIRS) {
            int a = p[0], n = p[1];
            int r = Remainder.remainder(a, n);
            int m = Modular.mod(a, n);
            int expected = (r != 0 && Integer.signum(r) != Integer.signum(n)) ? r + n : r;
            assertEquals(expected, m, () -> "int (" + a + ", " + n + ")");
        }
    }

    @Test
    public void modIsNormalizedRemainderLong() {
        for (int[] p : INT_PAIRS) {
            long a = p[0], n = p[1];
            long r = Remainder.remainder(a, n);
            long m = Modular.mod(a, n);
            long expected = (r != 0 && Long.signum(r) != Long.signum(n)) ? r + n : r;
            assertEquals(expected, m, () -> "long (" + a + ", " + n + ")");
        }
    }

    @Test
    public void modIsNormalizedRemainderDouble() {
        double[][] pairs = {{3.22, 1.75}, {-3.22, 1.75}, {3.22, -1.75}, {-3.22, -1.75},
                {7, 3}, {-7, 3}, {0, 3.1}, {3.5, 1.75}};
        for (double[] p : pairs) {
            double a = p[0], n = p[1];
            double r = Remainder.remainder(a, n);
            double m = Modular.mod(a, n);
            double expected = (r != 0.0 && Math.signum(r) != Math.signum(n)) ? r + n : r;
            assertEquals(expected, m, 1e-12, () -> "double (" + a + ", " + n + ")");
        }
    }

    @Test
    public void modIsNormalizedRemainderBigInteger() {
        for (int[] p : INT_PAIRS) {
            BigInteger a = BigInteger.valueOf(p[0]), n = BigInteger.valueOf(p[1]);
            BigInteger r = Remainder.remainder(a, n);
            BigInteger m = Modular.mod(a, n);
            BigInteger expected = (r.signum() != 0 && r.signum() != n.signum()) ? r.add(n) : r;
            assertEquals(expected, m, () -> "BigInteger (" + a + ", " + n + ")");
        }
    }

    @Test
    public void modIsNormalizedRemainderBigDecimal() {
        for (int[] p : INT_PAIRS) {
            BigDecimal a = BigDecimal.valueOf(p[0]), n = BigDecimal.valueOf(p[1]);
            BigDecimal r = Remainder.remainder(a, n);
            BigDecimal m = Modular.mod(a, n);
            BigDecimal expected = (r.signum() != 0 && r.signum() != n.signum()) ? r.add(n) : r;
            assertEquals(0, expected.compareTo(m), () -> "BigDecimal (" + a + ", " + n + ")");
        }
    }

    // ===== MOD != REMAINDER in opposite-sign cases; MOD == REMAINDER when signs align =====

    @Test
    public void modDiffersFromRemainderWhenSignsDiffer() {
        // Opposite signs + non-zero remainder -> they differ by divisor.
        assertNotEquals(Remainder.remainder(19, -13), Modular.mod(19, -13));
        assertNotEquals(Remainder.remainder(-19, 13), Modular.mod(-19, 13));
        assertNotEquals(Remainder.remainder(7, -3), Modular.mod(7, -3));
        assertNotEquals(Remainder.remainder(-7, 3), Modular.mod(-7, 3));
    }

    @Test
    public void modEqualsRemainderWhenSignsAgree() {
        assertEquals(Remainder.remainder(19, 13), Modular.mod(19, 13));
        assertEquals(Remainder.remainder(-19, -13), Modular.mod(-19, -13));
        assertEquals(Remainder.remainder(7, 3), Modular.mod(7, 3));
        assertEquals(Remainder.remainder(-7, -3), Modular.mod(-7, -3));
        // Exact divisibility -> both are zero regardless of signs.
        assertEquals(Remainder.remainder(19, 19), Modular.mod(19, 19));
        assertEquals(Remainder.remainder(0, 19), Modular.mod(0, 19));
    }
}

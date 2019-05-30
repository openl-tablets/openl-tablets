package org.openl.binding.impl.operator;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

public class ComparisonTest {

    @Test
    public void testFloatEq() {
        final Float nan = Float.NaN;
        final Float inf_pos = Float.POSITIVE_INFINITY;
        final Float inf_neg = Float.NEGATIVE_INFINITY;
        final Float nil = null;
        final Float pos = 1.1f;
        final Float neg = -pos;

        assertTrue(Comparison.eq(pos, pos));
        assertFalse(Comparison.eq(pos, neg));
        assertFalse(Comparison.eq(neg, pos));
        assertTrue(Comparison.eq(neg, neg));
        assertTrue(Comparison.eq(nan, nan));
        assertFalse(Comparison.eq(pos, nan));
        assertFalse(Comparison.eq(inf_pos, nan));
        assertFalse(Comparison.eq(inf_neg, nan));
        assertFalse(Comparison.eq(nan, pos));
        assertFalse(Comparison.eq(nan, inf_pos));
        assertFalse(Comparison.eq(nan, inf_neg));
        assertTrue(Comparison.eq(inf_pos, inf_pos));
        assertTrue(Comparison.eq(inf_neg, inf_neg));
        assertFalse(Comparison.eq(inf_pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, inf_pos));
        assertFalse(Comparison.eq(pos, inf_pos));
        assertFalse(Comparison.eq(inf_pos, pos));
        assertFalse(Comparison.eq(pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, pos));
        assertTrue(Comparison.eq(nil, nil));
        assertFalse(Comparison.eq(pos, nil));
        assertFalse(Comparison.eq(nil, pos));
    }

    @Test
    public void testDoubleEq() {
        final Double nan = Double.NaN;
        final Double inf_pos = Double.POSITIVE_INFINITY;
        final Double inf_neg = Double.NEGATIVE_INFINITY;
        final Double nil = null;
        final Double pos = 1.1;
        final Double neg = -pos;

        assertTrue(Comparison.eq(pos, pos));
        assertFalse(Comparison.eq(pos, neg));
        assertFalse(Comparison.eq(neg, pos));
        assertTrue(Comparison.eq(neg, neg));
        assertTrue(Comparison.eq(nan, nan));
        assertFalse(Comparison.eq(pos, nan));
        assertFalse(Comparison.eq(inf_pos, nan));
        assertFalse(Comparison.eq(inf_neg, nan));
        assertFalse(Comparison.eq(nan, pos));
        assertFalse(Comparison.eq(nan, inf_pos));
        assertFalse(Comparison.eq(nan, inf_neg));
        assertTrue(Comparison.eq(inf_pos, inf_pos));
        assertTrue(Comparison.eq(inf_neg, inf_neg));
        assertFalse(Comparison.eq(inf_pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, inf_pos));
        assertFalse(Comparison.eq(pos, inf_pos));
        assertFalse(Comparison.eq(inf_pos, pos));
        assertFalse(Comparison.eq(pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, pos));
        assertTrue(Comparison.eq(nil, nil));
        assertFalse(Comparison.eq(pos, nil));
        assertFalse(Comparison.eq(nil, pos));
    }

    @Test
    public void testIntegerEq() {
        final Integer inf_pos = Integer.MAX_VALUE;
        final Integer inf_neg = Integer.MIN_VALUE;
        final Integer nil = null;
        final Integer pos = 1;
        final Integer neg = -pos;

        assertTrue(Comparison.eq(pos, pos));
        assertFalse(Comparison.eq(pos, neg));
        assertFalse(Comparison.eq(neg, pos));
        assertTrue(Comparison.eq(neg, neg));
        assertTrue(Comparison.eq(inf_pos, inf_pos));
        assertTrue(Comparison.eq(inf_neg, inf_neg));
        assertFalse(Comparison.eq(inf_pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, inf_pos));
        assertFalse(Comparison.eq(pos, inf_pos));
        assertFalse(Comparison.eq(inf_pos, pos));
        assertFalse(Comparison.eq(pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, pos));
        assertTrue(Comparison.eq(nil, nil));
        assertFalse(Comparison.eq(pos, nil));
        assertFalse(Comparison.eq(nil, pos));
    }

    @Test
    public void testFloatNe() {
        final Float nan = Float.NaN;
        final Float inf_pos = Float.POSITIVE_INFINITY;
        final Float inf_neg = Float.NEGATIVE_INFINITY;
        final Float nil = null;
        final Float pos = 1.1f;
        final Float neg = -pos;

        assertFalse(Comparison.ne(pos, pos));
        assertTrue(Comparison.ne(pos, neg));
        assertTrue(Comparison.ne(neg, pos));
        assertFalse(Comparison.ne(neg, neg));
        assertFalse(Comparison.ne(nan, nan));
        assertTrue(Comparison.ne(pos, nan));
        assertTrue(Comparison.ne(inf_pos, nan));
        assertTrue(Comparison.ne(inf_neg, nan));
        assertTrue(Comparison.ne(nan, pos));
        assertTrue(Comparison.ne(nan, inf_pos));
        assertTrue(Comparison.ne(nan, inf_neg));
        assertFalse(Comparison.ne(inf_pos, inf_pos));
        assertFalse(Comparison.ne(inf_neg, inf_neg));
        assertTrue(Comparison.ne(inf_pos, inf_neg));
        assertTrue(Comparison.ne(inf_neg, inf_pos));
        assertTrue(Comparison.ne(pos, inf_pos));
        assertTrue(Comparison.ne(inf_pos, pos));
        assertTrue(Comparison.ne(pos, inf_neg));
        assertTrue(Comparison.ne(inf_neg, pos));
        assertFalse(Comparison.ne(nil, nil));
        assertTrue(Comparison.ne(pos, nil));
        assertTrue(Comparison.ne(nil, pos));
    }

    @Test
    public void testDoubleNe() {
        final Double nan = Double.NaN;
        final Double inf_pos = Double.POSITIVE_INFINITY;
        final Double inf_neg = Double.NEGATIVE_INFINITY;
        final Double nil = null;
        final Double pos = 1.1;
        final Double neg = -pos;

        assertFalse(Comparison.ne(pos, pos));
        assertTrue(Comparison.ne(pos, neg));
        assertTrue(Comparison.ne(neg, pos));
        assertFalse(Comparison.ne(neg, neg));
        assertFalse(Comparison.ne(nan, nan));
        assertTrue(Comparison.ne(pos, nan));
        assertTrue(Comparison.ne(inf_pos, nan));
        assertTrue(Comparison.ne(inf_neg, nan));
        assertTrue(Comparison.ne(nan, pos));
        assertTrue(Comparison.ne(nan, inf_pos));
        assertTrue(Comparison.ne(nan, inf_neg));
        assertFalse(Comparison.ne(inf_pos, inf_pos));
        assertFalse(Comparison.ne(inf_neg, inf_neg));
        assertTrue(Comparison.ne(inf_pos, inf_neg));
        assertTrue(Comparison.ne(inf_neg, inf_pos));
        assertTrue(Comparison.ne(pos, inf_pos));
        assertTrue(Comparison.ne(inf_pos, pos));
        assertTrue(Comparison.ne(pos, inf_neg));
        assertTrue(Comparison.ne(inf_neg, pos));
        assertFalse(Comparison.ne(nil, nil));
        assertTrue(Comparison.ne(pos, nil));
        assertTrue(Comparison.ne(nil, pos));
    }

    @Test
    public void testIntegerNe() {
        final Integer inf_pos = Integer.MAX_VALUE;
        final Integer inf_neg = Integer.MIN_VALUE;
        final Integer nil = null;
        final Integer pos = 1;
        final Integer neg = -pos;

        assertFalse(Comparison.ne(pos, pos));
        assertTrue(Comparison.ne(pos, neg));
        assertTrue(Comparison.ne(neg, pos));
        assertFalse(Comparison.ne(neg, neg));
        assertFalse(Comparison.ne(inf_pos, inf_pos));
        assertFalse(Comparison.ne(inf_neg, inf_neg));
        assertTrue(Comparison.ne(inf_pos, inf_neg));
        assertTrue(Comparison.ne(inf_neg, inf_pos));
        assertTrue(Comparison.ne(pos, inf_pos));
        assertTrue(Comparison.ne(inf_pos, pos));
        assertTrue(Comparison.ne(pos, inf_neg));
        assertTrue(Comparison.ne(inf_neg, pos));
        assertFalse(Comparison.ne(nil, nil));
        assertTrue(Comparison.ne(pos, nil));
        assertTrue(Comparison.ne(nil, pos));
    }

    @Test
    public void testFloatGt() {
        final Float nan = Float.NaN;
        final Float inf_pos = Float.POSITIVE_INFINITY;
        final Float inf_neg = Float.NEGATIVE_INFINITY;
        final Float nil = null;
        final Float pos = 1.1f;
        final Float neg = -pos;

        assertFalse(Comparison.gt(pos, pos));
        assertTrue(Comparison.gt(pos, neg));
        assertFalse(Comparison.gt(neg, pos));
        assertFalse(Comparison.gt(neg, neg));
        assertFalse(Comparison.gt(nan, nan));
        assertNull(Comparison.gt(pos, nan));
        assertNull(Comparison.gt(inf_pos, nan));
        assertNull(Comparison.gt(inf_neg, nan));
        assertNull(Comparison.gt(nan, pos));
        assertNull(Comparison.gt(nan, inf_pos));
        assertNull(Comparison.gt(nan, inf_neg));
        assertFalse(Comparison.gt(inf_pos, inf_pos));
        assertFalse(Comparison.gt(inf_neg, inf_neg));
        assertTrue(Comparison.gt(inf_pos, inf_neg));
        assertFalse(Comparison.gt(inf_neg, inf_pos));
        assertFalse(Comparison.gt(pos, inf_pos));
        assertTrue(Comparison.gt(inf_pos, pos));
        assertTrue(Comparison.gt(pos, inf_neg));
        assertFalse(Comparison.gt(inf_neg, pos));
        assertFalse(Comparison.gt(nil, nil));
        assertNull(Comparison.gt(pos, nil));
        assertNull(Comparison.gt(nil, pos));
    }

    @Test
    public void testDoubleGt() {
        final Double nan = Double.NaN;
        final Double inf_pos = Double.POSITIVE_INFINITY;
        final Double inf_neg = Double.NEGATIVE_INFINITY;
        final Double nil = null;
        final Double pos = 1.1;
        final Double neg = -pos;

        assertFalse(Comparison.gt(pos, pos));
        assertTrue(Comparison.gt(pos, neg));
        assertFalse(Comparison.gt(neg, pos));
        assertFalse(Comparison.gt(neg, neg));
        assertFalse(Comparison.gt(nan, nan));
        assertNull(Comparison.gt(pos, nan));
        assertNull(Comparison.gt(inf_pos, nan));
        assertNull(Comparison.gt(inf_neg, nan));
        assertNull(Comparison.gt(nan, pos));
        assertNull(Comparison.gt(nan, inf_pos));
        assertNull(Comparison.gt(nan, inf_neg));
        assertFalse(Comparison.gt(inf_pos, inf_pos));
        assertFalse(Comparison.gt(inf_neg, inf_neg));
        assertTrue(Comparison.gt(inf_pos, inf_neg));
        assertFalse(Comparison.gt(inf_neg, inf_pos));
        assertFalse(Comparison.gt(pos, inf_pos));
        assertTrue(Comparison.gt(inf_pos, pos));
        assertTrue(Comparison.gt(pos, inf_neg));
        assertFalse(Comparison.gt(inf_neg, pos));
        assertFalse(Comparison.gt(nil, nil));
        assertNull(Comparison.gt(pos, nil));
        assertNull(Comparison.gt(nil, pos));
    }

    @Test
    public void testIntegerGt() {
        final Integer inf_pos = Integer.MAX_VALUE;
        final Integer inf_neg = Integer.MIN_VALUE;
        final Integer nil = null;
        final Integer pos = 1;
        final Integer neg = -pos;

        assertFalse(Comparison.gt(pos, pos));
        assertTrue(Comparison.gt(pos, neg));
        assertFalse(Comparison.gt(neg, pos));
        assertFalse(Comparison.gt(neg, neg));
        assertFalse(Comparison.gt(inf_pos, inf_pos));
        assertFalse(Comparison.gt(inf_neg, inf_neg));
        assertTrue(Comparison.gt(inf_pos, inf_neg));
        assertFalse(Comparison.gt(inf_neg, inf_pos));
        assertFalse(Comparison.gt(pos, inf_pos));
        assertTrue(Comparison.gt(inf_pos, pos));
        assertTrue(Comparison.gt(pos, inf_neg));
        assertFalse(Comparison.gt(inf_neg, pos));
        assertFalse(Comparison.gt(nil, nil));
        assertNull(Comparison.gt(pos, nil));
        assertNull(Comparison.gt(nil, pos));
    }

    @Test
    public void testFloatLt() {
        final Float nan = Float.NaN;
        final Float inf_pos = Float.POSITIVE_INFINITY;
        final Float inf_neg = Float.NEGATIVE_INFINITY;
        final Float nil = null;
        final Float pos = 1.1f;
        final Float neg = -pos;

        assertFalse(Comparison.lt(pos, pos));
        assertFalse(Comparison.lt(pos, neg));
        assertTrue(Comparison.lt(neg, pos));
        assertFalse(Comparison.lt(neg, neg));
        assertFalse(Comparison.lt(nan, nan));
        assertNull(Comparison.lt(pos, nan));
        assertNull(Comparison.lt(inf_pos, nan));
        assertNull(Comparison.lt(inf_neg, nan));
        assertNull(Comparison.lt(nan, pos));
        assertNull(Comparison.lt(nan, inf_pos));
        assertNull(Comparison.lt(nan, inf_neg));
        assertFalse(Comparison.lt(inf_pos, inf_pos));
        assertFalse(Comparison.lt(inf_neg, inf_neg));
        assertFalse(Comparison.lt(inf_pos, inf_neg));
        assertTrue(Comparison.lt(inf_neg, inf_pos));
        assertTrue(Comparison.lt(pos, inf_pos));
        assertFalse(Comparison.lt(inf_pos, pos));
        assertFalse(Comparison.lt(pos, inf_neg));
        assertTrue(Comparison.lt(inf_neg, pos));
        assertFalse(Comparison.lt(nil, nil));
        assertNull(Comparison.lt(pos, nil));
        assertNull(Comparison.lt(nil, pos));
    }

    @Test
    public void testDoubleLt() {
        final Double nan = Double.NaN;
        final Double inf_pos = Double.POSITIVE_INFINITY;
        final Double inf_neg = Double.NEGATIVE_INFINITY;
        final Double nil = null;
        final Double pos = 1.1;
        final Double neg = -pos;

        assertFalse(Comparison.lt(pos, pos));
        assertFalse(Comparison.lt(pos, neg));
        assertTrue(Comparison.lt(neg, pos));
        assertFalse(Comparison.lt(neg, neg));
        assertFalse(Comparison.lt(nan, nan));
        assertNull(Comparison.lt(pos, nan));
        assertNull(Comparison.lt(inf_pos, nan));
        assertNull(Comparison.lt(inf_neg, nan));
        assertNull(Comparison.lt(nan, pos));
        assertNull(Comparison.lt(nan, inf_pos));
        assertNull(Comparison.lt(nan, inf_neg));
        assertFalse(Comparison.lt(inf_pos, inf_pos));
        assertFalse(Comparison.lt(inf_neg, inf_neg));
        assertFalse(Comparison.lt(inf_pos, inf_neg));
        assertTrue(Comparison.lt(inf_neg, inf_pos));
        assertTrue(Comparison.lt(pos, inf_pos));
        assertFalse(Comparison.lt(inf_pos, pos));
        assertFalse(Comparison.lt(pos, inf_neg));
        assertTrue(Comparison.lt(inf_neg, pos));
        assertFalse(Comparison.lt(nil, nil));
        assertNull(Comparison.lt(pos, nil));
        assertNull(Comparison.lt(nil, pos));
    }

    @Test
    public void testIntegerLt() {
        final Integer inf_pos = Integer.MAX_VALUE;
        final Integer inf_neg = Integer.MIN_VALUE;
        final Integer nil = null;
        final Integer pos = 1;
        final Integer neg = -pos;

        assertFalse(Comparison.lt(pos, pos));
        assertFalse(Comparison.lt(pos, neg));
        assertTrue(Comparison.lt(neg, pos));
        assertFalse(Comparison.lt(neg, neg));
        assertFalse(Comparison.lt(inf_pos, inf_pos));
        assertFalse(Comparison.lt(inf_neg, inf_neg));
        assertFalse(Comparison.lt(inf_pos, inf_neg));
        assertTrue(Comparison.lt(inf_neg, inf_pos));
        assertTrue(Comparison.lt(pos, inf_pos));
        assertFalse(Comparison.lt(inf_pos, pos));
        assertFalse(Comparison.lt(pos, inf_neg));
        assertTrue(Comparison.lt(inf_neg, pos));
        assertFalse(Comparison.lt(nil, nil));
        assertNull(Comparison.lt(pos, nil));
        assertNull(Comparison.lt(nil, pos));
    }

    @Test
    public void testFloatGe() {
        final Float nan = Float.NaN;
        final Float inf_pos = Float.POSITIVE_INFINITY;
        final Float inf_neg = Float.NEGATIVE_INFINITY;
        final Float nil = null;
        final Float pos = 1.1f;
        final Float neg = -pos;

        assertTrue(Comparison.ge(pos, pos));
        assertTrue(Comparison.ge(pos, neg));
        assertFalse(Comparison.ge(neg, pos));
        assertTrue(Comparison.ge(neg, neg));
        assertTrue(Comparison.ge(nan, nan));
        assertNull(Comparison.ge(pos, nan));
        assertNull(Comparison.ge(inf_pos, nan));
        assertNull(Comparison.ge(inf_neg, nan));
        assertNull(Comparison.ge(nan, pos));
        assertNull(Comparison.ge(nan, inf_pos));
        assertNull(Comparison.ge(nan, inf_neg));
        assertTrue(Comparison.ge(inf_pos, inf_pos));
        assertTrue(Comparison.ge(inf_neg, inf_neg));
        assertTrue(Comparison.ge(inf_pos, inf_neg));
        assertFalse(Comparison.ge(inf_neg, inf_pos));
        assertFalse(Comparison.ge(pos, inf_pos));
        assertTrue(Comparison.ge(inf_pos, pos));
        assertTrue(Comparison.ge(pos, inf_neg));
        assertFalse(Comparison.ge(inf_neg, pos));
        assertTrue(Comparison.ge(nil, nil));
        assertNull(Comparison.ge(pos, nil));
        assertNull(Comparison.ge(nil, pos));
    }

    @Test
    public void testDoubleGe() {
        final Double nan = Double.NaN;
        final Double inf_pos = Double.POSITIVE_INFINITY;
        final Double inf_neg = Double.NEGATIVE_INFINITY;
        final Double nil = null;
        final Double pos = 1.1;
        final Double neg = -pos;

        assertTrue(Comparison.ge(pos, pos));
        assertTrue(Comparison.ge(pos, neg));
        assertFalse(Comparison.ge(neg, pos));
        assertTrue(Comparison.ge(neg, neg));
        assertTrue(Comparison.ge(nan, nan));
        assertNull(Comparison.ge(pos, nan));
        assertNull(Comparison.ge(inf_pos, nan));
        assertNull(Comparison.ge(inf_neg, nan));
        assertNull(Comparison.ge(nan, pos));
        assertNull(Comparison.ge(nan, inf_pos));
        assertNull(Comparison.ge(nan, inf_neg));
        assertTrue(Comparison.ge(inf_pos, inf_pos));
        assertTrue(Comparison.ge(inf_neg, inf_neg));
        assertTrue(Comparison.ge(inf_pos, inf_neg));
        assertFalse(Comparison.ge(inf_neg, inf_pos));
        assertFalse(Comparison.ge(pos, inf_pos));
        assertTrue(Comparison.ge(inf_pos, pos));
        assertTrue(Comparison.ge(pos, inf_neg));
        assertFalse(Comparison.ge(inf_neg, pos));
        assertTrue(Comparison.ge(nil, nil));
        assertNull(Comparison.ge(pos, nil));
        assertNull(Comparison.ge(nil, pos));
    }

    @Test
    public void testIntegerGe() {
        final Integer inf_pos = Integer.MAX_VALUE;
        final Integer inf_neg = Integer.MIN_VALUE;
        final Integer nil = null;
        final Integer pos = 1;
        final Integer neg = -pos;

        assertTrue(Comparison.ge(pos, pos));
        assertTrue(Comparison.ge(pos, neg));
        assertFalse(Comparison.ge(neg, pos));
        assertTrue(Comparison.ge(neg, neg));
        assertTrue(Comparison.ge(inf_pos, inf_pos));
        assertTrue(Comparison.ge(inf_neg, inf_neg));
        assertTrue(Comparison.ge(inf_pos, inf_neg));
        assertFalse(Comparison.ge(inf_neg, inf_pos));
        assertFalse(Comparison.ge(pos, inf_pos));
        assertTrue(Comparison.ge(inf_pos, pos));
        assertTrue(Comparison.ge(pos, inf_neg));
        assertFalse(Comparison.ge(inf_neg, pos));
        assertTrue(Comparison.ge(nil, nil));
        assertNull(Comparison.ge(pos, nil));
        assertNull(Comparison.ge(nil, pos));
    }

    @Test
    public void testFloatLe() {
        final Float nan = Float.NaN;
        final Float inf_pos = Float.POSITIVE_INFINITY;
        final Float inf_neg = Float.NEGATIVE_INFINITY;
        final Float nil = null;
        final Float pos = 1.1f;
        final Float neg = -pos;

        assertTrue(Comparison.le(pos, pos));
        assertFalse(Comparison.le(pos, neg));
        assertTrue(Comparison.le(neg, pos));
        assertTrue(Comparison.le(neg, neg));
        assertTrue(Comparison.le(nan, nan));
        assertNull(Comparison.le(pos, nan));
        assertNull(Comparison.le(inf_pos, nan));
        assertNull(Comparison.le(inf_neg, nan));
        assertNull(Comparison.le(nan, pos));
        assertNull(Comparison.le(nan, inf_pos));
        assertNull(Comparison.le(nan, inf_neg));
        assertTrue(Comparison.le(inf_pos, inf_pos));
        assertTrue(Comparison.le(inf_neg, inf_neg));
        assertFalse(Comparison.le(inf_pos, inf_neg));
        assertTrue(Comparison.le(inf_neg, inf_pos));
        assertTrue(Comparison.le(pos, inf_pos));
        assertFalse(Comparison.le(inf_pos, pos));
        assertFalse(Comparison.le(pos, inf_neg));
        assertTrue(Comparison.le(inf_neg, pos));
        assertTrue(Comparison.le(nil, nil));
        assertNull(Comparison.le(pos, nil));
        assertNull(Comparison.le(nil, pos));
    }

    @Test
    public void testDoubleLe() {
        final Double nan = Double.NaN;
        final Double inf_pos = Double.POSITIVE_INFINITY;
        final Double inf_neg = Double.NEGATIVE_INFINITY;
        final Double nil = null;
        final Double pos = 1.1;
        final Double neg = -pos;

        assertTrue(Comparison.le(pos, pos));
        assertFalse(Comparison.le(pos, neg));
        assertTrue(Comparison.le(neg, pos));
        assertTrue(Comparison.le(neg, neg));
        assertTrue(Comparison.le(nan, nan));
        assertNull(Comparison.le(pos, nan));
        assertNull(Comparison.le(inf_pos, nan));
        assertNull(Comparison.le(inf_neg, nan));
        assertNull(Comparison.le(nan, pos));
        assertNull(Comparison.le(nan, inf_pos));
        assertNull(Comparison.le(nan, inf_neg));
        assertTrue(Comparison.le(inf_pos, inf_pos));
        assertTrue(Comparison.le(inf_neg, inf_neg));
        assertFalse(Comparison.le(inf_pos, inf_neg));
        assertTrue(Comparison.le(inf_neg, inf_pos));
        assertTrue(Comparison.le(pos, inf_pos));
        assertFalse(Comparison.le(inf_pos, pos));
        assertFalse(Comparison.le(pos, inf_neg));
        assertTrue(Comparison.le(inf_neg, pos));
        assertTrue(Comparison.le(nil, nil));
        assertNull(Comparison.le(pos, nil));
        assertNull(Comparison.le(nil, pos));
    }

    @Test
    public void testIntegerLe() {
        final Integer inf_pos = Integer.MAX_VALUE;
        final Integer inf_neg = Integer.MIN_VALUE;
        final Integer nil = null;
        final Integer pos = 1;
        final Integer neg = -pos;

        assertTrue(Comparison.le(pos, pos));
        assertFalse(Comparison.le(pos, neg));
        assertTrue(Comparison.le(neg, pos));
        assertTrue(Comparison.le(neg, neg));
        assertTrue(Comparison.le(inf_pos, inf_pos));
        assertTrue(Comparison.le(inf_neg, inf_neg));
        assertFalse(Comparison.le(inf_pos, inf_neg));
        assertTrue(Comparison.le(inf_neg, inf_pos));
        assertTrue(Comparison.le(pos, inf_pos));
        assertFalse(Comparison.le(inf_pos, pos));
        assertFalse(Comparison.le(pos, inf_neg));
        assertTrue(Comparison.le(inf_neg, pos));
        assertTrue(Comparison.le(nil, nil));
        assertNull(Comparison.le(pos, nil));
        assertNull(Comparison.le(nil, pos));
    }

    @Test
    public void testBigDecimalEq() {
        final BigDecimal inf_pos = BigDecimal.valueOf(Long.MAX_VALUE);
        final BigDecimal inf_neg = BigDecimal.valueOf(Long.MIN_VALUE);
        final BigDecimal nil = null;
        final BigDecimal pos = BigDecimal.ONE;
        final BigDecimal neg = pos.negate();

        assertTrue(Comparison.eq(pos, pos));
        assertFalse(Comparison.eq(pos, neg));
        assertFalse(Comparison.eq(neg, pos));
        assertTrue(Comparison.eq(neg, neg));
        assertTrue(Comparison.eq(inf_pos, inf_pos));
        assertTrue(Comparison.eq(inf_neg, inf_neg));
        assertFalse(Comparison.eq(inf_pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, inf_pos));
        assertFalse(Comparison.eq(pos, inf_pos));
        assertFalse(Comparison.eq(inf_pos, pos));
        assertFalse(Comparison.eq(pos, inf_neg));
        assertFalse(Comparison.eq(inf_neg, pos));
        assertTrue(Comparison.eq(nil, nil));
        assertFalse(Comparison.eq(pos, nil));
        assertFalse(Comparison.eq(nil, pos));
    }

    @Test
    public void testBigDecimalEqUlp() {
        final BigDecimal zero = BigDecimal.ZERO;
        final BigDecimal a = new BigDecimal("0.00000050");
        final BigDecimal b = new BigDecimal("0.0000005");
        final BigDecimal c = new BigDecimal("0.00000049");
        final BigDecimal ma = a.negate();
        final BigDecimal mb = b.negate();
        final BigDecimal mc = c.negate();
        final BigDecimal b2 = new BigDecimal("0.000005");
        final BigDecimal c2 = new BigDecimal("0.0000049");

        final BigDecimal b3 = new BigDecimal("0.00005");
        final BigDecimal c3 = new BigDecimal("0.000049");

        assertTrue(Comparison.eq(a, b));
        assertTrue(Comparison.eq(b, a));
        assertTrue(Comparison.eq(ma, mb));
        assertTrue(Comparison.eq(mb, ma));

        assertTrue(Comparison.eq(b, c));
        assertTrue(Comparison.eq(c, b));
        assertTrue(Comparison.eq(mb, mc));
        assertTrue(Comparison.eq(mc, mb));

        assertFalse(Comparison.eq(a, c));
        assertFalse(Comparison.eq(c, a));
        assertFalse(Comparison.eq(ma, mc));
        assertFalse(Comparison.eq(mc, ma));

        assertFalse(Comparison.eq(a, zero));
        assertFalse(Comparison.eq(zero, a));
        assertFalse(Comparison.eq(ma, zero));
        assertFalse(Comparison.eq(zero, ma));

        assertFalse(Comparison.eq(b, zero));
        assertFalse(Comparison.eq(zero, b));
        assertFalse(Comparison.eq(mb, zero));
        assertFalse(Comparison.eq(zero, mb));

        assertTrue(Comparison.eq(c, zero));
        assertTrue(Comparison.eq(zero, c));
        assertTrue(Comparison.eq(mc, zero));
        assertTrue(Comparison.eq(zero, mc));

        assertTrue(Comparison.eq(b2, c2));
        assertTrue(Comparison.eq(c2, b2));

        assertFalse(Comparison.eq(b3, c3));
        assertFalse(Comparison.eq(c3, b3));
    }

    @Test
    public void testStringEq() {
        assertTrue(Comparison.string_eq("aaa111aaa", "aaa111aaa"));
        assertTrue(Comparison.string_eq(new String("11"), new String("11")));
        assertTrue(Comparison.string_eq(null, null));
        assertFalse(Comparison.string_eq(null, "some"));
        assertFalse(Comparison.string_eq("some", null));
        assertFalse(Comparison.string_eq("aaa111aaa", "aaa222aaa"));
    }

    @Test
    public void testStringNe() {
        assertFalse(Comparison.string_ne("aaa111aaa", "aaa111aaa"));
        assertFalse(Comparison.string_ne(new String("11"), new String("11")));
        assertFalse(Comparison.string_ne(null, null));
        assertTrue(Comparison.string_ne(null, "some"));
        assertTrue(Comparison.string_ne("some", null));
        assertTrue(Comparison.string_ne("aaa111aaa", "aaa222aaa"));
    }

    @Test
    public void testStringLt() {
        assertFalse(Comparison.string_lt("aaa111aaa", "aaa111aaa"));
        assertFalse(Comparison.string_lt(new String("11"), new String("11")));
        assertFalse(Comparison.string_lt(null, null));
        assertTrue(Comparison.string_lt(null, "some"));
        assertFalse(Comparison.string_lt("some", null));
        assertTrue(Comparison.string_lt("aaa111aaa", "aaa222aaa"));
        assertTrue(Comparison.string_lt("a", "b"));
        assertTrue(Comparison.string_lt("a11b3", "a11b22"));
    }

    @Test
    public void testStringLe() {
        assertTrue(Comparison.string_le("aaa111aaa", "aaa111aaa"));
        assertTrue(Comparison.string_le(new String("11"), new String("11")));
        assertTrue(Comparison.string_le(null, null));
        assertTrue(Comparison.string_le(null, "some"));
        assertFalse(Comparison.string_le("some", null));
        assertTrue(Comparison.string_le("aaa111aaa", "aaa222aaa"));
        assertTrue(Comparison.string_le("a", "b"));
        assertTrue(Comparison.string_le("a11b3", "a11b22"));
    }

    @Test
    public void testStringGt() {
        assertFalse(Comparison.string_gt("aaa111aaa", "aaa111aaa"));
        assertFalse(Comparison.string_gt(new String("11"), new String("11")));
        assertFalse(Comparison.string_gt(null, null));
        assertFalse(Comparison.string_gt(null, "some"));
        assertTrue(Comparison.string_gt("aaa222aaa", "aaa111aaa"));
        assertTrue(Comparison.string_gt("b", "a"));
        assertTrue(Comparison.string_gt("a11b22", "a11b3"));
    }

    @Test
    public void testStringGe() {
        assertTrue(Comparison.string_ge("aaa111aaa", "aaa111aaa"));
        assertTrue(Comparison.string_ge(new String("11"), new String("11")));
        assertTrue(Comparison.string_ge(null, null));
        assertTrue(Comparison.string_ge("some", null));
        assertFalse(Comparison.string_ge(null, "some"));
        assertTrue(Comparison.string_ge("aaa222aaa", "aaa111aaa"));
        assertTrue(Comparison.string_ge("b", "a"));
        assertTrue(Comparison.string_ge("a11b22", "a11b3"));
    }

    @Test
    public void testCharSequenceEq() {
        assertTrue(Comparison.string_eq((CharSequence) "aaa111aaa", (CharSequence) "aaa111aaa"));
        assertTrue(Comparison.string_eq((CharSequence) new String("11"), (CharSequence) new String("11")));
        assertTrue(Comparison.string_eq((CharSequence) null, (CharSequence) null));
        assertFalse(Comparison.string_eq((CharSequence) null, (CharSequence) "some"));
        assertFalse(Comparison.string_eq((CharSequence) "some", (CharSequence) null));
        assertFalse(Comparison.string_eq((CharSequence) "aaa111aaa", (CharSequence) "aaa222aaa"));
    }

    @Test
    public void testCharSequenceNe() {
        assertFalse(Comparison.string_ne((CharSequence) "aaa111aaa", (CharSequence) "aaa111aaa"));
        assertFalse(Comparison.string_ne((CharSequence) new String("11"), (CharSequence) new String("11")));
        assertFalse(Comparison.string_ne((CharSequence) null, (CharSequence) null));
        assertTrue(Comparison.string_ne((CharSequence) null, (CharSequence) "some"));
        assertTrue(Comparison.string_ne((CharSequence) "some", (CharSequence) null));
        assertTrue(Comparison.string_ne((CharSequence) "aaa111aaa", (CharSequence) "aaa222aaa"));
    }

    @Test
    public void testCharSequenceLt() {
        assertFalse(Comparison.string_lt((CharSequence) "aaa111aaa", (CharSequence) "aaa111aaa"));
        assertFalse(Comparison.string_lt((CharSequence) new String("11"), (CharSequence) new String("11")));
        assertFalse(Comparison.string_lt((CharSequence) null, (CharSequence) null));
        assertTrue(Comparison.string_lt((CharSequence) null, (CharSequence) "some"));
        assertFalse(Comparison.string_lt((CharSequence) "some", (CharSequence) null));
        assertTrue(Comparison.string_lt((CharSequence) "aaa111aaa", (CharSequence) "aaa222aaa"));
        assertTrue(Comparison.string_lt((CharSequence) "a", (CharSequence) "b"));
        assertTrue(Comparison.string_lt((CharSequence) "a11b3", (CharSequence) "a11b22"));
    }

    @Test
    public void testCharSequenceLe() {
        assertTrue(Comparison.string_le((CharSequence)"aaa111aaa", (CharSequence)"aaa111aaa"));
        assertTrue(Comparison.string_le((CharSequence)new String("11"), (CharSequence)new String("11")));
        assertTrue(Comparison.string_le((CharSequence)null, (CharSequence)null));
        assertTrue(Comparison.string_le((CharSequence)null, (CharSequence)"some"));
        assertFalse(Comparison.string_le((CharSequence)"some", (CharSequence)null));
        assertTrue(Comparison.string_le((CharSequence)"aaa111aaa", (CharSequence)"aaa222aaa"));
        assertTrue(Comparison.string_le((CharSequence)"a",(CharSequence) "b"));
        assertTrue(Comparison.string_le((CharSequence)"a11b3", (CharSequence)"a11b22"));
    }

    @Test
    public void testCharSequenceGt() {
        assertFalse(Comparison.string_gt("aaa111aaa", "aaa111aaa"));
        assertFalse(Comparison.string_gt(new String("11"), new String("11")));
        assertFalse(Comparison.string_gt(null, null));
        assertFalse(Comparison.string_gt(null, "some"));
        assertTrue(Comparison.string_gt("aaa222aaa", "aaa111aaa"));
        assertTrue(Comparison.string_gt("b", "a"));
        assertTrue(Comparison.string_gt("a11b22", "a11b3"));
    }

    @Test
    public void testCharSequenceGe() {
        assertTrue(Comparison.string_ge("aaa111aaa", "aaa111aaa"));
        assertTrue(Comparison.string_ge(new String("11"), new String("11")));
        assertTrue(Comparison.string_ge(null, null));
        assertTrue(Comparison.string_ge("some", null));
        assertFalse(Comparison.string_ge(null, "some"));
        assertTrue(Comparison.string_ge("aaa222aaa", "aaa111aaa"));
        assertTrue(Comparison.string_ge("b", "a"));
        assertTrue(Comparison.string_ge("a11b22", "a11b3"));
    }

}

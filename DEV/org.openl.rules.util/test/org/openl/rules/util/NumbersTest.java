package org.openl.rules.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NumbersTest {
    private static Locale defaultLocale;

    @BeforeClass
    public static void setUp() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @AfterClass
    public static void tearDown() {
        Locale.setDefault(defaultLocale);
    }


    @Test
    public void toStringCheckNull() {
        assertNull(Numbers.toString(null));
        assertNull(Numbers.toString(null, ""));
        assertNull(Numbers.toString(1, null));
        assertNull(Numbers.toString(null, null));
    }

    @Test
    public void toStringForInteger() {
        int number100 = 100;
        assertEquals("100", Numbers.toString(number100));
        assertEquals("0", Numbers.toString(0));

        assertEquals("0100", Numbers.toString(number100, "0000"));
        assertEquals("0100.00", Numbers.toString(number100, "0000.00"));
        assertEquals("0100", Numbers.toString(number100, "0000.#"));
        assertEquals("100", Numbers.toString(number100, "#"));
        assertEquals("100", Numbers.toString(number100, "#.#"));
        int number12000 = 12000;
        assertEquals("12,000", Numbers.toString(number12000, "#,###.###"));
        assertEquals("1.2E4", Numbers.toString(number12000, "#0E0"));
        int numberMinus1 = -1;
        assertEquals("neg 001", Numbers.toString(numberMinus1, "000;neg 0"));
        int number3 = 3;
        assertEquals("003", Numbers.toString(number3, "000;neg 0"));
        assertEquals("300%", Numbers.toString(number3, "0%"));
        assertEquals("3000‰", Numbers.toString(number3, "0‰"));
        assertEquals("#3%", Numbers.toString(number3, "'#'0'%'"));
    }

    @Test
    public void toStringCheckException() {
        assertNull(Numbers.toString(10, "0.0.0"));
    }

    @Test
    public void toStringDouble() {
        double number100dot5 = 100.5;
        assertEquals("100.5", Numbers.toString(number100dot5));
        assertEquals("NaN", Numbers.toString(Double.NaN));
        assertEquals("∞", Numbers.toString(Double.POSITIVE_INFINITY));
        assertEquals("-∞", Numbers.toString(Double.NEGATIVE_INFINITY));
        assertEquals("0", Numbers.toString(0.0));
        assertEquals("1000", Numbers.toString(1000d));
        assertEquals("-1000", Numbers.toString(-1000d));
        assertEquals("0.01", Numbers.toString(0.01d));
        assertEquals("-0.01", Numbers.toString(-0.01d));

        double reallySmallValue = -2.2250738585072014E-308;
        assertEquals("-2.2250738585072014E-308", Numbers.toString(reallySmallValue));

        assertEquals("100.500", Numbers.toString(number100dot5, "0.000"));
        assertEquals("0100.500", Numbers.toString(number100dot5, "0000.000"));
        assertEquals("100", Numbers.toString(number100dot5, "#"));
        double number100dot51 = 100.51;
        assertEquals("100.5", Numbers.toString(number100dot51, "#.0"));
        double number10000dot5 = 10000.5;
        assertEquals("10,000.5", Numbers.toString(number10000dot5, "#,###.#"));
        double numberDot12 = 0.12;
        assertEquals("1.2E-1", Numbers.toString(numberDot12, "#.#E0"));
        double number1dot1 = 1.1;
        assertEquals("1.1", Numbers.toString(number1dot1, "#.#;neg 0"));
        double numberMinus1dot1 = -1.1;
        assertEquals("neg 1.1", Numbers.toString(numberMinus1dot1, "#.#;neg 0"));
        double numberDot03 = 0.03;
        assertEquals("3%", Numbers.toString(numberDot03, "0%"));
        assertEquals("30‰", Numbers.toString(numberDot03, "0‰"));
        double numberDot033 = 0.033;
        assertEquals("#3%%", Numbers.toString(numberDot033, "'#'0%'%'"));

        double reallyBigValue = 85070591730234620000000000000000000000D;
        assertEquals("8.507059173023462E37", Numbers.toString(reallyBigValue));
    }

    @Test
    public void toStringBigInteger() {
        BigInteger reallyBigValue = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE));
        assertEquals("0", Numbers.toString(BigInteger.ZERO));
        assertEquals("1", Numbers.toString(BigInteger.ONE));

        assertEquals("85070591730234615847396907784232501249", Numbers.toString(reallyBigValue));
        
        assertEquals("85070591730234615847396907784232501249.000", Numbers.toString(reallyBigValue, "0.000"));
        assertEquals("85,070591,730234,615847,396907,784232,501249.000", Numbers.toString(reallyBigValue, "#,######.000"));
        assertEquals("85E36", Numbers.toString(reallyBigValue, "#,#E0"));
        assertEquals("neg 85E36", Numbers.toString(reallyBigValue.negate(), "#,#E0;neg 0"));
        assertEquals("8507,059173,023461,584739,690778,423250,124900.0%", Numbers.toString(reallyBigValue, "#,######.0%"));
        assertEquals("85070,591730,234615,847396,907784,232501,249000.0‰", Numbers.toString(reallyBigValue, "#,######.0‰"));
        assertEquals("#85E36'", Numbers.toString(reallyBigValue, "'#'#,#E0''"));
    }

    @Test
    public void toStringBigDecimal() {
        BigDecimal reallyBigValue = BigDecimal.valueOf(Long.MAX_VALUE).multiply(BigDecimal.valueOf(Long.MAX_VALUE));
        assertEquals("85070591730234615847396907784232501249", Numbers.toString(reallyBigValue));
        BigDecimal reallyBigValueWithPoint = reallyBigValue.divide(BigDecimal.valueOf(1000));
        assertEquals("85070591730234615847396907784232501.249", Numbers.toString(reallyBigValueWithPoint));
        assertEquals("0", Numbers.toString(BigDecimal.ZERO));
        assertEquals("1", Numbers.toString(BigDecimal.ONE));
        assertEquals("1000", Numbers.toString(new BigDecimal("1000.000")));
        assertEquals("1000", Numbers.toString(new BigDecimal("1000")));
        assertEquals("100.001", Numbers.toString(new BigDecimal("100.00100")));

        BigDecimal reallySmallValue = BigDecimal.valueOf(-2.2250738585072014E-307);
        assertEquals("-2.2250738585072014E-307", Numbers.toString(reallySmallValue));

        assertEquals("85070591730234615847396907784232501.249", Numbers.toString(reallyBigValueWithPoint, "0.000"));
        assertEquals("85070,591730,234615,847396,907784,232501.249", Numbers.toString(reallyBigValueWithPoint, "#,######.000"));
        assertEquals("8.5E34", Numbers.toString(reallyBigValueWithPoint, "#,#E0"));
        assertEquals("neg 8.5E34", Numbers.toString(reallyBigValueWithPoint.negate(), "#,#E0;neg 0"));
        assertEquals("8,507059,173023,461584,739690,778423,250124.9%", Numbers.toString(reallyBigValueWithPoint, "#,######.0%"));
        assertEquals("85,070591,730234,615847,396907,784232,501249.0‰", Numbers.toString(reallyBigValueWithPoint, "#,######.0‰"));
        assertEquals("#8.5E34'", Numbers.toString(reallyBigValueWithPoint, "'#'#,#E0''"));
    }

    @Test
    public void toStringByte() {
        byte number = 124;
        byte negativeNumber = -124;
        assertEquals("124", Numbers.toString(number));
        assertEquals("-124", Numbers.toString(negativeNumber));
    }

    @Test
    public void toStringShort() {
        short number = 1025;
        short negativeNumber = -1240;

        assertEquals("1025", Numbers.toString(number));
        assertEquals("-1240", Numbers.toString(negativeNumber));
    }

    @Test
    public void testLong() {
        long number = 1234567890123L;
        long negativeNumber = -1234567890123L;

        assertEquals("1234567890123", Numbers.toString(number));
        assertEquals("-1234567890123", Numbers.toString(negativeNumber));
        assertEquals("0", Numbers.toString(0L));
    }

    @Test
    public void toStringFloat() {
        float number100dot5 = 100.5f;
        assertEquals("100.5", Numbers.toString(number100dot5));
        assertEquals("NaN", Numbers.toString(Float.NaN));
        assertEquals("∞", Numbers.toString(Float.POSITIVE_INFINITY));
        assertEquals("-∞", Numbers.toString(Float.NEGATIVE_INFINITY));
        assertEquals("0", Numbers.toString(0.0f));
        assertEquals("1000", Numbers.toString(1000f));
        assertEquals("-1000", Numbers.toString(-1000f));
        assertEquals("0.01", Numbers.toString(0.01f));
        assertEquals("-0.01", Numbers.toString(-0.01f));

        float reallySmallValue = -25E-10f;
        assertEquals("-2.5E-9", Numbers.toString(reallySmallValue));

        assertEquals("1.2345", Numbers.toString(1.2345));

        assertEquals("100.500", Numbers.toString(number100dot5, "0.000"));
        assertEquals("0100.500", Numbers.toString(number100dot5, "0000.000"));
        assertEquals("100", Numbers.toString(number100dot5, "#"));
        float number100dot51 = 100.51f;
        assertEquals("100.5", Numbers.toString(number100dot51, "#.0"));
        float number10000dot5 = 10000.5f;
        assertEquals("10,000.5", Numbers.toString(number10000dot5, "#,###.#"));
        float numberDot12 = 0.12f;
        assertEquals("1.2E-1", Numbers.toString(numberDot12, "#.#E0"));
        float number1dot1 = 1.1f;
        assertEquals("1.1", Numbers.toString(number1dot1, "#.#;neg 0"));
        float numberMinus1dot1 = -1.1f;
        assertEquals("neg 1.1", Numbers.toString(numberMinus1dot1, "#.#;neg 0"));
        float numberDot03 = 0.03f;
        assertEquals("3%", Numbers.toString(numberDot03, "0%"));
        assertEquals("30‰", Numbers.toString(numberDot03, "0‰"));
        float numberDot033 = 0.033f;
        assertEquals("#3%%", Numbers.toString(numberDot033, "'#'0%'%'"));

        float reallyBigValue = 85070590000000000000000000000000000000f;
        assertEquals("8.507059E37", Numbers.toString(reallyBigValue));
    }
}

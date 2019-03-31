package org.openl.rules.binding;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.ShortValue;
import org.openl.types.java.JavaOpenClass;

/**
 * Created by dl on 5/4/15.
 */
public class StringCastsTest {

    private CastFactory factory;

    @Before
    public void before() {
        factory = new CastFactory();
        factory.setMethodFactory(JavaOpenClass.getOpenClass(StringOperators.class));
    }

    @Test
    public void testStringToByte() {
        javaCastTest(String.class, byte.class);
        javaCastTest(String.class, Byte.class);
    }

    @Test
    public void testStringToShort() {
        javaCastTest(String.class, short.class);
        javaCastTest(String.class, Short.class);
    }

    @Test
    public void testStringToInt() {
        javaCastTest(String.class, int.class);
        javaCastTest(String.class, Integer.class);
    }

    @Test
    public void testStringToFloat() {
        javaCastTest(String.class, float.class);
        javaCastTest(String.class, Float.class);
    }

    @Test
    public void testStringToDouble() {
        javaCastTest(String.class, double.class);
        javaCastTest(String.class, Double.class);
    }

    @Test
    public void testStringToBigInt() {
        javaCastTest(String.class, BigInteger.class);
    }

    @Test
    public void testStringToBigDecimal() {
        javaCastTest(String.class, BigDecimal.class);
    }

    @Test
    public void testStringToByteValue() {
        javaCastTest(String.class, ByteValue.class);
    }

    @Test
    public void testStringToShortValue() {
        javaCastTest(String.class, ShortValue.class);
    }

    @Test
    public void testStringToIntValue() {
        javaCastTest(String.class, IntValue.class);
    }

    @Test
    public void testStringToFloatValue() {
        javaCastTest(String.class, FloatValue.class);
    }

    @Test
    public void testStringToDoubleValue() {
        javaCastTest(String.class, DoubleValue.class);
    }

    @Test
    public void testStringToBigIntValue() {
        javaCastTest(String.class, BigIntegerValue.class);
    }

    @Test
    public void testStringToBigDecimalValue() {
        javaCastTest(String.class, BigDecimalValue.class);
    }

    void javaCastTest(Class<?> from, Class<?> to) {
        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(from), JavaOpenClass.getOpenClass(to));
        assertNotNull(cast);
        assertTrue(cast.isImplicit());
    }

}

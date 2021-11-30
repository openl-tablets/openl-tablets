package org.openl.rules.binding;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
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

    void javaCastTest(Class<?> from, Class<?> to) {
        IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(from), JavaOpenClass.getOpenClass(to));
        assertNotNull(cast);
        assertTrue(cast.isImplicit());
    }

}

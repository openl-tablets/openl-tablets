package org.openl.rules.convertor;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;
import org.openl.rules.convertor.ObjectToDataConvertorFactory.MatchedConstructorConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory.StaticMethodConvertor;

public class ObjectToDataConvertorFactoryTest {

    @Test
    public void testInteger2Double() {
        IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(Double.class, Integer.class);
        assertNotNull(convertor);
        assertTrue(convertor instanceof MatchedConstructorConvertor);

        Double value = (Double) convertor.convert(2333);
        assertEquals(2333, value, 0);
    }

    @Test
    public void testDoublePrimitive2BigDecimal() {
        // when converting from double to BigDecimal, converter that uses BigDecimal.valueOf(double a) method will be
        // used.
        //
        IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(BigDecimal.class, double.class);
        assertNotNull(convertor);
        assertTrue(convertor instanceof StaticMethodConvertor);

        BigDecimal value = (BigDecimal) convertor.convert(23.5666);
        assertEquals("23.5666", value.toString());
    }

    @Test
    public void testDouble2BigDecimal() {
        // when converting from Double to BigDecimal, converter that uses BigDecimal(double a) constructor will be used.
        //
        IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(BigDecimal.class, Double.class);
        assertNotNull(convertor);
        assertTrue(convertor instanceof MatchedConstructorConvertor);

        Double valueToConvert = 23.5666;

        BigDecimal value = (BigDecimal) convertor.convert(valueToConvert);
        assertEquals(valueToConvert, value.doubleValue(), 0);
    }

    @Test
    public void testDoublePrimitive2String() {
        IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(String.class, double.class);
        assertNotNull(convertor);
        assertTrue(convertor instanceof StaticMethodConvertor);

        assertEquals("13.356", convertor.convert(13.356));
    }

    @Test
    public void testDouble2String() {
        IObjectToDataConvertor convertor = ObjectToDataConvertorFactory.getConvertor(String.class, Double.class);
        assertNotNull(convertor);
        assertTrue(convertor instanceof StaticMethodConvertor);

        assertEquals("13.356", convertor.convert(13.356));
    }
}

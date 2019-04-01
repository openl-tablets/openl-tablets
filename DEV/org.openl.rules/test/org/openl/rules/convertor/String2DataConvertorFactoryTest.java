package org.openl.rules.convertor;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * @author NSamatov.
 */
public class String2DataConvertorFactoryTest {
    @Test
    public void testUnregisterClassLoader() throws Exception {
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(String.class);
        IString2DataConvertor convertorArray = String2DataConvertorFactory.getConvertor(String[].class);

        assertSame(convertor, String2DataConvertorFactory.getConvertor(String.class));
        assertSame(convertorArray, String2DataConvertorFactory.getConvertor(String[].class));

        String2DataConvertorFactory.unregisterClassLoader(String.class.getClassLoader());

        assertSame(convertor, String2DataConvertorFactory.getConvertor(String.class));
        assertNotSame(convertorArray, String2DataConvertorFactory.getConvertor(String[].class));
    }
}

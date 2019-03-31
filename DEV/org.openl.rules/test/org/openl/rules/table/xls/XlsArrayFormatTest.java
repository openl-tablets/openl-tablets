package org.openl.rules.table.xls;

import static org.junit.Assert.*;
import org.junit.Test;
import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;

public class XlsArrayFormatTest {

    public static enum TestConstants {
        TEST_CONST_1("Test Constant 1"),
        TEST_CONST_2("Test Constant 2");

        private final String displayName;

        private TestConstants(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Test
    public void testParse() {
        ArrayFormatter arrayFormat = new ArrayFormatter(new DefaultFormatter());
        String value = "tag1,tag2,tag3";

        Object result = arrayFormat.parse(value);
        assertNotNull(result);

        String[] resStr = ((String[]) result);
        assertTrue(resStr.length == 3);
        assertEquals("tag1", resStr[0]);
        assertEquals("tag2", resStr[1]);
        assertEquals("tag3", resStr[2]);
    }

    @Test
    public void testParseEmptyString() {
        ArrayFormatter arrayFormat = new ArrayFormatter(new DefaultFormatter());
        String value = null;

        Object result = arrayFormat.parse(value);
        assertNull(result);
    }

    @Test
    public void testFormatEnums() {
        ArrayFormatter arrayFormat = new ArrayFormatter(new EnumFormatter(TestConstants.class));

        TestConstants[] arrayEnum = new TestConstants[2];

        arrayEnum[0] = TestConstants.TEST_CONST_1;
        arrayEnum[1] = TestConstants.TEST_CONST_2;

        String result = arrayFormat.format(arrayEnum);
        assertNotNull(result);
        assertEquals(TestConstants.TEST_CONST_1.name() + "," + TestConstants.TEST_CONST_2.name(), result);
    }

    @Test
    public void testFormatNull() {
        ArrayFormatter arrayFormat = new ArrayFormatter(new EnumFormatter(TestConstants.class));

        TestConstants[] arrayEnum = new TestConstants[2];

        arrayEnum[0] = TestConstants.TEST_CONST_1;
        arrayEnum[1] = TestConstants.TEST_CONST_2;

        String result = arrayFormat.format(null);

        assertNull(result);
    }
}

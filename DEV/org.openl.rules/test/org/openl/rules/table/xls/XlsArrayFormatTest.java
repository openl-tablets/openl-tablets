package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import org.openl.rules.table.formatters.ArrayFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;

class XlsArrayFormatTest {

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public enum TestConstants {
        TEST_CONST_1("Test Constant 1"),
        TEST_CONST_2("Test Constant 2");

        private final String displayName;

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Test
    void testParse() {
        var arrayFormat = new ArrayFormatter(new DefaultFormatter(), String.class);
        var value = "tag1,tag2,tag3";

        var result = arrayFormat.parse(value);
        assertNotNull(result);

        var resStr = (String[]) result;
        assertEquals(3, resStr.length);
        assertEquals("tag1", resStr[0]);
        assertEquals("tag2", resStr[1]);
        assertEquals("tag3", resStr[2]);
    }

    @Test
    void testParseEmptyString() {
        var arrayFormat = new ArrayFormatter(new DefaultFormatter(), String.class);
        String value = null;

        var result = arrayFormat.parse(value);
        assertNull(result);
    }

    @Test
    void testFormatEnums() {
        var arrayFormat = new ArrayFormatter(new EnumFormatter(TestConstants.class), TestConstants.class);

        TestConstants[] arrayEnum = new TestConstants[2];

        arrayEnum[0] = TestConstants.TEST_CONST_1;
        arrayEnum[1] = TestConstants.TEST_CONST_2;

        var result = arrayFormat.format(arrayEnum);
        assertNotNull(result);
        assertEquals(TestConstants.TEST_CONST_1.name() + "," + TestConstants.TEST_CONST_2.name(), result);
    }

    @Test
    void testFormatNull() {
        var arrayFormat = new ArrayFormatter(new EnumFormatter(TestConstants.class), TestConstants.class);

        TestConstants[] arrayEnum = new TestConstants[2];

        arrayEnum[0] = TestConstants.TEST_CONST_1;
        arrayEnum[1] = TestConstants.TEST_CONST_2;

        var result = arrayFormat.format(null);

        assertNull(result);
    }
}

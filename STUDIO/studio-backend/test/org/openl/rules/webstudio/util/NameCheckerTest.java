package org.openl.rules.webstudio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class NameCheckerTest {

    @Test
    void testCheckNameBad() {
        final String[] badNames = {"\\f",
                "f/g",
                "f*",
                "?f*",
                "g?",
                "f:x",
                "f;x",
                "a<b",
                "a>b",
                "a|b",
                "a\"b",
                "a\tb",
                "a\nv",
                "a%b",
                " test",
                "test ",
                "test.",
                "test..",
                "test..."};

        for (String s : badNames) {
            var res = NameChecker.checkName(s);
            if (res) {
                fail("wrong name passed check: " + s);
            }
        }
    }

    /** Windows reserves a device name in any letter case and with any extension. */
    @Test
    void reservesADeviceNameInAnyCaseAndWithAnyExtension() {
        for (String name : new String[]{"CON", "con", "Con.xlsx", "lpt1.txt", "NUL.tar.gz"}) {
            assertTrue(NameChecker.isReservedName(name), name);
        }
        for (String name : new String[]{"Console", "icon.xlsx", "COM10", "rules.con"}) {
            assertFalse(NameChecker.isReservedName(name), name);
        }
    }

    @Test
    void findsTheReservedNameOfAPath() {
        assertEquals("Con.xlsx", NameChecker.findReservedName("rules/Con.xlsx"));
        assertEquals("aux", NameChecker.findReservedName("aux\\Main.xlsx"));
        assertNull(NameChecker.findReservedName("rules/Main.xlsx"));
    }

    @Test
    void testCheckNameOk() {
        final String[] okNames = {"a-b", "1,2,3", "hello world", "PublicClass$InnerClass"};

        for (String s : okNames) {
            assertTrue(NameChecker.checkName(s));
        }
    }
}

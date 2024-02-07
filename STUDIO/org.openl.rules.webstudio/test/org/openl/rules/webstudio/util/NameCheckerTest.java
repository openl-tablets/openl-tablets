package org.openl.rules.webstudio.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class NameCheckerTest {

    @Test
    public void testCheckNameBad() {
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
            boolean res = NameChecker.checkName(s);
            if (res) {
                fail("wrong name passed check: " + s);
            }
        }
    }

    @Test
    public void testCheckNameOk() {
        final String[] okNames = {"a-b", "1,2,3", "hello world", "PublicClass$InnerClass"};

        for (String s : okNames) {
            assertTrue(NameChecker.checkName(s));
        }
    }
}

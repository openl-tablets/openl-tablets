package org.openl.rules.ui.repository;

import junit.framework.TestCase;

public class RepositoryTreeControllerTestCase extends TestCase {
    private RepositoryTreeController instance;

    @Override
    protected void setUp() throws Exception {
        instance = new RepositoryTreeController();
    }

    public void testCheckNameBad() {
        final String[] badNames = {
                "\\f","f/g", "f*", "?f*", "g?","f:x", "f;x","a<b", "a>b", "a\tb", "a\nv", "a$", "a%b"
        };

        for (String s : badNames) {
            boolean res = instance.checkName(s);
            if (res) {
                fail("wrong name passed check: " + s);
            }
        }
    }

    public void testCheckNameOk() {
        final String[] okNames = {
                "a-b", "1,2,3", "hello world"
        };

        for (String s: okNames)
            assertTrue(instance.checkName(s));
    }
}

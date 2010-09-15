package org.openl.rules.webstudio.repository;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.RepositoryTreeController;

import junit.framework.TestCase;

public class RepositoryTreeControllerTestCase extends TestCase {
    private RepositoryTreeController instance;

    @Override
    protected void setUp() throws Exception {
        instance = new RepositoryTreeController();
    }

    public void testCheckNameBad() {
        final String[] badNames = { "\\f", "f/g", "f*", "?f*", "g?", "f:x", "f;x", "a<b", "a>b", "a\tb", "a\nv", "a%b",
                " test", "test ", "test.", "test..", "test..." };

        for (String s : badNames) {
            boolean res = NameChecker.checkName(s);
            if (res) {
                fail("wrong name passed check: " + s);
            }
        }
    }

    public void testCheckNameOk() {
        final String[] okNames = { "a-b", "1,2,3", "hello world", "PublicClass$InnerClass" };

        for (String s : okNames) {
            assertTrue(NameChecker.checkName(s));
        }
    }
}

package org.openl.ie.constrainer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

    static final Class[] _testClasses = { IntExpMulExpTest.class };

    public static Test suite() {
        TestSuite suite = new TestSuite();
        for (Class testClass : _testClasses) {
            suite.addTestSuite(testClass);
        }
        return suite;
    }

    public AllTests(String name) {
        super(name);
    }

}

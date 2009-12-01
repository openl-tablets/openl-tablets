package com.exigen.poi.array.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestArrays {
    @org.junit.Test
    public static Test suite() {
        TestSuite suite = new TestSuite(TestArrays.class.getName());
        	suite.addTest(ArrayFormulaSetRemoveTests.suite());
        	suite.addTest(ArrayEvalTests.suite());
        return suite;
    }

}

package com.exigen.poi.array.test;


import junit.framework.Test;
import junit.framework.TestSuite;


public class ArrayFormulaSetRemoveTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(ArrayFormulaSetRemoveTests.class.getName());
        	suite.addTest(ArrayFormulaSetRemoveTestNewFormat.suite());
        	suite.addTest(ArrayFormulaSetRemoveTestOldFormat.suite());
        return suite;
    }

}

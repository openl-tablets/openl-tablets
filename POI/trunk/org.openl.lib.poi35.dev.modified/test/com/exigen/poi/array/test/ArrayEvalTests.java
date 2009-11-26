package com.exigen.poi.array.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ArrayEvalTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(ArrayEvalTests.class.getName());
        	suite.addTest(ArrayTestNewFormat.suite());
        	suite.addTest(ArrayTestOldFormat.suite());
        	suite.addTest(ArrayFormulaEvalTestNewFormat.suite());
        	suite.addTest(ArrayFormulaEvalTestOldFormat.suite());
        	suite.addTest(ArrayFormulaFunctionTestNewFormat.suite());
           	suite.addTest(ArrayFormulaFunctionTestOldFormat.suite());
        return suite;
    }

}

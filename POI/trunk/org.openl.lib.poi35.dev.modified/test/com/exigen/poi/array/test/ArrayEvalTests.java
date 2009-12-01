package com.exigen.poi.array.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ArrayEvalTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(ArrayEvalTests.class.getName());
        	suite.addTest(ArrayTest2007.suite());
        	suite.addTest(ArrayTest2003.suite());
        	suite.addTest(ArrayFormulaEvalTest2007.suite());
        	suite.addTest(ArrayFormulaEvalTest2003.suite());
        	suite.addTest(ArrayFormulaFunctionTest2007.suite());
           	suite.addTest(ArrayFormulaFunctionTest2003.suite());
        return suite;
    }

}

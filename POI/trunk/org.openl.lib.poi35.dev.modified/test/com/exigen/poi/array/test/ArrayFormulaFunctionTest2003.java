package com.exigen.poi.array.test;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;

public class ArrayFormulaFunctionTest2003 extends ArrayFormulaFunctionTest{

	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayFormulaFunctionTest2003.class);
	
	
	@BeforeClass 
	public  static void readWorkbook(){
		th = new TestHelper();
		th.readWorkbook("ArrayFormulaFunctions.xls");
	}
	
	public static junit.framework.Test suite() {  
		return new JUnit4TestAdapter(ArrayFormulaFunctionTest2003.class);
	}
	
	public TestHelper th(){
		return th;
	}
}

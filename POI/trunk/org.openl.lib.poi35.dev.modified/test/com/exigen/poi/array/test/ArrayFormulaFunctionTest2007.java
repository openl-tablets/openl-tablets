package com.exigen.poi.array.test;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;

public class ArrayFormulaFunctionTest2007 extends ArrayFormulaFunctionTest{

	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayFormulaFunctionTest2007.class);
	
	
	@BeforeClass 
	public  static void readWorkbook(){
		th = new TestHelper();
		th.readWorkbook("ArrayFormulaFunctions.xlsx");
	}
	
	public static junit.framework.Test suite() {  
		return new JUnit4TestAdapter(ArrayFormulaFunctionTest2007.class);
	}
	
	public TestHelper th(){
		return th;
	}
}

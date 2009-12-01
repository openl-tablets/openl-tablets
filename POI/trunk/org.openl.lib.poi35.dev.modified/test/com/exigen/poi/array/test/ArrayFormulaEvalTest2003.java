package com.exigen.poi.array.test;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;

public class ArrayFormulaEvalTest2003 extends ArrayFormulaEvalTest{

	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayFormulaEvalTest2003.class);
	
	
	@BeforeClass 
	public  static void readWorkbook(){
		th = new TestHelper();
		th.readWorkbook("ArrayFormula.xls");
	}
	
	public static junit.framework.Test suite() {  
		return new JUnit4TestAdapter(ArrayFormulaEvalTest2003.class);
	}
	
	public TestHelper th(){
		return th;
	}
}
package com.exigen.poi.array.test;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.junit.BeforeClass;
import org.junit.Test;

public class ArrayFormulaEvalTest2007 extends ArrayFormulaEvalTest{

	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayFormulaEvalTest2007.class);
	
	
	@BeforeClass 
	public  static void readWorkbook(){
		th = new TestHelper();
		th.readWorkbook("ArrayFormula.xlsm");
	}
	
	public static junit.framework.Test suite() {  
		return new JUnit4TestAdapter(ArrayFormulaEvalTest2007.class);
	}
	
	public TestHelper th(){
		return th;
	}
	// Exclude after implementation
	@Test (expected=NotImplementedException.class)
	public void EvaluateMyFunctionInArrayContext() {
		EvaluateMyFunctionInArrayContextInt();
		}

}

package com.exigen.poi.array.test;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;

public class ArrayTest2003 extends ArrayTest{

	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayTest2003.class);
	
	
	@BeforeClass 
	public  static void readWorkbook(){
		th = new TestHelper();
		th.readWorkbook("ConstantArray.xls");
	}
	
	public static junit.framework.Test suite() {  
		return new JUnit4TestAdapter(ArrayTest2003.class);
	}
	
	public TestHelper th(){
		return th;
	}
}

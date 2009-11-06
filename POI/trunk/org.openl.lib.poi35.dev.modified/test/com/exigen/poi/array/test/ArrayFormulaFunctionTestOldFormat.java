package com.exigen.poi.array.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.BeforeClass;
import org.junit.Test;

public class ArrayFormulaFunctionTestOldFormat {

	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayFormulaFunctionTestOldFormat.class);
	
	@BeforeClass
	public  static void readWorkbook(){

		URL url = ArrayTest.class.getClassLoader().getResource("ArrayFormulaFunctions.xls");
		Workbook wb;
		try {
			wb =  new HSSFWorkbook(new FileInputStream(url.getFile()));
		} catch (IOException ioe){
			log.error("Failed to open test workbook from file:" + url.getFile(), ioe );
			throw new IllegalArgumentException(url.getFile());
		}
		th = new TestHelper(wb);
	}
	
	
	// check if non-calculated array cells does not contain value
	@Test(expected=IllegalArgumentException.class)
	public void nonCalculated1(){
		th.getNumericValue("C4");
	}
	
	@Test
	public void horizontalArray1(){
		assertEquals("C4-I4",th.getNumericValue("I4"), th.calculateNumericFormula("C4"), 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nonCalculated2(){
		th.getNumericValue("D4");
	}

	@Test
	public void horizontalArray2(){
		assertEquals("D4-J4",th.getNumericValue("J4"), th.calculateNumericFormula("D4"), 0);
		assertEquals("E4-K4",th.getNumericValue("K4"), th.calculateNumericFormula("E4"), 0);
	}
	
	@Test 
	public void horizontalArray3(){
		assertTrue("C4:E4 - I4:K4",th.calculateAndCompareNumericArray('C', 4, 'E', 4, 'I', 4, 0));
	}
	
	@Test 
	public void twoDimArray(){
		assertTrue("C6:E7 - I6:K7",th.calculateAndCompareNumericArray('C', 6, 'E', 7, 'I', 6, 0));
	}
	
	@Test 
	public void VertArray(){
		assertTrue("C9:C11 - I9:I11",th.calculateAndCompareNumericArray('C', 9, 'C', 11, 'I', 9, 0));
	}
	
	@Test 
	public void powerArray(){
		assertTrue("C16:D16 - I16:K16",th.calculateAndCompareNumericArray('C', 16, 'D', 16, 'I', 16, 0));
	}
	
	@Test 
	public void powerArrayChanged(){
		th.setNumericValue("A15", 2);
		assertEquals("changed array", 4, th.calculateNumericFormula("C16"),0);
	}

	@Test 
	public void indexArray(){
		assertTrue("C23:C25 - I23:I25",th.calculateAndCompareNumericArray('C', 23, 'C', 25, 'I', 23, 0));
	}

	@Test 
	public void matrixMultiply(){
		assertTrue("C33:D34 - I33:K34",th.calculateAndCompareNumericArray('C', 33, 'D', 34, 'I', 33, 0));
	}
	
	@Test
	public void sumOnMultiply(){
		assertEquals("C41",th.getNumericValue("I41"), th.calculateNumericFormula("C41"), 0);
	}
	
	@Test
	public void sumOnRangeArray(){
		assertEquals("C48",th.getNumericValue("I48"), th.calculateNumericFormula("C48"), 0);
	}
	
	@Test
	public void sumOnTwoArraya(){
		assertEquals("C50",th.getNumericValue("I50"), th.calculateNumericFormula("C50"), 0);
	}
	
	@Test 
	public void arrayMultiply(){
		assertTrue("C55:F57 - I55:L57",th.calculateAndCompareNumericArray('C', 55, 'F', 57, 'I', 55, 0));
	}
	
	
	@Test
	public void sumOnLen(){
		assertEquals("C63",th.getNumericValue("I63"), th.calculateNumericFormula("C63"), 0);
	}	

	@Test 
	public void smallRangeArray(){
		assertTrue("C69:D69",th.calculateAndCompareNumericArray('C', 69, 'D', 69, 'I', 69, 0));
	}	

	@Test
	public void averageOnSmall(){
		assertEquals("C72",th.getNumericValue("I72"), th.calculateNumericFormula("C72"), 0);
	}

	@Test
	public void sumOnIf(){
		assertEquals("C75",th.getNumericValue("I75"), th.calculateNumericFormula("C75"), 0);
	}
	
	@Test
	public void sumOnComplexIf(){
		assertEquals("C78",th.getNumericValue("I78"), th.calculateNumericFormula("C78"), 0);
	}	

	@Test
	public void sumOnComplexIf2(){
		assertEquals("C84",th.getNumericValue("I84"), th.calculateNumericFormula("C84"), 0);
	}	
	
	@Test 
	public void column(){
		assertTrue("C87:F87",th.calculateAndCompareNumericArray('C', 87, 'F', 87, 'I', 87, 0));
	}	

	@Test 
	public void row(){
		assertTrue("C90:C94",th.calculateAndCompareNumericArray('C', 90, 'C', 94, 'I', 90, 0));
	}	

	@Test
	public void columnNonArray(){
		assertEquals("C88",th.getNumericValue("I88"), th.calculateNumericFormula("C88"), 0);
	}	
	
	@Test
	public void columnArray(){
		assertEquals("C89",th.getNumericValue("I89"), th.calculateNumericFormula("C89"), 0);
	}		
}

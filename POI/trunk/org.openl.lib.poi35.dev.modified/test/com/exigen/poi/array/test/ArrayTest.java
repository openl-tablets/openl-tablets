package com.exigen.poi.array.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.Test;

/*
 * Constant array test class
 * 
 * Workbook contains formulas and tests
 */
public class ArrayTest {
	
	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayTest.class);
	
	@BeforeClass
	public  static void readWorkbook(){

		URL url = ArrayTest.class.getClassLoader().getResource("ConstantArray.xlsx");
		XSSFWorkbook wb;
		try {
		wb = new XSSFWorkbook(url.getFile());
		} catch (IOException ioe){
			log.error("Failed to open test workbook from file:" + url.getFile(), ioe );
			throw new IllegalArgumentException(url.getFile());
		}
		th = new TestHelper(wb);
	}
	
	@Test
	public void testSingleArg(){
		
		assertEquals("D5-C5",th.getNumericValue("D5"), th.calculateNumericFormula("C5"), 0);
	}

	@Test
	public void testSingleArgOnArray(){
		
		assertEquals("D6-C6",th.getNumericValue("D6"), th.calculateNumericFormula("C6"), 0);
	}
	
	@Test
	public void testTwoArgFirstArray(){
		
		assertEquals("D9-C9",th.getNumericValue("D9"), th.calculateNumericFormula("C9"), 0);
	}
	
	@Test
	public void testTwoArgTwoArrays(){
		
		assertEquals("D10-C10",th.getNumericValue("D10"), th.calculateNumericFormula("C10"), 0);
	}	
	
	
	@Test
	public void testTwoArgFirstArrayResult(){
		
		assertEquals("D11-C11",th.getNumericValue("D11"), th.calculateNumericFormula("C11"), 0);
	}
	
	@Test
	public void testAggregateOnArray(){
		
		assertEquals("D13-C13",th.getNumericValue("D13"), th.calculateNumericFormula("C13"), 0);
	}
	
	@Test
	public void testAggregateOnTwoArrays(){
		
		assertEquals("D14-C14",th.getNumericValue("D14"), th.calculateNumericFormula("C14"), 0);
	}
	
	@Test
	public void testAggregateWithSpecialLast(){
		
		assertEquals("D15-C15",th.getNumericValue("D15"), th.calculateNumericFormula("C15"), 0);
	}
	
	@Test
	public void testAggregateWithSpecialLastAsArray(){
		
		assertEquals("D16-C16",th.getNumericValue("D16"), th.calculateNumericFormula("C16"), 0);
	}
	
	@Test
	public void testAggregateOnArrayFromSingle(){
		
		assertEquals("D19-C19",th.getNumericValue("D19"), th.calculateNumericFormula("C19"), 0);
	}
	
	@Test
	public void testAggregateOnTwoDimArrayFromSingle(){
		
		assertEquals("D20-C20",th.getNumericValue("D20"), th.calculateNumericFormula("C20"), 0);
	}
	
	@Test
	public void testAggregateOnArrayFromTwoArg(){
		
		assertEquals("D21-C21",th.getNumericValue("D21"), th.calculateNumericFormula("C21"), 0);
	}
	
	@Test
	public void testAggregateOnNonProperArray(){
		
		FormulaError fe = th.calculateNumericFormulaWithError("C22");
		assertEquals("C22",fe.getCode(), FormulaError.NA.getCode());
	}
	
	@Test
	public void testAggregateOnTwoArg(){
		
		assertEquals("D23-C23",th.getNumericValue("D23"), th.calculateNumericFormula("C23"), 0);
	}
	
	@Test
	public void testAggregateOnTwoArgWithLastSpecial(){
		
		assertEquals("D24-C24",th.getNumericValue("D24"), th.calculateNumericFormula("C24"), 0);
	}
	
	@Test
	public void testAggregateOnNonProperArrayViaSingle(){
		
		FormulaError fe = th.calculateNumericFormulaWithError("C25");
		assertEquals("C25",fe.getCode(), FormulaError.NA.getCode());
	}
	
	@Test
	public void SimpleNumericOneArg(){
		
		assertEquals("D27-C27",th.getNumericValue("D27"), th.calculateNumericFormula("C27"), 0);
	}
	
	@Test
	public void SimpleNumericTwoArg(){
		
		assertEquals("D28-C28",th.getNumericValue("D28"), th.calculateNumericFormula("C28"), 0);
	}
	
	@Test
	public void SimpleAgregate(){
		
		assertEquals("D29-C29",th.getNumericValue("D29"), th.calculateNumericFormula("C29"), 0);
	}
	

	@Test
	public void SimpleAgregateWithLast(){
		
		assertEquals("D30-C30",th.getNumericValue("D30"), th.calculateNumericFormula("C30"), 0);
	}
	

	
	
	
	@Test
	public void SimpleTextOnNumeric(){
		
		assertEquals("D43-C43",th.getNumericValue("D43"), th.calculateNumericFormula("C43"), 0);
	}
	
	@Test
	public void SingleArgTextReturnNum(){
		
		assertEquals("D44-C44",th.getNumericValue("D44"), th.calculateNumericFormula("C44"), 0);
	}
	
	@Test
	public void SingleArgOnArrayFromNum(){
		
		assertEquals("D45-C45",th.getNumericValue("D45"), th.calculateNumericFormula("C45"), 0);
	}
	
	@Test
	public void NumericAggregateOnArrayFromTextSingleArg(){
		
		assertEquals("D46-C46",th.getNumericValue("D46"), th.calculateNumericFormula("C46"), 0);
	}

	@Test
	public void NumericAggregateSigleArgtTextSingleArgTextArray(){
		
		assertEquals("D47-C47",th.getNumericValue("D47"), th.calculateNumericFormula("C47"), 0);
	}
	

	@Test
	public void SimpleMultiArgText(){
		
		assertEquals("D48-C48",th.getStringValue("D48"), th.calculateStringFormula("C48"));
	}
	
	@Test
	public void SimpleMultiArgTextOneArray(){
		
		assertEquals("D49-C49",th.getStringValue("D49"), th.calculateStringFormula("C49"));
	}

	@Test
	public void SimpleMultiArgTextTwoArraysSizeMatch(){
		
		assertEquals("D50-C50",th.getStringValue("D50"), th.calculateStringFormula("C50"));
	}
	
	@Test
	public void SimpleMultiArgTextTwoArraysSizeNotMatch(){
		
		assertEquals("D51-C51",th.getStringValue("D51"), th.calculateStringFormula("C51"));
	}
	
	@Test
	public void NumericAggregateOnTextArraySizeMatch(){
		
		assertEquals("D52-C52",th.getNumericValue("D52"), th.calculateNumericFormula("C52"), 0);
	}	

	@Test
	public void AggregateOnTextArraySizenotMatch(){
		
		FormulaError fe = th.calculateNumericFormulaWithError("C53");
		assertEquals("C53",fe.getCode(), FormulaError.NA.getCode());
	}
	
	@Test
	public void simpleXY(){
		
		assertEquals("D60-C60",th.getNumericValue("D60"), th.calculateNumericFormula("C60"), 0);
	}	

	@Test
	public void XYonArray(){
		
		assertEquals("D61-C61",th.getNumericValue("D61"), th.calculateNumericFormula("C61"), 0);
	}
	
	@Test
	public void XYonArrayFromFunction(){
		
		assertEquals("D62-C62",th.getNumericValue("D62"), th.calculateNumericFormula("C62"), 0);
	}	

	@Test
	public void XYonWrongArray(){
		
		FormulaError fe = th.calculateNumericFormulaWithError("C63");
		assertEquals("C63",fe.getCode(), FormulaError.NA.getCode());
	}
	
	@Test 
	public void andTrue(){
		assertEquals("C73 - D73", th.getBooleanValue("D73"), th.calculateBooleanFormula("C73"));
	}
	
	@Test 
	public void andFalse(){
		assertEquals("C74 - D74", th.getBooleanValue("D74"), th.calculateBooleanFormula("C74"));
	}
	
	@Test 
	public void orTrue(){
		assertEquals("C75 - D75", th.getBooleanValue("D75"), th.calculateBooleanFormula("C75"));
	}
	
	@Test 
	public void orFalse(){
		assertEquals("C76 - D76", th.getBooleanValue("D76"), th.calculateBooleanFormula("C76"));
	}
	
	
	@Test
	public void choose(){
		
		assertEquals("D82-C82",th.getNumericValue("D82"), th.calculateNumericFormula("C82"), 0);
	}	

	@Test
	public void chooseAggregate(){
		
		assertEquals("D83-C83",th.getNumericValue("D83"), th.calculateNumericFormula("C83"), 0);
	}	
	
	@Test
	public void vlookupArrayAsTable(){
		
		assertEquals("D88-C88",th.getNumericValue("D88"), th.calculateNumericFormula("C88"), 0);
	}	
	
	@Test
	public void vlookupAllArrays(){
		
		assertEquals("D89-C89",th.getNumericValue("D89"), th.calculateNumericFormula("C89"), 0);
	}	
	

	@Test
	public void hlookupArrayAsTable(){
		
		assertEquals("D95-C95",th.getNumericValue("D95"), th.calculateNumericFormula("C95"), 0);
	}	

	@Test
	public void hlookupAllArrays(){
		
		assertEquals("D96-C96",th.getNumericValue("D96"), th.calculateNumericFormula("C96"), 0);
	}	

	
	
	@Test
	public void lookupArray(){
		
		assertEquals("D101-C101",th.getNumericValue("D101"), th.calculateNumericFormula("C101"), 0);
	}	
	
	
	@Test
	public void lookupAllArrays(){
		
		assertEquals("D102-C102",th.getNumericValue("D102"), th.calculateNumericFormula("C102"), 0);
	}	
	

	@Test
	public void lookupAggregateArrays(){
		
		assertEquals("D103-C103",th.getNumericValue("D103"), th.calculateNumericFormula("C103"), 0);
	}	
		

	@Test
	public void countArray(){
		
		assertEquals("D108-C108",th.getNumericValue("D108"), th.calculateNumericFormula("C108"), 0);
	}	
	
	
	@Test
	public void countaArray(){
		
		assertEquals("D112-C112",th.getNumericValue("D112"), th.calculateNumericFormula("C112"), 0);
	}	
	

	@Test
	public void countifArray(){
		
		assertEquals("D117-C117",th.getNumericValue("D117"), th.calculateNumericFormula("C117"), 0);
	}	
	
	
	@Test
	public void countifArrayAggregate(){
		
		assertEquals("D118-C118",th.getNumericValue("D118"), th.calculateNumericFormula("C118"), 0);
	}	
	
	
	@Test
	public void sumifArray(){
		
		assertEquals("D124-C124",th.getNumericValue("D124"), th.calculateNumericFormula("C124"), 0);
	}	
	
	
	@Test
	public void sumifArrayAggregate(){
		
		assertEquals("D125-C125",th.getNumericValue("D125"), th.calculateNumericFormula("C125"), 0);
	}	

	@Test
	public void sumproductArray(){
		
		assertEquals("D130-C130",th.getNumericValue("D130"), th.calculateNumericFormula("C130"), 0);
	}
	

	@Test
	public void columnsArray(){
		
		assertEquals("D136-C136",th.getNumericValue("D136"), th.calculateNumericFormula("C136"), 0);
	}
	
	@Test
	public void matchArray(){
		
		assertEquals("D140-C140",th.getNumericValue("D140"), th.calculateNumericFormula("C140"), 0);
	}	
	
	
	@Test
	public void matchArrayAggregate(){
		
		assertEquals("D141-C141",th.getNumericValue("D141"), th.calculateNumericFormula("C141"), 0);
	}	
		
	@Test
	public void modeArray(){
		
		assertEquals("D145-C145",th.getNumericValue("D145"), th.calculateNumericFormula("C145"), 0);
	}	

	@Test
	public void offsetArray(){
		
		assertEquals("D149-C149",th.getNumericValue("D149"), th.calculateNumericFormula("C149"), 0);
	}	
	

	@Test
	public void rowsArray(){
		
		assertEquals("D154-C154",th.getNumericValue("D154"), th.calculateNumericFormula("C154"), 0);
	}	

	@Test
	public void indexArray(){
		
		assertEquals("D160-C160",th.getNumericValue("D160"), th.calculateNumericFormula("C160"), 0);
	}		

	@Test
	public void indexArrayEntireColumn(){
		
		assertEquals("D161-C161",th.getNumericValue("D161"), th.calculateNumericFormula("C161"), 0);
	}
	
	@Test
	public void indexArrayEntireColumnAggregate(){
		
		assertEquals("D162-C162",th.getNumericValue("D162"), th.calculateNumericFormula("C162"), 0);
	}
	

}

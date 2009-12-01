package com.exigen.poi.array.test;

import static org.junit.Assert.assertEquals;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.Test;

public abstract class ArrayFormulaEvalTest {

	abstract TestHelper th();
	
	@Test
	public void NumericSquareArrayFormula(){
		// Clean cell's values before calculation
		th().setNumericValue("C4", 0);
		th().setNumericValue("D4", 0);
		th().setNumericValue("C5", 0);
		th().setNumericValue("D5", 0);
		
		assertEquals("C4-F4",th().getNumericValue("F4"), th().calculateNumericFormula("C4"), 0);
		assertEquals("D4-G4",th().getNumericValue("G4"), th().calculateNumericFormula("D4"), 0);
		assertEquals("C5-F5",th().getNumericValue("F5"), th().calculateNumericFormula("C5"), 0);
		assertEquals("D5-G5",th().getNumericValue("G5"), th().calculateNumericFormula("D5"), 0);
	}
	
	
	@Test
	public void NumericArrayFormulaWORange(){
		
		// Clean cell's values before calculation
		th().setNumericValue("C7", 0);
		
		assertEquals("C7-F7",th().getNumericValue("F7"), th().calculateNumericFormula("C7"), 0);
	}
	
	@Test
	public void NumericArrayFormulaFullRow(){

		// Clean cell's values before calculation
		th().setNumericValue("A9", 0);
		th().setNumericValue("B9", 0);
		th().setNumericValue("C9", 0);
		assertEquals("A9-F9",th().getNumericValue("F9"), th().calculateNumericFormula("A9"), 0);
		assertEquals("B9-G9",th().getNumericValue("G9"), th().calculateNumericFormula("B9"), 0);
		assertEquals("C9-H9",th().getNumericValue("H9"), th().calculateNumericFormula("C9"), 0);
	}
	
	@Test
	public void NumericArrayFormulaSmallRow(){
		
		// Clean cell's values before calculation
		th().setNumericValue("A11", 0);
		th().setNumericValue("B11", 0);
		assertEquals("A11-F11",th().getNumericValue("F11"), th().calculateNumericFormula("A11"), 0);
		assertEquals("B11-G11",th().getNumericValue("G11"), th().calculateNumericFormula("B11"), 0);
	}
	
	@Test
	public void NumericArrayFormulaBigRow(){
		
		// Clean cell's values before calculation
		th().setNumericValue("A13", 0);
		th().setNumericValue("B13", 0);
		th().setNumericValue("C13", 0);
		
		assertEquals("A13-F13",th().getNumericValue("F13"), th().calculateNumericFormula("A13"), 0);
		assertEquals("B13-G13",th().getNumericValue("G13"), th().calculateNumericFormula("B13"), 0);
		assertEquals("C13-H13",th().getNumericValue("H13"), th().calculateNumericFormula("C13"), 0);
		assertEquals("D3-I13",th().getErrorValue("I13"), th().calculateNumericFormulaWithError("D13").getString());
	}
	
	@Test
	public void NumericArrayFormulaFewerRows(){
		
		// Clean cell's values before calculation
		th().setNumericValue("A16", 0);
		th().setNumericValue("B16", 0);
		th().setNumericValue("A17", 0);
		th().setNumericValue("B17", 0);

		assertEquals("A16-F16",th().getNumericValue("F16"), th().calculateNumericFormula("A16"), 0);
		assertEquals("B16-G16",th().getNumericValue("G16"), th().calculateNumericFormula("B16"), 0);
		assertEquals("A17-F17",th().getNumericValue("F17"), th().calculateNumericFormula("A17"), 0);
		assertEquals("B17-G17",th().getNumericValue("G17"), th().calculateNumericFormula("B17"), 0);
	}
	@Test
	public void NumericArrayFormulaDataExceed(){
		// Clean cell's values before calculation
		th().setNumericValue("A19", 0);
		th().setNumericValue("B19", 0);
		th().setNumericValue("A20", 0);
		th().setNumericValue("B20", 0);
		
		assertEquals("A19-F19",th().getNumericValue("F19"), th().calculateNumericFormula("A19"), 0);
		assertEquals("B19-G19",th().getNumericValue("G19"), th().calculateNumericFormula("B19"), 0);
		assertEquals("A20-F20",th().getNumericValue("F20"), th().calculateNumericFormula("A20"), 0);
		assertEquals("B20-G20",th().getNumericValue("G20"), th().calculateNumericFormula("B20"), 0);
	}
	@Test
	public void NumericArrayFormulaCol4Row(){
		
		// Clean cell's values before calculation
		th().setNumericValue("A22", 0);
		th().setNumericValue("A23", 0);
		th().setNumericValue("A24", 0);
		th().setNumericValue("A25", 0);
		
		assertEquals("A22-F22",th().getNumericValue("F22"), th().calculateNumericFormula("A22"), 0);
		assertEquals("A23-F23",th().getNumericValue("F23"), th().calculateNumericFormula("A23"), 0);
		assertEquals("A24-F24",th().getNumericValue("F24"), th().calculateNumericFormula("A24"), 0);
		assertEquals("A25-F25",th().getNumericValue("F25"), th().calculateNumericFormula("A25"), 0);
	}
	@Test
	public void NumericArrayFormulaRow4Col(){
		
		// Clean cell's values before calculation
		th().setNumericValue("A27", 0);
		th().setNumericValue("B27", 0);
		th().setNumericValue("C27", 0);
		th().setNumericValue("D27", 0);
	
		assertEquals("A27-F27",th().getNumericValue("F27"), th().calculateNumericFormula("A27"), 0);
		assertEquals("B27-G27",th().getNumericValue("G27"), th().calculateNumericFormula("B27"), 0);
		assertEquals("C27-H27",th().getNumericValue("H27"), th().calculateNumericFormula("C27"), 0);
		assertEquals("D27-I27",th().getNumericValue("I27"), th().calculateNumericFormula("D27"), 0);
	}
	@Test
	public void NumericArrayFormulaDataRow4Col(){
		
		// Clean cell's values before calculation
		th().setNumericValue("A27", 0);
		th().setNumericValue("B27", 0);
		th().setNumericValue("C27", 0);
		th().setNumericValue("D27", 0);
		
		assertEquals("A27-F27",th().getNumericValue("F27"), th().calculateNumericFormula("A27"), 0);
		assertEquals("B27-G27",th().getNumericValue("G27"), th().calculateNumericFormula("B27"), 0);
		assertEquals("C27-H27",th().getNumericValue("H27"), th().calculateNumericFormula("C27"), 0);
		assertEquals("D27-I27",th().getNumericValue("I27"), th().calculateNumericFormula("D27"), 0);
	}
	@Test
	public void NumericArrayFormulaDataShortage(){
		
		// Clean cell's values before calculation
		th().setNumericValue("B30", 0);
		th().setNumericValue("C30", 0);
//		th().setNumericValue("D30", 0);
		th().setNumericValue("B31", 0);
		th().setNumericValue("C31", 0);
//		th().setNumericValue("D31", 0);
//		th().setNumericValue("B32", 0);
//		th().setNumericValue("C32", 0);
//		th().setNumericValue("D32", 0);

		assertEquals("C30-G30",th().getNumericValue("G30"), th().calculateNumericFormula("C30"), 0);
		assertEquals("B30-F30",th().getNumericValue("F30"), th().calculateNumericFormula("B30"), 0);
//		assertEquals("C30-G30",th().getNumericValue("G30"), th().getNumericValue("C30"), 0);
		assertEquals("B31-G30",th().getNumericValue("F31"), th().calculateNumericFormula("B31"), 0);
		assertEquals("C31-G31",th().getNumericValue("G31"), th().calculateNumericFormula("C31"), 0);
		assertEquals("D30-H30",th().getErrorValue("H30"), th().calculateNumericFormulaWithError("D30").getString());
		assertEquals("D31-H31",th().getErrorValue("H31"), th().calculateNumericFormulaWithError("D31").getString());
		
		assertEquals("B32-F32",th().getErrorValue("F32"), th().calculateNumericFormulaWithError("B32").getString());
		assertEquals("C32-G32",th().getErrorValue("G32"), th().calculateNumericFormulaWithError("C32").getString());
		assertEquals("D32-H32",th().getErrorValue("H32"), th().calculateNumericFormulaWithError("D32").getString());

	}
	
	@Test
	public void NumericArrayFormulaRefArguments(){

		// Clean cell's values before calculation
		th().setNumericValue("A37", 0);
		th().setNumericValue("B37", 0);
		th().setNumericValue("C37", 0);
		
		assertEquals("A37-F37",th().getNumericValue("F37"), th().calculateNumericFormula("A37"), 0);
		assertEquals("B37-G37",th().getNumericValue("G37"), th().calculateNumericFormula("B37"), 0);
		assertEquals("C37-H37",th().getNumericValue("H37"), th().calculateNumericFormula("C37"), 0);
	}
	@Test
	public void NumericArrayFormulasRefArguments(){

		// Clean cell's values before calculation
		th().setNumericValue("A40", 0);
		th().setNumericValue("B40", 0);
		th().setNumericValue("C40", 0);
		
		assertEquals("A40-F40",th().getNumericValue("F40"), th().calculateNumericFormula("A40"), 0);
		assertEquals("B40-G40",th().getNumericValue("G40"), th().calculateNumericFormula("B40"), 0);
		assertEquals("C40-H40",th().getNumericValue("H40"), th().calculateNumericFormula("C40"), 0);
	}
	
	@Test
	public void NumericOperation4Range(){

		// Clean cell's values before calculation
		th().setNumericValue("C43", 0);
		th().setNumericValue("C44", 0);
		th().setNumericValue("C45", 0);
		th().setNumericValue("C46", 0);
		
		assertEquals("C43-F43",th().getNumericValue("F43"), th().calculateNumericFormula("C43"), 0);
		assertEquals("C44-F44",th().getNumericValue("F44"), th().calculateNumericFormula("C44"), 0);
		assertEquals("C45-F45",th().getNumericValue("F45"), th().calculateNumericFormula("C45"), 0);
		assertEquals("C46-F46",th().getNumericValue("F46"), th().calculateNumericFormula("C46"), 0);
	}
	
	@Test
	public void NumericOperation4DiffRanges(){

		// Clean cell's values before calculation
		th().setNumericValue("C48", 0);
		th().setNumericValue("C49", 0);
		th().setNumericValue("C50", 0);
		th().setNumericValue("C51", 0);
		
		assertEquals("C48-F48",th().getNumericValue("F48"), th().calculateNumericFormula("C48"), 0);
		assertEquals("C49-F49",th().getNumericValue("F49"), th().calculateNumericFormula("C49"), 0);
		assertEquals("C50-F50",th().getNumericValue("F50"), th().calculateNumericFormula("C50"), 0);
		assertEquals("C51-F51",th().getNumericValue("F51"), th().calculateNumericFormula("C51"), 0);
//		assertEquals("C50-F50",th().getErrorValue("F50"), th().getErrorValue("C50"));
//		assertEquals("C51-F51",th().getErrorValue("F51"), th().getErrorValue("C51"));
	}
	@Test
	public void NumericArrayChangeRefArguments(){

		// Clean cell's values before calculation
		th().setNumericValue("A40", 0);
		th().setNumericValue("B40", 0);
		th().setNumericValue("C40", 0);
		
		assertEquals("A40-F40",th().getNumericValue("F40"), th().calculateNumericFormula("A40"), 0);
		assertEquals("B40-G40",th().getNumericValue("G40"), th().calculateNumericFormula("B40"), 0);
		assertEquals("C40-H40",th().getNumericValue("H40"), th().calculateNumericFormula("C40"), 0);
		Cell cell = th().getCell("B40");
//		evaluator.clearAllCachedResultValues();
		th().setNumericValue("A41", 0.4);
		th().setNumericValue("B41", 0.5);
		th().setNumericValue("C41", 0.6);
		cell = th().getCell("B40");
		assertEquals("B40-G40",Math.cos(Math.sin(0.5)), th().calculateNumericFormula("B40"), 0);
		assertEquals("A40-F40",Math.cos(Math.sin(0.4)), th().calculateNumericFormula("A40"), 0);
		assertEquals("C40-H40",Math.cos(Math.sin(0.6)), th().calculateNumericFormula("C40"), 0);
	}
	@Test
	public void NumericArrayDifTypeArguments(){

		// Clean cell's values before calculation
		th().setNumericValue("A54", 0);
		th().setNumericValue("B54", 0);
		th().setNumericValue("C54", 0);
		th().setNumericValue("A55", 0);
		th().setNumericValue("B55", 0);
		th().setNumericValue("C55", 0);
		
		assertEquals("A54-F54",th().getNumericValue("F54"), th().calculateNumericFormula("A54"), 0);
		assertEquals("B54-G54",th().getNumericValue("G54"), th().calculateNumericFormula("B54"), 0);
		assertEquals("C54-H54",th().getNumericValue("H54"), th().calculateNumericFormula("C54"), 0);
		assertEquals("A55-F55",th().getNumericValue("F55"), th().calculateNumericFormula("A55"), 0);
		assertEquals("B55-G55",th().getNumericValue("G55"), th().calculateNumericFormula("B55"), 0);
		assertEquals("C55-H55",th().getNumericValue("H55"), th().calculateNumericFormula("C55"), 0);
	}

}


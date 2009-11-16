package com.exigen.poi.array.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;

import static org.junit.Assert.*;

/*
 * Constant array test class
 * 
 * Workbook contains formulas and tests
 */
public class ArrayFormulaTestOldFormat {
	
	private static Workbook wb;
	private static FormulaEvaluator evaluator = null;
	static Log log = LogFactory.getLog(ArrayFormulaTestOldFormat.class);
	
	@BeforeClass
	public  static void readWorkbook(){

		URL url = ArrayFormulaTestOldFormat.class.getClassLoader().getResource("ArrayFormula.xls");
		try {
			wb =  new HSSFWorkbook(new FileInputStream(url.getFile()));
		} catch (IOException ioe){
			log.error("Failed to open test workbook from file:" + url.getFile(), ioe );
			throw new IllegalArgumentException(url.getFile());
		}

	}
	
	
	@Test
	public void NumericSquareArrayFormula(){
		// Clean cell's values before calculation
		setNumericValue("C4", 0);
		setNumericValue("D4", 0);
		setNumericValue("C5", 0);
		setNumericValue("D5", 0);
		
		assertEquals("C4-F4",getNumericValue("F4"), calculateNumericFormula("C4"), 0);
		assertEquals("D4-G4",getNumericValue("G4"), calculateNumericFormula("D4"), 0);
		assertEquals("C5-F5",getNumericValue("F5"), calculateNumericFormula("C5"), 0);
		assertEquals("D5-G5",getNumericValue("G5"), calculateNumericFormula("D5"), 0);
	}
	
	
	@Test
	public void NumericArrayFormulaWORange(){
		
		// Clean cell's values before calculation
		setNumericValue("C7", 0);
		
		assertEquals("C7-F7",getNumericValue("F7"), calculateNumericFormula("C7"), 0);
	}
	
	@Test
	public void NumericArrayFormulaFullRow(){

		// Clean cell's values before calculation
		setNumericValue("A9", 0);
		setNumericValue("B9", 0);
		setNumericValue("C9", 0);
		assertEquals("A9-F9",getNumericValue("F9"), calculateNumericFormula("A9"), 0);
		assertEquals("B9-G9",getNumericValue("G9"), calculateNumericFormula("B9"), 0);
		assertEquals("C9-H9",getNumericValue("H9"), calculateNumericFormula("C9"), 0);
	}
	
	@Test
	public void NumericArrayFormulaSmallRow(){
		
		// Clean cell's values before calculation
		setNumericValue("A11", 0);
		setNumericValue("B11", 0);
		assertEquals("A11-F11",getNumericValue("F11"), calculateNumericFormula("A11"), 0);
		assertEquals("B11-G11",getNumericValue("G11"), calculateNumericFormula("B11"), 0);
	}
	
	@Test
	public void NumericArrayFormulaBigRow(){
		
		// Clean cell's values before calculation
		setNumericValue("A13", 0);
		setNumericValue("B13", 0);
		setNumericValue("C13", 0);
//		setNumericValue("D13", 0);
		
		assertEquals("A13-F13",getNumericValue("F13"), calculateNumericFormula("A13"), 0);
		assertEquals("B13-G13",getNumericValue("G13"), calculateNumericFormula("B13"), 0);
		assertEquals("C13-H13",getNumericValue("H13"), calculateNumericFormula("C13"), 0);
		assertEquals("D3-I13",getErrorValue("I13"), calculateNumericFormulaWithError("D13"));
	}
	
	@Test
	public void NumericArrayFormulaFewerRows(){
		
		// Clean cell's values before calculation
		setNumericValue("A16", 0);
		setNumericValue("B16", 0);
		setNumericValue("A17", 0);
		setNumericValue("B17", 0);

		assertEquals("A16-F16",getNumericValue("F16"), calculateNumericFormula("A16"), 0);
		assertEquals("B16-G16",getNumericValue("G16"), calculateNumericFormula("B16"), 0);
		assertEquals("A17-F17",getNumericValue("F17"), calculateNumericFormula("A17"), 0);
		assertEquals("B17-G17",getNumericValue("G17"), calculateNumericFormula("B17"), 0);
	}
	@Test
	public void NumericArrayFormulaDataExceed(){
		// Clean cell's values before calculation
		setNumericValue("A19", 0);
		setNumericValue("B19", 0);
		setNumericValue("A20", 0);
		setNumericValue("B20", 0);
		
		assertEquals("A19-F19",getNumericValue("F19"), calculateNumericFormula("A19"), 0);
		assertEquals("B19-G19",getNumericValue("G19"), calculateNumericFormula("B19"), 0);
		assertEquals("A20-F20",getNumericValue("F20"), calculateNumericFormula("A20"), 0);
		assertEquals("B20-G20",getNumericValue("G20"), calculateNumericFormula("B20"), 0);
	}
	@Test
	public void NumericArrayFormulaCol4Row(){
		
		// Clean cell's values before calculation
		setNumericValue("A22", 0);
		setNumericValue("A23", 0);
		setNumericValue("A24", 0);
		setNumericValue("A25", 0);
		
		assertEquals("A22-F22",getNumericValue("F22"), calculateNumericFormula("A22"), 0);
		assertEquals("A23-F23",getNumericValue("F23"), calculateNumericFormula("A23"), 0);
		assertEquals("A24-F24",getNumericValue("F24"), calculateNumericFormula("A24"), 0);
		assertEquals("A25-F25",getNumericValue("F25"), calculateNumericFormula("A25"), 0);
	}
	@Test
	public void NumericArrayFormulaRow4Col(){
		
		// Clean cell's values before calculation
		setNumericValue("A27", 0);
		setNumericValue("B27", 0);
		setNumericValue("C27", 0);
		setNumericValue("D27", 0);
	
		assertEquals("A27-F27",getNumericValue("F27"), calculateNumericFormula("A27"), 0);
		assertEquals("B27-G27",getNumericValue("G27"), calculateNumericFormula("B27"), 0);
		assertEquals("C27-H27",getNumericValue("H27"), calculateNumericFormula("C27"), 0);
		assertEquals("D27-I27",getNumericValue("I27"), calculateNumericFormula("D27"), 0);
	}
	@Test
	public void NumericArrayFormulaDataRow4Col(){
		
		// Clean cell's values before calculation
		setNumericValue("A27", 0);
		setNumericValue("B27", 0);
		setNumericValue("C27", 0);
		setNumericValue("D27", 0);
		
		assertEquals("A27-F27",getNumericValue("F27"), calculateNumericFormula("A27"), 0);
		assertEquals("B27-G27",getNumericValue("G27"), calculateNumericFormula("B27"), 0);
		assertEquals("C27-H27",getNumericValue("H27"), calculateNumericFormula("C27"), 0);
		assertEquals("D27-I27",getNumericValue("I27"), calculateNumericFormula("D27"), 0);
	}
	@Test
	public void NumericArrayFormulaDataShortage(){
		
		// Clean cell's values before calculation
		setNumericValue("B30", 0);
		setNumericValue("C30", 0);
//		setNumericValue("D30", 0);
		setNumericValue("B31", 0);
		setNumericValue("C31", 0);
//		setNumericValue("D31", 0);
//		setNumericValue("B32", 0);
//		setNumericValue("C32", 0);
//		setNumericValue("D32", 0);

		assertEquals("C30-G30",getNumericValue("G30"), calculateNumericFormula("C30"), 0);
		assertEquals("B30-F30",getNumericValue("F30"), calculateNumericFormula("B30"), 0);
//		assertEquals("C30-G30",getNumericValue("G30"), getNumericValue("C30"), 0);
		assertEquals("B31-G30",getNumericValue("F31"), calculateNumericFormula("B31"), 0);
		assertEquals("C31-G31",getNumericValue("G31"), calculateNumericFormula("C31"), 0);
		assertEquals("D30-H30",getErrorValue("H30"), calculateNumericFormulaWithError("D30"));
		assertEquals("D31-H31",getErrorValue("H31"), calculateNumericFormulaWithError("D31"));
		
		assertEquals("B32-F32",getErrorValue("F32"), calculateNumericFormulaWithError("B32"));
		assertEquals("C32-G32",getErrorValue("G32"), calculateNumericFormulaWithError("C32"));
		assertEquals("D32-H32",getErrorValue("H32"), calculateNumericFormulaWithError("D32"));

	}
	
	@Test /*(expected=NotImplementedException.class)*/
	public void  NewNumericArrayFormula(){
		

		setArrayFormula("A35", "SIN({0.1,0.2,0.3})", "A35:B35");
		
		assertEquals("A35-F35",getNumericValue("F35"), calculateNumericFormula("A35"), 0);
		assertEquals("B35-G35",getNumericValue("G35"), calculateNumericFormula("B35"), 0);
	}

	@Test
	public void NumericArrayFormulaRefArguments(){

		// Clean cell's values before calculation
		setNumericValue("A37", 0);
		setNumericValue("B37", 0);
		setNumericValue("C37", 0);
		
		assertEquals("A37-F37",getNumericValue("F37"), calculateNumericFormula("A37"), 0);
		assertEquals("B37-G37",getNumericValue("G37"), calculateNumericFormula("B37"), 0);
		assertEquals("C37-H37",getNumericValue("H37"), calculateNumericFormula("C37"), 0);
	}
	@Test
	public void NumericArrayFormulasRefArguments(){

		// Clean cell's values before calculation
		setNumericValue("A40", 0);
		setNumericValue("B40", 0);
		setNumericValue("C40", 0);
		
		assertEquals("A40-F40",getNumericValue("F40"), calculateNumericFormula("A40"), 0);
		assertEquals("B40-G40",getNumericValue("G40"), calculateNumericFormula("B40"), 0);
		assertEquals("C40-H40",getNumericValue("H40"), calculateNumericFormula("C40"), 0);
	}
	
	@Test
	public void NumericOperation4Range(){

		// Clean cell's values before calculation
		setNumericValue("C43", 0);
		setNumericValue("C44", 0);
		setNumericValue("C45", 0);
		setNumericValue("C46", 0);
		
		assertEquals("C43-F43",getNumericValue("F43"), calculateNumericFormula("C43"), 0);
		assertEquals("C44-F44",getNumericValue("F44"), calculateNumericFormula("C44"), 0);
		assertEquals("C45-F45",getNumericValue("F45"), calculateNumericFormula("C45"), 0);
		assertEquals("C46-F46",getNumericValue("F46"), calculateNumericFormula("C46"), 0);
	}
	
	@Test
	public void NumericOperation4DiffRanges(){

		// Clean cell's values before calculation
		setNumericValue("C48", 0);
		setNumericValue("C49", 0);
		setNumericValue("C50", 0);
		setNumericValue("C51", 0);
		
		assertEquals("C48-F48",getNumericValue("F48"), calculateNumericFormula("C48"), 0);
		assertEquals("C49-F49",getNumericValue("F49"), calculateNumericFormula("C49"), 0);
		assertEquals("C50-F50",getNumericValue("F50"), calculateNumericFormula("C50"), 0);
		assertEquals("C51-F51",getNumericValue("F51"), calculateNumericFormula("C51"), 0);
//		assertEquals("C50-F50",getErrorValue("F50"), getErrorValue("C50"));
//		assertEquals("C51-F51",getErrorValue("F51"), getErrorValue("C51"));
	}
	@Test
	public void NumericArrayChangeRefArguments(){

		// Clean cell's values before calculation
		setNumericValue("A40", 0);
		setNumericValue("B40", 0);
		setNumericValue("C40", 0);
		
		assertEquals("A40-F40",getNumericValue("F40"), calculateNumericFormula("A40"), 0);
		assertEquals("B40-G40",getNumericValue("G40"), calculateNumericFormula("B40"), 0);
		assertEquals("C40-H40",getNumericValue("H40"), calculateNumericFormula("C40"), 0);
		Cell cell = getCell("B40");
//		evaluator.clearAllCachedResultValues();
		setNumericValue("A41", 0.4);
		setNumericValue("B41", 0.5);
		setNumericValue("C41", 0.6);
		cell = getCell("B40");
		assertEquals("B40-G40",Math.cos(Math.sin(0.5)), calculateNumericFormula("B40"), 0);
		assertEquals("A40-F40",Math.cos(Math.sin(0.4)), calculateNumericFormula("A40"), 0);
		assertEquals("C40-H40",Math.cos(Math.sin(0.6)), calculateNumericFormula("C40"), 0);
	}
	@Test
	public void NumericArrayDifTypeArguments(){

		// Clean cell's values before calculation
		setNumericValue("A54", 0);
		setNumericValue("B54", 0);
		setNumericValue("C54", 0);
		setNumericValue("A55", 0);
		setNumericValue("B55", 0);
		setNumericValue("C55", 0);
		
		assertEquals("A54-F54",getNumericValue("F54"), calculateNumericFormula("A54"), 0);
		assertEquals("B54-G54",getNumericValue("G54"), calculateNumericFormula("B54"), 0);
		assertEquals("C54-H54",getNumericValue("H54"), calculateNumericFormula("C54"), 0);
		assertEquals("A55-F55",getNumericValue("F55"), calculateNumericFormula("A55"), 0);
		assertEquals("B55-G55",getNumericValue("G55"), calculateNumericFormula("B55"), 0);
		assertEquals("C55-H55",getNumericValue("H55"), calculateNumericFormula("C55"), 0);
	}
	@Test
	public void NumericArrayBeforeCalc(){
		
		Cell cell1 = wb.getSheetAt(0).getRow(59).getCell(1);  // B60
		assertEquals("B60-precal",10.0,cell1.getNumericCellValue(), 0);
        cell1 = wb.getSheetAt(0).getRow(59).getCell(0); //A60
		assertEquals("A60-precal",4.0,cell1.getNumericCellValue(), 0);

		// Clean cell's values before calculation
		setNumericValue("A60", 0);
		setNumericValue("B60", 0);
		setNumericValue("A61", 0);
		setNumericValue("B61", 0);
		
		assertEquals("A60-F60",getNumericValue("F60"), calculateNumericFormula("A60"), 0);
		assertEquals("B60-G60",getNumericValue("G60"), calculateNumericFormula("B60"), 0);
		assertEquals("A61-F61",getNumericValue("F61"), calculateNumericFormula("A61"), 0);
		assertEquals("B61-G61",getNumericValue("G61"), calculateNumericFormula("B61"), 0);
	}
	@Test
	public void RemoveArrayFormula(){
		
		removeArrayFormula("B40");

		assertEquals("A40-F40",Cell.CELL_TYPE_BLANK, getCellType("A40"), 0);
		assertEquals("B40-G40",Cell.CELL_TYPE_BLANK, getCellType("B40"), 0);
		assertEquals("C40-H40",Cell.CELL_TYPE_BLANK, getCellType("C40"), 0); 
		
	}
	
	protected  int getCellType(String cellRef) {
		 Cell cell = getCell(cellRef);
		return cell.getCellType(); 
	}


	protected Cell getCell(String cellRef){
		
		log.debug("Access to Cell:" + cellRef);
		Sheet sheet = wb.getSheetAt(0);
		CellReference cellReference = new CellReference(cellRef); 
		Row row = sheet.getRow(cellReference.getRow());
		Cell cell = row.getCell(cellReference.getCol()); 
		return cell;
		
	}
	
	protected double calculateNumericFormula(Cell cell){
		
		if (cell.getCellType() != Cell.CELL_TYPE_FORMULA){
			log.error("Not formula in cell: " + cell.toString());
			throw new IllegalArgumentException("Not formula" + cell.toString());
		}

		log.debug("Formula: " + cell.getCellFormula());
		if(evaluator == null)
			evaluator = wb.getCreationHelper().createFormulaEvaluator();
		int type = evaluator.evaluateFormulaCell(cell);
		if (type != Cell.CELL_TYPE_NUMERIC){
			log.error("not numeric result: ");
			throw new IllegalArgumentException("Not numeric type" + type);
		}
		
		double result = cell.getNumericCellValue();
		log.debug("Calculated:" + result);
		return result;
	}
	
	protected String calculateNumericFormulaWithError(Cell cell){
		
		if (cell.getCellType() != Cell.CELL_TYPE_FORMULA){
			log.error("Not formula in cell: " + cell.toString());
			throw new IllegalArgumentException("Not formula" + cell.toString());
		}

		log.debug("Formula: " + cell.getCellFormula());
		if(evaluator == null)
			evaluator = wb.getCreationHelper().createFormulaEvaluator();
		int type = evaluator.evaluateFormulaCell(cell);
		if (type != Cell.CELL_TYPE_ERROR){
			log.error("not error result");
			throw new IllegalArgumentException("Not error type: " + type);
		}
		
		byte result = cell.getErrorCellValue();
		log.debug("Error value:" + result);
		return org.apache.poi.ss.usermodel.ErrorConstants.getText(result);
		
	}
	
	protected String calculateNumericFormulaWithError(String cellRef){
		return calculateNumericFormulaWithError(getCell(cellRef));
	}		
	
	protected double calculateNumericFormula(String cellRef){
		
		return calculateNumericFormula(getCell(cellRef));
	}	
	
	protected double getNumericValue(Cell cell){
		if (cell.getCellType() != Cell.CELL_TYPE_NUMERIC){
			log.error("Not numeric in cell:" + cell.toString());
			throw new IllegalArgumentException("Not numeric:" + cell.toString());
		}
		double result = cell.getNumericCellValue();
		log.debug("value: " + result );
		return result;
	}
	
	protected String getErrorValue(Cell cell){
		if (cell.getCellType() != Cell.CELL_TYPE_ERROR){
			log.error("Not numeric in cell:" + cell.toString());
			throw new IllegalArgumentException("Not error:" + cell.toString());
		}
		byte result = cell.getErrorCellValue();
		log.debug("value: " + result );
		return org.apache.poi.ss.usermodel.ErrorConstants.getText(result);
	}

	protected String getErrorValue(String cellRef){
		
		return getErrorValue(getCell(cellRef));
	}

	protected double getNumericValue(String cellRef){
		
		return getNumericValue(getCell(cellRef));
	}
	
	protected void setNumericValue(String cellRef, double value){
		
		setNumericValue(getCell(cellRef),value);
		// Test value
		try {
			getNumericValue(cellRef);
		} catch (IllegalArgumentException e) { // Root cell has type Formula
			 
		}
	}
	
	
    protected void setNumericValue(Cell cell, double value) {
        cell.setCellValue(value);
        // Notify that values changed 
        if(evaluator != null)
        { 
            evaluator.notifySetFormula(cell);
        }
    }
    protected void setArrayFormula(String cellRef, String formula, String range){
        Sheet sheet = getCell(cellRef).getSheet();
        sheet.setArrayFormula(formula, CellRangeAddress.valueOf(range));
    }
    protected void removeArrayFormula(String cellRef){
    	Cell cell = getCell(cellRef);
        Sheet sheet = cell.getSheet();
        sheet.removeArrayFormula(cell);
    }
}

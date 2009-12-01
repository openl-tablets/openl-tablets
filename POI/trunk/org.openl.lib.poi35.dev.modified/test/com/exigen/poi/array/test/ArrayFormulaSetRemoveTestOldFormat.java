package com.exigen.poi.array.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/*
 * Array Formula Set/Remove  test class
 * 
 * Workbook contains formulas and tests
 */
public class ArrayFormulaSetRemoveTestOldFormat {
	
	private static TestHelper th;
	static Log log = LogFactory.getLog(ArrayFormulaSetRemoveTestOldFormat.class);
	
	@BeforeClass
	public  static void readWorkbook(){

			URL url = ArrayFormulaSetRemoveTestOldFormat.class.getClassLoader().getResource("ArrayFormula.xls");
			Workbook wb;
			try {
				
			wb =  new HSSFWorkbook(new FileInputStream(url.getFile()));
			} catch (IOException ioe){
				log.error("Failed to open test workbook from file:" + url.getFile(), ioe );
				throw new IllegalArgumentException(url.getFile());
			}
			th = new TestHelper(wb);
	}

	public static junit.framework.Test suite() {  
		return new JUnit4TestAdapter(ArrayFormulaSetRemoveTestOldFormat.class);
	}
	@Test
	public void  NewNumericArrayFormula(){
		

		th.setArrayFormula("A35", "SIN({0.1,0.2,0.3})", "A35:B35");
		
		assertEquals("A35-F35",th.getNumericValue("F35"), th.calculateNumericFormula("A35"), 0);
		assertEquals("B35-G35",th.getNumericValue("G35"), th.calculateNumericFormula("B35"), 0);
	}

	@Test
	public void  NewNumericArrayFormulaOut(){

		// create empty workbook
        Workbook workbook =  new XSSFWorkbook();
        try {
        	File excelFile = File.createTempFile("tst", ".xls");
			FileOutputStream out = new FileOutputStream(excelFile);
       	
	         Sheet sheet = workbook.createSheet();
		         
	         Row rowd = sheet.createRow((short) (0));
	       	 Cell cd = rowd.createCell((short) 0);
	       	 CellRangeAddress range = new CellRangeAddress(0,1,0,1);
	       	 sheet.setArrayFormula("SQRT({1,4;9,16})",range);
	     
	         // Calculate formula 
	         FormulaEvaluator eval = workbook.getCreationHelper().createFormulaEvaluator();
	         int type = eval.evaluateFormulaCell(cd);

	 
	       	 // Set tested values
	       	 for(int rowIn = range.getFirstRow(); rowIn <= range.getLastRow();rowIn++)
		       	 for(int colIn = range.getFirstColumn(); colIn <= range.getLastColumn();colIn++)
		       	 {
		       		Cell cell = sheet.getRow(rowIn).getCell(colIn);
		       		double value = cell.getNumericCellValue();
		       		Row row = sheet.getRow(rowIn+5);
		       		if(row == null)
		       			row = sheet.createRow(rowIn+5);
		       		row.createCell(colIn).setCellValue(value);
		       	 }
	       	 
	       	 // Write file
	         workbook.write(out);
	         out.close();
	         
	       	 // Read file
	         workbook =  new XSSFWorkbook(excelFile.getAbsolutePath());
	         sheet = workbook.getSheetAt(0);
	       	 // Set 0 values before calculation
	         for(int rowIn = range.getFirstRow(); rowIn <= range.getLastRow();rowIn++)
		       	 for(int colIn = range.getFirstColumn(); colIn <= range.getLastColumn();colIn++)
		       	 {
		       		Cell cell = sheet.getRow(rowIn).getCell(colIn);
		       		sheet.getRow(rowIn).getCell(colIn).setCellValue(0.0);
		       	 }
             // Calculate formula (we use cell from  firstRow and firstColumn)	         
	         eval = workbook.getCreationHelper().createFormulaEvaluator();
	         eval.evaluateFormulaCell(sheet.getRow(range.getFirstRow()).getCell(range.getFirstColumn()));
	         // Check calculated values
	         for(int rowIn = range.getFirstRow(); rowIn <= range.getLastRow();rowIn++)
		       	 for(int colIn = range.getFirstColumn(); colIn <= range.getLastColumn();colIn++)
		       	 {
		       		Cell cell = sheet.getRow(rowIn).getCell(colIn);
		       		double value = cell.getNumericCellValue();
		       		cell = sheet.getRow(rowIn+5).getCell(colIn);
					assertEquals("ArrayFormula:"+rowIn+","+colIn,cell.getNumericCellValue(),value, 0);
		       		
		       	 }
				
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	@Test
	public void RemoveArrayFormula(){
		
		th.removeArrayFormula("A40");

		assertEquals("A40-F40",Cell.CELL_TYPE_BLANK, th.getCellType("A40"), 0);
		assertEquals("B40-G40",Cell.CELL_TYPE_BLANK, th.getCellType("B40"), 0);
		assertEquals("C40-H40",Cell.CELL_TYPE_BLANK, th.getCellType("C40"), 0); 
		
	}
	

}

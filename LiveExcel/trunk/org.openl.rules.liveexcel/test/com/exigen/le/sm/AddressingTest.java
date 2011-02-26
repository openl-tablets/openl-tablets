
package com.exigen.le.sm;
import static junit.framework.Assert.assertFalse;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.After;
import org.junit.Test;

import junit.framework.TestCase;

import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.Cell;
import com.exigen.le.smodel.Range;
//import com.exigen.le.utils.POIHelper;



public class AddressingTest extends TestCase{

	@Test
	public void testColumnIndex(){
		String cellRef = "mySheet!AB124";
		Cell cell = new Cell().init(cellRef);
		String sheetName =cell.getSheetName();
		System.out.println("SheetName = "+sheetName);
		assertEquals("mySheet",sheetName);
		
		System.out.println("Colunm = "+cell.getColumn());
		assertEquals("AB",cell.getColumn());
		int colIndex = cell.getColumnIndex();
		System.out.println("Colunm Index = "+colIndex);
		assertEquals(27,colIndex);
		int rowIndex = cell.getRowIndex();
		System.out.println("Row Index = "+rowIndex);
		assertEquals(123,rowIndex);
		
		cell = new Cell();
		cell.setSheetName(sheetName);
		cell.setColumnIndex(colIndex);
		cell.setRow(rowIndex+1);
		System.out.println("Cell Ref = "+cell.toString());
		assertEquals(cellRef,cell.toString());
		
		
		
		Range range = new Range().init("B4:C7");
		System.out.println("SheetName = "+range.from().getSheetName());
		assertEquals("",range.from().getSheetName());
		System.out.println("From Colunm = "+range.from().getColumn());
		System.out.println("From Colunm Index = "+range.from().getColumnIndex());
		assertEquals(1,range.from().getColumnIndex());
		System.out.println("From Row = "+range.from().getRow());
		assertEquals(4,range.from().getRow());
		
		System.out.println("SheetName = "+range.to().getSheetName());
		assertEquals("",range.to().getSheetName());
		System.out.println("To Colunm = "+range.to().getColumn());
		System.out.println("To Colunm Index = "+range.to().getColumnIndex());
		assertEquals(2,range.to().getColumnIndex());
		System.out.println("To  Row= "+range.to().getRow());
		assertEquals(7,range.to().getRow());
		
		range = new Range().init("[excel1.xls]B4:[excel2]Sheet!C7");
		System.out.println("WorkbooktName = "+range.from().getWorkbookName());
		assertEquals("excel1.xls",range.from().getWorkbookName());

		System.out.println("WorkbooktName = "+range.to().getWorkbookName());
		assertEquals("excel2",range.to().getWorkbookName());
		
		System.out.println("SheetName = "+range.from().getSheetName());
		assertEquals("",range.from().getSheetName());
		System.out.println("From Colunm = "+range.from().getColumn());
		System.out.println("From Colunm Index = "+range.from().getColumnIndex());
		assertEquals(1,range.from().getColumnIndex());
		System.out.println("From Row = "+range.from().getRow());
		assertEquals(4,range.from().getRow());
		
		System.out.println("SheetName = "+range.to().getSheetName());
		assertEquals("Sheet",range.to().getSheetName());
		System.out.println("To Colunm = "+range.to().getColumn());
		System.out.println("To Colunm Index = "+range.to().getColumnIndex());
		assertEquals(2,range.to().getColumnIndex());
		System.out.println("To  Row= "+range.to().getRow());
		assertEquals(7,range.to().getRow());
//		Workbook wb =POIHelper.getWorbook("ACF.xlsm");
//		Sheet sheet = wb.getSheetAt(0);
//		Row row = sheet.getRow(0); 
//		org.apache.poi.ss.usermodel.Cell c = row.getCell(cell.getColumnIndex());
//		String content= c.getStringCellValue();
//		System.out.println("Content ="+content);
//		assertEquals("ACF",content);
		range = new Range().init("B4:C7");
		System.out.println("SheetName = "+range.from().getSheetName());
		
	}
    
    // We should clear all created temp files manually because JUnit terminates
    // JVM incorrectly and finalization methods are not executed
    @After
    public void finalize() {
        try {
            ProjectLoader.reset();
            FileUtils.deleteDirectory(ProjectLoader.getTempDir());
        } catch (IOException e) {
            e.printStackTrace();
            assertFalse(true);
        }
    }
}

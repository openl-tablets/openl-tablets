package org.openl.rules.test.spreadsheet.errors.last;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

public class LastErrorsTest {
    
    /**
     * When LifeExcel plugin recalculates the spreadsheet, it changes the formula definition of Service Model to some special id
     */
    @Test
    public void testServiceModelDefinition() throws Exception{        
        Workbook workbook = getXSSFWorkbook("./test/resources/The rules 8.xlsm");
        Sheet sheet = workbook.getSheet("Rating Algorithm");
        Cell cell = sheet.getRow(10).getCell(4);        
        assertFalse("Expect type of cell won`t be formula.It was spoiled by LE plugin.",Cell.CELL_TYPE_FORMULA==cell.getCellType());        
    }
    
    /**
     * fails on evaluating OL_PRINT_FULLNAME function
     */
    @Test(expected = RuntimeException.class)
    public void testOlPrintFullname() throws Exception {
        Workbook workbook = getXSSFWorkbook("./test/resources/The rules 8.xlsm");
        Sheet sheet = workbook.getSheet("Service Model");
        Cell evaluateInCell = new XSSFFormulaEvaluator((XSSFWorkbook) workbook).evaluateInCell(sheet.getRow(9).getCell(
                5));
        assertFalse("Test won`t pass if evaluation success", true);
    }
    
    /**
     * POI fails on parsing multi line condition. e.g.
     * 'IF(E28>=21000, "[21000+]",
     *  IF(E28>=18000, "[18000-20999]",
     *  IF(E28>=15000, "[15000-17999]",
     *  IF(E28>=12000, "[12000-14999]",
     *  IF(E28>=11000, "[11000-11999]",
     *  IF(E28>=10000, "[10000-10999]",
     *  IF(E28>=9000, "[9000-9999]",
     *  IF(E28>=8000, "[8000-8999]",E31))))))))'
     * 
     */
    @Test(expected = RuntimeException.class)
    public void testMultyLineFormula() {
        Workbook workbook = getXSSFWorkbook("./test/resources/The rules 8.xlsm");
        Sheet sheet = workbook.getSheet("Annual Mileage.2");
        Cell evaluateInCell;
        evaluateInCell = new XSSFFormulaEvaluator((XSSFWorkbook) workbook).evaluateInCell(sheet.getRow(31).getCell(4));
        assertFalse("Test won`t pass if evaluation success", true);
    }
    
    public static Workbook getXSSFWorkbook(String fileName) {
        LiveExcelWorkbook workbook = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            
            workbook = LiveExcelWorkbookFactory.create(is, "SimpleExample");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return workbook;
    }
}

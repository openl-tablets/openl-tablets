package org.openl.rules.test.liveexcel.formula;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.TypeResolver;

public class TypeResolverTest {
    
    private HSSFWorkbook getWorkbook() {
        String fileName = "./test/resources/TestTypes.xls";
        HSSFWorkbook workbook = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            workbook = new HSSFWorkbook(fs);
        } catch (IOException e) {
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
    
    @Test
    public void testDateType() {
        Calendar cal = new GregorianCalendar();
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFCell cell = sheet.getRow(0).getCell(1);   
        if(cell.getCellType ()==HSSFCell.CELL_TYPE_NUMERIC) {
            assertTrue(HSSFDateUtil.isCellDateFormatted(cell));
            double d = cell.getNumericCellValue();
            String cellText =
                (String.valueOf(cal.get(Calendar.YEAR))).substring(2);
            cellText = cal.get(Calendar.MONTH)+1 + "/" +
                         cal.get(Calendar.DAY_OF_MONTH) + "/" +
                         cellText;            
       }
    }
    
    @Test
    public void testNumberType() {
        DataFormatter formatter = new DataFormatter();
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFCell cell = sheet.getRow(1).getCell(1);
        if(cell.getCellType ()==HSSFCell.CELL_TYPE_NUMERIC) {
            CellStyle cellStyle = cell.getCellStyle();
            assertEquals("0.00", cellStyle.getDataFormatString());            
        }
    }
    
    @Test
    public void testCurrencyType() {
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFCell cell = sheet.getRow(2).getCell(1);
        if(cell.getCellType ()==HSSFCell.CELL_TYPE_NUMERIC) {
            CellStyle cellStyle = cell.getCellStyle();
            assertEquals('"'+"$"+'"'+"#,##0.00", cellStyle.getDataFormatString());
        }
    }  
    
    @Test 
    public void testBooleanSum() {
        HSSFWorkbook workbook = getWorkbook();
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(workbook);
        searcher.findFunctions();
        Sheet sheet = workbook.getSheetAt(0);
        HSSFCell evaluateInCell = new HSSFFormulaEvaluator(workbook).evaluateInCell(sheet.getRow(16).getCell(1));
                
        assertTrue(2 == evaluateInCell.getNumericCellValue());        
    }
    
    /**
     * 
     */
    @Test
    public void testType() {
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(1);
        HSSFCell cellNumber = sheet.getRow(0).getCell(0);
        Class res = TypeResolver.resolveType(cellNumber);        
        if((res.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cellBool = sheet.getRow(1).getCell(0);
        Class res1 = TypeResolver.resolveType(cellBool);
        if((res1.getName().toString()).equals(Boolean.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cellDate = sheet.getRow(2).getCell(0);
        Class res2 = TypeResolver.resolveType(cellDate);
        if((res2.getName().toString()).equals(Date.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cellString = sheet.getRow(3).getCell(0);
        Class res3 = TypeResolver.resolveType(cellString);
        if((res3.getName().toString()).equals(String.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cellFunc = sheet.getRow(4).getCell(0);
        Class res4 = TypeResolver.resolveType(cellFunc);        
        if((res4.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cell1 = sheet.getRow(6).getCell(0);
        Class res6 = TypeResolver.resolveType(cell1);        
        if((res6.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
    }
    
    @Test
    public void testFormat() {
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(1);
        HSSFCell cellStrFormNum = sheet.getRow(6).getCell(0);
        Class<?> res = TypeResolver.resolveType(cellStrFormNum);
        if((res.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cellNumFormText = sheet.getRow(13).getCell(0);
        Class<?> res1 = TypeResolver.resolveType(cellNumFormText); 
        if((res1.getName().toString()).equals(String.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        HSSFCell cellCurr = sheet.getRow(14).getCell(0);
        Class<?> res2 = TypeResolver.resolveType(cellCurr);
        if((res2.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
    } 
    
    /*@Test
    public void testErrorType() {
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(1);
        HSSFCell cellStrFormNum = sheet.getRow(8).getCell(0);
        Class<?> res = TypeResolver.resolveType(cellStrFormNum);
        assertTrue(res==null);
    }*/
 }
        
        
        
       
    
       


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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.ServiceModelAPI;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.TypeResolver;
import org.openl.rules.liveexcel.hssf.usermodel.LiveExcelHSSFWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

import com.exigen.ipb.schemas.rating.hb.CoverageTypeImpl;
import com.exigen.ipb.schemas.rating.hb.VehicleDriverRelationshipTypeImpl;

public class TypeResolverTest {
    
    private Workbook getWorkbook(String fileName) {
        
        Workbook workbook = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            workbook = LiveExcelWorkbookFactory.create(is, "SimpleExample");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {            
            e.printStackTrace();
        }finally {
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
        Workbook workbook = getWorkbook("./test/resources/TestTypes.xls");
        Sheet sheet = workbook.getSheetAt(0);
        Cell cell = sheet.getRow(0).getCell(1);   
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
        Workbook workbook = getWorkbook("./test/resources/TestTypes.xls");
        Sheet sheet = workbook.getSheetAt(0);
        Cell cell = sheet.getRow(1).getCell(1);
        if(cell.getCellType ()==HSSFCell.CELL_TYPE_NUMERIC) {
            CellStyle cellStyle = cell.getCellStyle();
            assertEquals("0.00", cellStyle.getDataFormatString());            
        }
    }
    
    @Test
    public void testCurrencyType() {
        Workbook workbook = getWorkbook("./test/resources/TestTypes.xls");
        Sheet sheet = workbook.getSheetAt(0);
        Cell cell = sheet.getRow(2).getCell(1);
        if(cell.getCellType ()==HSSFCell.CELL_TYPE_NUMERIC) {
            CellStyle cellStyle = cell.getCellStyle();
            assertEquals('"'+"$"+'"'+"#,##0.00", cellStyle.getDataFormatString());
        }
    }  
    
    @Test 
    public void testBooleanSum() {
        Workbook workbook = getWorkbook("./test/resources/TestTypes.xls");
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(workbook);
        searcher.findFunctions();
        Sheet sheet = workbook.getSheetAt(0);
        HSSFCell evaluateInCell = new HSSFFormulaEvaluator((LiveExcelHSSFWorkbook)workbook).evaluateInCell(sheet.getRow(16).getCell(1));
                
        assertTrue(2 == evaluateInCell.getNumericCellValue());        
    }
    
    /**
     * 
     */
    @Test
    public void testType() {        
        Workbook workbook = getWorkbook("./test/resources/TestTypes.xls");
        ServiceModelAPI serviceModelAPI = new ServiceModelAPI("SimpleExample");
        Sheet sheet = workbook.getSheetAt(1);        
        Cell cellNumber = sheet.getRow(0).getCell(0);
        Class res = TypeResolver.resolveType(cellNumber, serviceModelAPI);        
        if((res.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellBool = sheet.getRow(1).getCell(0);
        Class res1 = TypeResolver.resolveType(cellBool, serviceModelAPI);
        if((res1.getName().toString()).equals(Boolean.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellDate = sheet.getRow(2).getCell(0);
        Class res2 = TypeResolver.resolveType(cellDate, serviceModelAPI);
        if((res2.getName().toString()).equals(Calendar.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellString = sheet.getRow(3).getCell(0);
        Class res3 = TypeResolver.resolveType(cellString, serviceModelAPI);
        if((res3.getName().toString()).equals(String.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellFunc = sheet.getRow(4).getCell(0);
        Class res4 = TypeResolver.resolveType(cellFunc, serviceModelAPI);         
        if((res4.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cell1 = sheet.getRow(6).getCell(0);
        Class res6 = TypeResolver.resolveType(cell1, serviceModelAPI);        
        if((res6.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
    }
    
    @Test
    public void testFormat() {
        Workbook workbook = getWorkbook("./test/resources/TestTypes.xls");
        ServiceModelAPI serviceModelAPI = new ServiceModelAPI("SimpleExample");
        Sheet sheet = workbook.getSheetAt(1);
        Cell cellStrFormNum = sheet.getRow(6).getCell(0);
        Class<?> res = TypeResolver.resolveType(cellStrFormNum, serviceModelAPI);
        if((res.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellNumFormText = sheet.getRow(13).getCell(0);
        Class<?> res1 = TypeResolver.resolveType(cellNumFormText, serviceModelAPI); 
        if((res1.getName().toString()).equals(String.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellCurr = sheet.getRow(14).getCell(0);
        Class<?> res2 = TypeResolver.resolveType(cellCurr, serviceModelAPI);
        if((res2.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
    } 
    
    //fails because there is no working live Excel
    /*@Test
    public void testServiceModelType() {
        Workbook workbook = getWorkbook("./test/resources/DataAccessTest.xls");
        ServiceModelAPI serviceModelAPI = new ServiceModelAPI("SimpleExample");
        Sheet sheet = workbook.getSheetAt(0);
        Cell cellSMFunc = sheet.getRow(3).getCell(1);
        Class<?> res = TypeResolver.resolveType(cellSMFunc, serviceModelAPI);        
        if((res.getName().toString()).equals(Double.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cellServMod = sheet.getRow(3).getCell(2);
        Class<?> res1 = TypeResolver.resolveType(cellServMod, serviceModelAPI);       
        if((res1.getName().toString()).equals(String.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
        
        Cell cell = sheet.getRow(2).getCell(1);
        Class<?> res2 = TypeResolver.resolveType(cell, serviceModelAPI);
        System.out.println("111111111111111"+res2.getName().toString());
        if((res2.getName().toString()).equals(String.class.getName().toString())) {
            assertTrue(true);            
        } else {            
            assertTrue(false);
        }
    }*/
    
//    @Test
//    public void testServiceModelRootType() {
//        Workbook workbook = getWorkbook("./test/resources/LiveExcel Demo Case Revised.xlsx");
//        ServiceModelAPI serviceModelAPI = new ServiceModelAPI("DemoCase");
//        Sheet sheet = workbook.getSheetAt(0);
//        Cell cellSM = sheet.getRow(10).getCell(3);
//        Class<?> res = TypeResolver.resolveType(cellSM, serviceModelAPI);        
//        if((res.getName().toString()).equals(CoverageTypeImpl.class.getName().toString())) {
//            assertTrue(true);            
//        } else {            
//            assertTrue(false);
//        }
//    }
    
 }
        
        
        
       
    
       


package org.openl.rules.test.liveexcel.formula;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
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
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;

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
            System.out.println ("Date value: " + cellText);
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
            
            System.out.println("Format of cell string="+cellStyle.getDataFormatString());
            System.out.println("Format of cell short="+cellStyle.getDataFormat() );
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
            System.out.println("Format of cell string="+cellStyle.getDataFormatString());
        }
    }
    
    @Test
    public void testAllTypes() {
        Calendar cal = new GregorianCalendar();
        HSSFWorkbook workbook = getWorkbook();
        HSSFSheet sheet = workbook.getSheetAt(0);
        for (Row row : sheet) {
            System.out.println ("===================================");
            System.out.println ("Row No.: " + row.getRowNum ());
            for (Cell cell : row) {
                System.out.println ("----------------------------------");
                System.out.println ("Cell No.: " + cell.getColumnIndex());
                switch (cell.getCellType ()) {
                    case HSSFCell.CELL_TYPE_NUMERIC: {
                        double d = cell.getNumericCellValue();                        
                        if (HSSFDateUtil.isCellDateFormatted(cell)) {
                            cal.setTime(HSSFDateUtil.getJavaDate(d));
                            String cellText =
                                (String.valueOf(cal.get(Calendar.YEAR))).substring(2);
                            cellText = cal.get(Calendar.MONTH)+1 + "/" +
                                     cal.get(Calendar.DAY_OF_MONTH) + "/" +
                                     cellText;
                            CellStyle cellStyle = cell.getCellStyle();
                            System.out.println ("Format of cell: " + cellStyle.getDataFormatString());
                            System.out.println ("Date value: " + cellText);
                        } else {
                            CellStyle cellStyle = cell.getCellStyle();
                            System.out.println ("Format of cell: " + cellStyle.getDataFormatString());
                            System.out.println ("Numeric value: " + cell.getNumericCellValue ());
                        }  
                        break;
                    }
                    case HSSFCell.CELL_TYPE_STRING : {                        
                        if (cell instanceof HSSFCell) {
                            HSSFCell new_hssfCell = (HSSFCell) cell;
                            HSSFRichTextString richTextString = new_hssfCell.getRichStringCellValue ();                        
                            System.out.println ("String value: " + richTextString.getString ());                        
                            break;
                        }
                    }
                    case HSSFCell.CELL_TYPE_BOOLEAN : {                        
                        if (cell instanceof HSSFCell) {
                            HSSFCell new_hssfCell = (HSSFCell) cell;
                                                    
                            System.out.println ("Boolean value: " + new_hssfCell.getBooleanCellValue());                        
                            break;
                        }
                    }
                    default:{
                        System.out.println ("Type not supported.");                        
                        break;
                    }
                }               
            }
        }
        
    }
    
    @Test 
    public void testBooleanSum() {
        HSSFWorkbook workbook = getWorkbook();
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(workbook);
        searcher.findFunctions();
        Sheet sheet = workbook.getSheetAt(0);
        HSSFCell evaluateInCell = new HSSFFormulaEvaluator(workbook).evaluateInCell(sheet.getRow(16).getCell(1));
        
        System.out.println("Boolean result = "+evaluateInCell.getNumericCellValue());
        assertTrue(2 == evaluateInCell.getNumericCellValue());        
    }
        
 }
        
        
        
       
    
       


/* ====================================================================    
   Licensed to the Apache Software Foundation (ASF) under one or more      
   contributor license agreements.  See the NOTICE file distributed with   
   this work for additional information regarding copyright ownership.     
   The ASF licenses this file to You under the Apache License, Version 2.0 
   (the "License"); you may not use this file except in compliance with    
   the License.  You may obtain a copy of the License at                   
                                                                           
       http://www.apache.org/licenses/LICENSE-2.0                          
                                                                           
   Unless required by applicable law or agreed to in writing, software     
   distributed under the License is distributed on an "AS IS" BASIS,       
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and     
   limitations under the License.                                          
==================================================================== */

package org.apache.poi.ss.formula.eval;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

import org.apache.poi.POIDataSamples;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Base class for formula evaluation test cases. Workbook reading and accessing
 * functionality is implemented here. 
 * @author dsapunovs
 */
public abstract class BaseFormulaTest extends TestCase {

    private static final int TYPE_2003 = 1;
    private static final int TYPE_2007 = 2;
    private static final int TYPE_UNKNOWN = 0;

    private Workbook workbook;

    /**
     * Creates test case instance.
     */
    protected BaseFormulaTest() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    protected BaseFormulaTest(String name) {
        super(name);
    }

    @Override
    /**
     * Reads workbook initializing test case.
     */
    protected void setUp() throws Exception {
        super.setUp();
        workbook = readWorkbook();
    }
    
    
    protected Workbook getWorkbook() {
        return workbook;
    }
    

    /**
     * Fills all formula cells in worksheet with specified value. Used to
     * ensure value is really calculated when calculation method is called.
     * @param value The value.
     */
    protected void fillFormulaValues(int sheetIndex, String value) {
        // fill all formula cells with fake value.
        // this will ensure formula is calculated when requested.
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            
            if (row != null) {
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null && cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                        cell.setCellValue( value );
                    }
                }
            }
        }
    }

    /**
     * Reads workbook from resource.
     * @return New instance of workbook.
     * @throws IOException
     * @throws IllegalStateException if resource with Excel test file is not accessible.  
     * @see #getResourceName()
     */
    protected Workbook readWorkbook() throws IOException, IllegalStateException {
        String resourceName = getResourceName();
        
        InputStream stream;
        // get stream by resource name.
        // this operation is performed in two steps:
        // 1. using standard POI approach - POIDataSamples class. This requires
        // system variable POI.testdata.path pointing to test-data directory.
        // 2. workaround if system variable is not set - try to get resource
        // from classpath.
        
        try {
            POIDataSamples res = POIDataSamples.getSpreadSheetInstance();
            stream = res.openResourceAsStream( resourceName );

        } catch (RuntimeException rte) {
            // looks like system variable is not set. try to get from classpath
            ClassLoader cl = getClass().getClassLoader();
            stream = cl.getResourceAsStream(resourceName);
            if (stream == null) {
                // not found in classpath - throw previous runtime exception.
                throw rte;
            }
        }
         
        if (stream == null) {
            fail( MessageFormat.format("Resource {0} is not found.", resourceName) );
            // the "fail" method should throw exception
            // and code should not proceed in this case.
        }

        // check type of file - is it 2003 or 2007
        int fileType;
        
        String lowerCaseResourceName = resourceName.toLowerCase();
        if (lowerCaseResourceName.endsWith(".xls")) {
            fileType = TYPE_2003;

        } else if (lowerCaseResourceName.endsWith(".xlsx")) {
            fileType = TYPE_2007;

        } else if (lowerCaseResourceName.endsWith(".xlsm")) {
            fileType = TYPE_2007;
            
        } else {
            fileType = TYPE_UNKNOWN;
        }

        switch (fileType) {
            case TYPE_2003:
                return new HSSFWorkbook( stream );
            
            case TYPE_2007:
                return new XSSFWorkbook( stream );

            default:
                throw new IllegalStateException(
                    MessageFormat.format("Resource of unsupported type: {0}", resourceName)
                );
        }
    }

    /**
     * Gets number of rows available for specified sheet in workbook.
     * @param sheetIndex Index of the sheet.
     * @return Number of rows in sheet or 0.
     */
    protected int getNumberOfRows(int sheetIndex) {
        Sheet sheet = workbook.getSheetAt( sheetIndex );
        if (sheet == null) {
            return 0;
        }
        
        return sheet.getLastRowNum() + 1;
    }
    
    /**
     * Gets cell withing specified range.
     * @param range The cell range.
     * @param offsetX Horizontal offset, 0 for first cell in range.
     * @param offsetY Vertical offset, 0 for first cell in range.
     * @param sheetIndex Index of sheet with the range. 
     * @return Cell or <code>null</code> if cell is not found.
     */
    protected Cell getCell(CellRangeAddress range, int offsetX, int offsetY, int sheetIndex) {
        Sheet sheet = workbook.getSheetAt( sheetIndex );
        if (sheet == null) {
            return null;
        }

        Row row = sheet.getRow(range.getFirstRow() + offsetY);
        if (row==null) {
            return null;
        }

        Cell cell = row.getCell(range.getFirstColumn() + offsetX); 
        if (cell==null) {
            return null;
        }
        return cell;
    }

    /**
     * Gets specified cell from specified sheet.
     * @param cellRef Cell address like A5, D2 etc.
     * @param sheetIndex Sheet index.
     * @return Cell or <code>null</code> if referenced outside workbook.
     */
    protected Cell getCell(String cellRef, int sheetIndex) {
        return getCell(cellRef, 0, 0, sheetIndex);
    }
    
    /**
     * Gets a cell from specified sheet relative to another cell.
     * @param cellRef Base cell address like A5, D2 etc.
     * @param offsetX Horizontal offset to the cell.
     * @param offsetY Vertical offset to the cell.
     * @param sheetIndex Sheet index.
     * @return Cell or <code>null</code> if referenced outside workbook.
     */
    protected Cell getCell(String cellRef, int offsetX, int offsetY, int sheetIndex) {
        Sheet sheet = workbook.getSheetAt( sheetIndex );
        if (sheet == null) {
            return null;
        }
        
        CellReference cellReference = new CellReference(cellRef); 
        Row row = sheet.getRow(cellReference.getRow() + offsetY);
        if (row==null) {
            return null;
        }

        Cell cell = row.getCell(cellReference.getCol() + offsetX); 
        if (cell==null) {
            return null;
        }
        return cell;
    }
    

    /**
     * Asserts formula calculation result with cell's expected value.
     * @param message Message used when assertion is failed.
     * @param expectedNumeric Cell with expected numeric value.
     * @param actualFormula Cell with numeric formula to check.
     * @param expectedRef String address (reference) to cell with expected
     * value. Used in assertion failed message.
     * @param delta Allowed delta for value comparision.
     */
    protected void assertFormulaCell(String message, Cell expectedNumeric, Cell actualFormula, String expectedRef, double delta) {
        assertNotNull("Test failed. Expected value must not be empty in cell " + expectedRef, expectedNumeric);
        double expected = expectedNumeric.getNumericCellValue();
        assertFormulaCell(message, expected, actualFormula, delta);
    }
    
    /**
     * Asserts formula calculation result with expected value.
     * @param message Message used when assertion is failed.
     * @param expected The expected numeric value.
     * @param actualFormula Cell with numeric formula to check.
     * @param delta Allowed delta for value comparision.
     */
    protected void assertFormulaCell(String message, double expected, Cell actualFormula, double delta) {
        double actual = calculateNumericFormula(actualFormula);
        assertEquals(message, expected, actual, delta);
    }
    
    
    protected double calculateNumericFormula(Cell cell) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        int type = evaluator.evaluateFormulaCell(cell);
        if (type != Cell.CELL_TYPE_NUMERIC) {
            fail("Not numeric type" + type);
        }
        
        double result = cell.getNumericCellValue();
        return result;
    }


    protected Boolean calculateBooleanFormula(Cell cell) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        int type = evaluator.evaluateFormulaCell(cell);
        if (type != Cell.CELL_TYPE_BOOLEAN) {
            fail("Not boolean type" + type);
        }
        
        Boolean result = cell.getBooleanCellValue();
        return result;
    }
    

    protected String calculateStringFormula(Cell cell) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        int type = evaluator.evaluateFormulaCell(cell);
        if (type != Cell.CELL_TYPE_STRING) {
            fail("Not string type " + cell);
        }
        
        String result = cell.getStringCellValue();
        return result;
    }

    
    protected FormulaError calculateFormulaError(Cell cell) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        int type = evaluator.evaluateFormulaCell(cell);
        if (type != Cell.CELL_TYPE_ERROR) {
            fail("Not error type: " + type);
        }
        
        byte result = cell.getErrorCellValue();
        return FormulaError.forInt(result);
    }
    
    /**
     * Short stack-trace printing method used when one test case should report
     * number of errors - short trace should be printed for each error. 
     * @param e Stack-trace containing exception.
     */
    protected void printShortStackTrace(AssertionFailedError e) {
        printShortStackTrace(System.err, e);
    }

    /**
     * Short stack-trace printing method used when one test case should report
     * number of errors - short trace should be printed for each error. 
     * @param ps Stream where to print.
     * @param e Stack-trace containing exception.
     */
    protected void printShortStackTrace(PrintStream ps, AssertionFailedError e) {
        StackTraceElement[] stes = e.getStackTrace();

        int startIx = 0;
        // skip any top frames inside junit.framework.Assert
        while(startIx<stes.length) {
            if(!stes[startIx].getClassName().equals(Assert.class.getName())) {
                break;
            }
            startIx++;
        }
        // skip bottom frames (part of junit framework)
        int endIx = startIx+1;
        while(endIx < stes.length) {
            if(stes[endIx].getClassName().equals(TestCase.class.getName())) {
                break;
            }
            endIx++;
        }
        if(startIx >= endIx) {
            // something went wrong. just print the whole stack trace
            e.printStackTrace(ps);
        }
        endIx -= 4; // skip 4 frames of reflection invocation
        ps.println(e.toString());
        for(int i=startIx; i<endIx; i++) {
            ps.println("\tat " + stes[i].toString());
        }
    }
    
    /**
     * Compares two arrays comparing their elements.
     * @param message Error message used in case of failure.
     * @param expected Array as it is expected.
     * @param actual Actual array to compare.
     * @param delta Delta value used in double value comparision. Use 0 for
     * higher precision.
     */
    protected void assertEquals(String message, Object[][] expected, Object[][] actual, double delta) {
        
        if (expected == null) {
            assertNull(message, actual);

        } else {
            assertEquals(message + " Invalid size of array", expected.length, actual.length);
            for (int x = 0; x < actual.length; x++) {
                
                if (expected[x] == null) {
                    assertNull(message, actual[x]);
                } else {
                    assertEquals(message + " Invalid element size for row " + x, expected[x].length, actual[x].length);
                    for (int y = 0; y < actual[x].length; y++) {
                        if (expected[x][y] instanceof Double) {
                            assertEquals(message, (Double) expected[x][y], (Double) actual[x][y], delta);
                        } else {
                            assertEquals(message, expected[x][y], actual[x][y]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets name of workboou containing resource.
     * @return Name of workbook resource.
     */
    abstract protected String getResourceName();
    
    protected String failedMessage(String cellRef){
    	return "Failed formula in " + cellRef + resource(); 
    }
    
    protected String resource(){
    	return "(resource: " + getResourceName() + " )";
    }
}

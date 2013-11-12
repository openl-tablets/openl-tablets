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

import java.text.MessageFormat;
import java.util.LinkedList;

import junit.framework.AssertionFailedError;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Base class for formula evaluation test cases.
 */
abstract public class BaseArrayFormulaTest extends BaseFormulaTest {

    // to test a particular formula, not all formulas in Excel file, this
    // array should contain 1-based number of row where formula is defined.
    // all formulas will be tested if this array is empty.
    protected int[] testRows = {  };
    // IMPORTANT! only first row from range may be used in array above!

	
	/**
     * Creates test case instance.
     */
    protected BaseArrayFormulaTest() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    protected BaseArrayFormulaTest(String name) {
        super(name);
    }
    
    @Override
    /**
     * Fills all formula cells in workbook with fake values to ensure
     * formulas will be calculated during tests.
     */
    protected void setUp() throws Exception {
        super.setUp();
        fillFormulaValues(getTestSheetIndex(), "Fake value");
    }

    /**
     * Tests formula evaluation based on test
     * data in <i>ArrayFormulaFunctions</i> file.
     */
    public void testArrayFormulaFunctions() {
        
        if (testRows.length != 0){
        	System.out.println("only rows defined in testRows would be tested!");
        }
    	
    	final int sheetIndex = getTestSheetIndex();
        
        // index of starting row in Excel test file
        int startRow = getTestStartingRow();
        int maxRow = getNumberOfRows(sheetIndex);
        
        LinkedList<String> failedFormulas = new LinkedList<String>();
        int passedFormulasCount = 0;
        
        for (int rowIndex = startRow; rowIndex <= maxRow; rowIndex++) {
            
            // all rows must be tested if testRows array is empty.
            if (testRows.length > 0) {
                // only rows from this array may be tested,
                // other rows must be skipped if array is not empty.
                boolean skipRow = true;
                for (int rowNumber : testRows) {
                    if (rowNumber == rowIndex) {
                        skipRow = false;
                        break;
                    }
                }
                if (skipRow) {
                    // skip row if not in array.
                    continue;
                }
            }
            
            
            String formulaRef = getFormulaCellRef(rowIndex);
            
            Cell cellFormula = getCell(formulaRef, sheetIndex);
            
            // check, should we stop here:
            if (cellFormula != null && cellFormula.getCellType() == Cell.CELL_TYPE_STRING) {
                if (getFunctionEndString().equals( cellFormula.getStringCellValue() )) {
                    // should not proceed with formula testing
                    break;
                }
            }
            
            if (cellFormula != null && cellFormula.getCellType() == Cell.CELL_TYPE_FORMULA) {

                try {
                    // proceed with the formula:
                    String message = failedMessage(formulaRef);
                    
                    checkArrayFormula( message, cellFormula, rowIndex, sheetIndex );
                    passedFormulasCount++;
                } catch (AssertionFailedError e) {
                    printShortStackTrace(e);
                    failedFormulas.add( formulaRef );
                }
                
                if (cellFormula.isPartOfArrayFormulaGroup()) {
                    // skip rows from same range. this should avoid same
                    // formula calculation for another row(s) from range:
                    CellRangeAddress range = cellFormula.getArrayFormulaRange();
                    rowIndex += (range.getLastRow() - range.getFirstRow());
                }
            }
        }
        
        if (failedFormulas.size() > 0) {
            // test failed.
            fail( MessageFormat.format("Passed {0}, failed {1} formula(s): {2}", passedFormulasCount, failedFormulas.size(), failedFormulas) );
        }
    }

    private void checkArrayFormula(String message, Cell cellFormula, int rowIndex, int sheetIndex) {
        if (cellFormula.isPartOfArrayFormulaGroup()) {
            // check array formula here:
            CellRangeAddress range = cellFormula.getArrayFormulaRange();
            
            Object[][] expected = getExpectedArrayResult(rowIndex, range, sheetIndex);
            Object[][] actual = calculateArrayFormula(range, sheetIndex);

            assertEquals(message, expected, actual, 0.000001);
            
        } else {
            // check simple formula here:
            String expectedRef = getExpectedValueCellRef(rowIndex); 
            
            Cell cellExpected = getCell(expectedRef, sheetIndex);
            assertFormulaCell(message, cellExpected, cellFormula, expectedRef, 0);
        }
    }
    
    
    protected Object[][] getExpectedArrayResult(int rowIndex, CellRangeAddress formulaRange, int sheetIndex) {
        int width = formulaRange.getLastColumn() - formulaRange.getFirstColumn() + 1;
        int height = formulaRange.getLastRow() - formulaRange.getFirstRow() + 1;
        
        String baseCell = getExpectedValueCellRef(rowIndex);
        
        Object[][] result = new Object[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = getCell( baseCell, x, y, sheetIndex );
                Object value;
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_BLANK:
                    default:
                        value = null;
                        break;

                    case Cell.CELL_TYPE_BOOLEAN:
                        value = cell.getBooleanCellValue();
                        break;

                    case Cell.CELL_TYPE_ERROR:
                        value = FormulaError.forInt( cell.getErrorCellValue() );
                        break;

                    case Cell.CELL_TYPE_NUMERIC:
                        value = cell.getNumericCellValue();
                        break;

                    case Cell.CELL_TYPE_STRING:
                        value = cell.getStringCellValue();
                        break;
                }
                
                result[x][y] = value;
            }
        }
        
        return result;
    }
    
    /**
     * Calculates formula in first cell of range and gets resulting data
     * for all cells in range. 
     * @param range The cell range.
     * @param sheetIndex 0-based index of sheet in workbook.
     * @return Array of result data.
     */
    protected Object[][] calculateArrayFormula(CellRangeAddress range, int sheetIndex) {
        int width = range.getLastColumn() - range.getFirstColumn() + 1;
        int height = range.getLastRow() - range.getFirstRow() + 1;
        
        Cell firstCell = getCell(range, 0, 0, sheetIndex);
        
        // calculate formula. onse the formula is calculated for one cell
        // it should be calculated for all cells within corresponding range.
        calculateNumericFormula(firstCell);
        
        Object[][] result = new Object[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = getCell(range, x, y, sheetIndex);
                
                Object value;
                switch (cell.getCachedFormulaResultType()) {
                    case Cell.CELL_TYPE_BOOLEAN:
                        value = cell.getBooleanCellValue();
                        break;

                    case Cell.CELL_TYPE_ERROR:
                        value = FormulaError.forInt( cell.getErrorCellValue() );
                        break;

                    case Cell.CELL_TYPE_NUMERIC:
                        value = cell.getNumericCellValue();
                        break;

                    case Cell.CELL_TYPE_STRING:
                        value = cell.getStringCellValue();
                        break;

                    default:
                        value = null;
                        break;
                }
                result[x][y] = value;
            }
        }
        
        return result;
    }

    /**
     * Gets reference to cell where testing formula is defined.
     * @param rowIndex Index of testing row.
     * @return Reference to cell, like C5.
     * @see #getExpectedValueCellRef(int)
     */
    abstract protected String getFormulaCellRef(int rowIndex);
    
    /**
     * Gets reference to cell where expected value is specified.
     * @param rowIndex Index of testing row.
     * @return Reference to cell, like D5.
     * @see #getFormulaCellRef(int)
     */
    abstract protected String getExpectedValueCellRef(int rowIndex);

    /**
     * Gets index of workbook sheet where tested formula(s) defined.
     * @return 0-based index of the sheet.
     */
    abstract protected int getTestSheetIndex();
    
    /**
     * Gets index of row where formula is started.
     * @return 1-based index of the row. 
     */
    abstract protected int getTestStartingRow();
    
    /**
     * Gets string used to stop tests execution. The tests will be stopped
     * if formula cell in file contains exactly this value. 
     * @return Not-null value used to stop tests.
     */
    abstract protected String getFunctionEndString();
}

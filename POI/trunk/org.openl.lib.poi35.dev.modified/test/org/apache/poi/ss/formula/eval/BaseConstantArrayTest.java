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

/**
 * Base class for <i>ConstantArray</i> test cases - one test case per
 * ConstantArray file (xsls, xls etc.)
 */
abstract public class BaseConstantArrayTest extends BaseFormulaTest {

    /**
     * Creates test case instance.
     */
    protected BaseConstantArrayTest() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    protected BaseConstantArrayTest(String name) {
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
     * Tests all formulas from <i>ConstantArray</i> file.
     */
    public void testConstantFormula() {
        
        // to test a particular formula, not all formulas in Excel file, this
        // array should contain 1-based number of row where formula is defined.
        // all formulas will be tested if this array is empty. 
        int[] testRows = {};
        
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
                    continue;
                }
            }
            
            String formulaRef = getFormulaCellRef(rowIndex);
            Cell cellFormula = getCell( formulaRef, sheetIndex);
            
            // check is it end of functions:
            if (cellFormula != null && cellFormula.getCellType() == Cell.CELL_TYPE_STRING) {
                if (getFunctionEndString().equals(cellFormula.getStringCellValue())) {
                    // break the loop when END-OF-FUNCTIONS is reached.
                    break;
                }
            }
            
            String expectedRef = getExpectedValueCellRef(rowIndex);
            Cell cellExpected = getCell( expectedRef, sheetIndex);
            
            // check are that function and expected value cells:
            if (cellFormula != null && cellExpected != null && cellFormula.getCellType() == Cell.CELL_TYPE_FORMULA) {
                try {
                    checkFormulaCalculation(formulaRef, cellFormula, cellExpected, sheetIndex);
                    passedFormulasCount++;
                } catch (AssertionFailedError e) {
                    printShortStackTrace(e);
                    failedFormulas.add( formulaRef );
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    failedFormulas.add( formulaRef );
                }
            }
        }
        
        if (failedFormulas.size() > 0) {
            // test failed.
            fail( MessageFormat.format("Passed {0}, failed {1} formula(s): {2}", passedFormulasCount, failedFormulas.size(), failedFormulas) );
        }
    }
    
    /**
     * Checks formula calculation.
     * @param formulaRef Reference to cell with formula, like C5.
     * @param expectedRef Reference to cell with expected value, like D5.
     * @param sheetIndex Zero-based index of sheet with test data.
     */
    protected void checkFormulaCalculation(String formulaRef, Cell cellFormula, Cell cellExpected, int sheetIndex) {
        // both cells exist, check are they define test case:
        switch (cellExpected.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC: {
                double expected = cellExpected.getNumericCellValue();
                double actual = calculateNumericFormula(cellFormula);
                assertEquals(failedMessage(formulaRef), expected, actual);
                break;
            }
                
            case Cell.CELL_TYPE_STRING: {
                String expected = cellExpected.getStringCellValue();
                
                // expected may contain description of expected error.
                // check, is it error:
                try {
                    FormulaError fe = FormulaError.forString( expected );
                    FormulaError actual = calculateFormulaError(cellFormula);
                    assertEquals(failedMessage(formulaRef), fe, actual);

                } catch (IllegalArgumentException ex) {
                    // this is not an error, check String value:
                    String actual = calculateStringFormula(cellFormula);
                    assertEquals(failedMessage(formulaRef), expected, actual);
                }
                break;
            }
                
            case Cell.CELL_TYPE_BOOLEAN:
                boolean expected = cellExpected.getBooleanCellValue();
                boolean actual = calculateBooleanFormula(cellFormula);
                assertEquals(failedMessage(formulaRef), expected, actual);
                break;
                
            case Cell.CELL_TYPE_ERROR:
            {
            	FormulaError fer = FormulaError.forInt(cellExpected.getErrorCellValue());
                FormulaError result = calculateFormulaError(cellFormula);
                assertEquals(failedMessage(formulaRef), fer, result);
            }
            	break;
                
            default:
                // cells reference formula and/or value of unknown type.
                // this is not part of test case, ignore. 
                break;
        }
    }
    
    /**
     * Gets reference to cell where testing formula is defined.
     * @param rowIndex Index of testing row.
     * @return Reference to cell, like C5.
     * @see #getExpectedValueCellRef(int)
     */
    protected String getFormulaCellRef(int rowIndex) {
        final String columnFormula = "C";
        return columnFormula + "" + rowIndex;
    }
    
    /**
     * Gets reference to cell where expected value is specified.
     * @param rowIndex Index of testing row.
     * @return Reference to cell, like D5.
     * @see #getFormulaCellRef(int)
     */
    protected String getExpectedValueCellRef(int rowIndex) {
        final String columnExpected = "D";
        return columnExpected + "" + rowIndex;
    }

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

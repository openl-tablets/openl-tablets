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

import org.apache.poi.ss.usermodel.Cell;

/**
 * Base class for <i>ArrayFormulaFunctions</i> test cases - one test case per
 * ArrayFormulaFunctions file (xsls, xls etc.)
 */
abstract public class BaseArrayFormulaFunctionsTest extends BaseArrayFormulaTest {

    /**
     * Creates test case instance.
     */
    protected BaseArrayFormulaFunctionsTest() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    protected BaseArrayFormulaFunctionsTest(String name) {
        super(name);
    }
    
    /**
     * Tests formula calculation when value for cell is changed,
     * in addition to base tests.
     */
    public void testPowerArrayChanged() {
        if (testRows.length != 0){
        	// skip test
        	assertTrue(true);
        }
    	
        Cell cellFormula = getCell("C16", getTestSheetIndex());

        Cell cellExpected = getCell("I16", getTestSheetIndex());
        assertEquals("original data " + failedMessage("C16") , cellExpected.getNumericCellValue(), calculateNumericFormula(cellFormula));
        
        Cell cellData = getCell("A15", getTestSheetIndex());
        cellData.setCellValue( 2 );
        assertEquals("changed data " + failedMessage("C16"), 4.0, calculateNumericFormula(cellFormula));
    }
    
    
    /**
     * Tests one cell calculation not using a range,
     * in addition to base tests.
     */
    public void testOneCellCalculation() {
        if (testRows.length != 0){
        	// skip test
        	assertTrue(true);
        }

    	
    	String[] formulas = {"C4", "D4", "E4"};
        String[] expected = {"I4", "J4", "K4"};
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(getTestSheetIndex(), expected[i], formulas[i], 0);
        }
    }
    
    /**
     * Tests specific cells using not-null delta (precision specified),
     * in addition to base tests.
     */
    public void testOneCellCalculationWithDelta() {
        if (testRows.length != 0){
        	// skip test
        	assertTrue(true);
        }

    	
    	String[] formulas = {"B102", "B103", "C102", "C103"};
        String[] expected = {"I102", "I103", "J102", "J103"};
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(getTestSheetIndex(), expected[i], formulas[i], 0.001);
        }
    }
    
    /**
     * Asserts two cell in workbook.
     * @param sheetIndex Index of sheet where cells are stored.
     * @param expectedRef Cell with expected numeric value.
     * @param formulaRef Cell with numeric formula to check.
     * @param delta Allowed delta comparing two <b>double</b> values.
     */
    private void assertEquals(int sheetIndex, String expectedRef, String formulaRef, double delta) {
        Cell cellExpected = getCell(expectedRef, sheetIndex);
        Cell cellFormula = getCell(formulaRef, sheetIndex);
        
        double exp = cellExpected.getNumericCellValue();
        double actual = calculateNumericFormula(cellFormula);
        assertEquals(failedMessage(formulaRef), exp, actual, delta);
    }

    /**
     * Gets reference to cell where testing formula is defined.
     * @param rowIndex Index of testing row.
     * @return Reference to cell, like C5.
     * @see #getExpectedValueCellRef(int)
     */
    @Override
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
    @Override
    protected String getExpectedValueCellRef(int rowIndex) {
        final String columnExpected = "I";
        return columnExpected + "" + rowIndex;
    }

    @Override
    protected int getTestSheetIndex() {
        return 0;
    }

    @Override
    protected int getTestStartingRow() {
        return 4;
    }

    @Override
    protected String getFunctionEndString() {
        return "<END-OF-FUNCTIONS>";
    }
}

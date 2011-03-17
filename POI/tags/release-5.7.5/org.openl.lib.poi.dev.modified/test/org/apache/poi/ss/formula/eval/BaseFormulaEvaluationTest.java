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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;

import junit.framework.AssertionFailedError;

import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport;
import org.apache.poi.hssf.record.formula.udf.AggregatingUDFFinder;
import org.apache.poi.hssf.record.formula.udf.DefaultUDFFinder;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Base class for <i>ArrayFormula</i> test cases - one test case per
 * ArrayFormula file (xsls, xls etc.)
 */
abstract public class BaseFormulaEvaluationTest extends BaseArrayFormulaTest {
    
    private UDFFinder udfFinder;

    /**
     * Creates test case instance.
     */
    protected BaseFormulaEvaluationTest() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    protected BaseFormulaEvaluationTest(String name) {
        super(name);
    }

    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // register the two test UDFs in a UDF finder,
        // to be passed to UDF formula evaluator
        UDFFinder udff1 = new DefaultUDFFinder(
            new String[] { MyFuncWithoutArraySupport.class.getSimpleName()/*"MyFuncWithoutArraySupport"*/, },
            new FreeRefFunction[] { new MyFuncWithoutArraySupport(), }
            );
        UDFFinder udff2 = new DefaultUDFFinder(
            new String[] { MyFuncWithArraySupport.class.getSimpleName()/*"MyFuncWithArraySupport"*/, },
            new FreeRefFunction[] { new MyFuncWithArraySupport(), }
            );
        udfFinder = new AggregatingUDFFinder(udff1, udff2);
    }

    @Override
    /**
     * Overrides formula calculation method to support UDF functions
     * in this test. Calls {@link #calculateMyFunctionFormula} method
     * if function name is started with <i>MyFunc</i> .
     * @see 
     */
    protected double calculateNumericFormula(Cell cell) {
        
        String formula = cell.getCellFormula();
        if (formula != null && formula.startsWith("MyFunc")) {
            return calculateMyFunctionFormula(cell);
            
        } else {
            return super.calculateNumericFormula(cell);
        }
    }


    /**
     * Common method for UDF function testing.
     */
    protected double calculateMyFunctionFormula(Cell cell) {
       
        // Calculate formula (we use cell from  range)   
        Workbook wb = getWorkbook();

        FormulaEvaluator eval;
        if (wb instanceof HSSFWorkbook) {
            eval = HSSFFormulaEvaluator.create((HSSFWorkbook)wb, null, udfFinder);
        } else {
            eval = XSSFFormulaEvaluator.create((XSSFWorkbook)wb, null, udfFinder);
        }
        
        // evaluate the formula. this method should update all
        // cells in formula range.
        int type = eval.evaluateFormulaCell( cell );

        String message = MessageFormat.format("Incorrect type for {0} formula evaluation." + resource(), cell.getCellFormula());
        assertEquals(message, Cell.CELL_TYPE_NUMERIC, type);
        return cell.getNumericCellValue();
    }
    

    // Define UDF: My Functions
    private static class MyFuncWithoutArraySupport implements FreeRefFunction {
        public MyFuncWithoutArraySupport() {
            //
        }

        public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
            return args[0];
        }
    }

    private static class MyFuncWithArraySupport implements FreeRefFunction, FunctionWithArraySupport {
        public MyFuncWithArraySupport() {
            //
        }

        public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
            return args[0];
        }

        public boolean supportArray(int paramIndex) {
            return true;
        }

        public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
            return args[0];
        }
    }
    
    
    /**
     * Tests formula evaluation functionality
     * if formula is dynamically created using API.
     */
    public void testNewNumericFormulaEvaluation() {
        if (testRows.length !=0){
        	//skip test
        	assertTrue(true);
        }
    	
    	// 1. find place-holder cells in existing file.
        // 2. set formula to the cells
        // 3. evaluate formula and compare with expected values.
        int sheetIndex = getTestSheetIndex();

        Cell cell1 = null;
        Cell cell2 = null;
        
        int rowNum = getNumberOfRows( sheetIndex );
        for (int index = getTestStartingRow(); index < rowNum; index++) {
            cell1 = getCell("A" + index, sheetIndex);
            cell2 = getCell("B" + index, sheetIndex);
            
            if (cell1 != null
                && cell2 != null
                && cell1.getCellType() == cell2.getCellType()
                && cell1.getCellType() == Cell.CELL_TYPE_STRING
                && cell1.getStringCellValue().equalsIgnoreCase("Place")
                && cell2.getStringCellValue().equalsIgnoreCase("Holder")
                ) {
                // place holder cells are found.
                break;
            }
        }
        
        assertNotNull("First place holder cell is not found." + resource(), cell1);
        assertNotNull("Second place holder cell is not found." + resource(), cell2);
        
        Sheet sheet = cell1.getSheet();
        CellRangeAddress range = new CellRangeAddress( cell1.getRowIndex(), cell2.getRowIndex(), cell1.getColumnIndex(), cell2.getColumnIndex() );

        sheet.setArrayFormula("SIN({0.1,0.2,0.3})", range);
        
        calculateNumericFormula(cell1);
        calculateNumericFormula(cell2);
        
        // compare with expected value for SIN(0.1)
        String message = MessageFormat.format("Failed formula calculation for cell A{0}" + resource(), cell1.getRowIndex()+1);
        assertFormulaCell( message, 0.0998334166468282, cell1, 0.0001);
        
        // compare with expected value for SIN(0.2)
        message = MessageFormat.format("Failed formula calculation for cell B{0}" + resource(), cell2.getRowIndex()+1);
        assertFormulaCell(message, 0.198669330795061, cell2, 0.0001);
    }

    /**
     * Testing the <code>Sheet.removeArrayFormula</code> functionality.
     */
    public void testRemoveNumericFormula() {
        if (testRows.length !=0){
        	//skip test
        	assertTrue(true);
        }
        String[] existingFormulaRefs = {"A9", "B9", "C9"};
        
        for (String existingFormulaRef : existingFormulaRefs) {
            Cell cell = getCell(existingFormulaRef, getTestSheetIndex());
            assertNotNull("Expected cell not found "+resource()+":"  + existingFormulaRef, cell);
            assertEquals("Formula expected in cell " + resource() + existingFormulaRef, Cell.CELL_TYPE_FORMULA, cell.getCellType());
        }
        
        // testing the formula remove functionality
        Cell cell1 = getCell( existingFormulaRefs[0], getTestSheetIndex() );
        Sheet sheet = cell1.getSheet();
        sheet.removeArrayFormula( cell1 );

        for (String existingFormulaRef : existingFormulaRefs) {
            Cell cell = getCell(existingFormulaRef, getTestSheetIndex());
            assertNotNull("Expected cell not found: " + resource() + existingFormulaRef, cell);
            assertEquals("Blank cell expected in " + resource() +  existingFormulaRef, Cell.CELL_TYPE_BLANK, cell.getCellType());
        }
    }
    
    
    public void testSaveNewFormula() {
        if (testRows.length !=0){
        	//skip test
        	assertTrue(true);
        }

        Workbook workbook;
        try {
            workbook = createWorkbook( null );
        } catch (IOException e) {
            throw new AssertionFailedError("Unexpected error, IOException may not be thrown creating workbook.");
        }
        Sheet sheet = workbook.createSheet();
        Row rowd = sheet.createRow((short) (0));
        Cell cd = rowd.createCell((short) 0);
        CellRangeAddress range = new CellRangeAddress(0,1,0,1);
        sheet.setArrayFormula("SQRT({1,4;9,16})",range);

        // Calculate formula 
        FormulaEvaluator eval = workbook.getCreationHelper().createFormulaEvaluator();
        eval.evaluateFormulaCell(cd);

        // Set tested values
        for (int rowIn = range.getFirstRow(); rowIn <= range.getLastRow();rowIn++) {

            for (int colIn = range.getFirstColumn(); colIn <= range.getLastColumn();colIn++) {
               Cell cell = sheet.getRow(rowIn).getCell(colIn);
               double value = cell.getNumericCellValue();
               Row row = sheet.getRow(rowIn+5);
               if (row == null) {
                   row = sheet.createRow(rowIn+5);
               }
               row.createCell(colIn).setCellValue(value);
            }
        }
        
        // save workbook to temporary file:
        File tmpExcelFile;
        try {
            tmpExcelFile = File.createTempFile("tst", ".xls");
            tmpExcelFile.deleteOnExit();
            
            FileOutputStream out = new FileOutputStream(tmpExcelFile);
            try {
                workbook.write(out);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new AssertionFailedError("Cannot write workbook to temporary file." + resource());
        }

        // Read workbook from temp file
        try {
            workbook = createWorkbook( tmpExcelFile );
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new AssertionFailedError("Cannot read workbook from created temporary file." + resource());
        }

        sheet = workbook.getSheetAt(0);
        // Set 0 values before calculation
        for (int rowIn = range.getFirstRow(); rowIn <= range.getLastRow();rowIn++) {
            for(int colIn = range.getFirstColumn(); colIn <= range.getLastColumn();colIn++) {
               Cell cell = sheet.getRow(rowIn).getCell(colIn);
               if (cell != null) {
                   cell.setCellValue(0.0);
               }
            }
        }

        // Calculate formula (we use cell from  firstRow and firstColumn)           
        eval = workbook.getCreationHelper().createFormulaEvaluator();
        eval.evaluateFormulaCell(sheet.getRow(range.getFirstRow()).getCell(range.getFirstColumn()));
        // Check calculated values
        for (int rowIn = range.getFirstRow(); rowIn <= range.getLastRow();rowIn++) {
            for(int colIn = range.getFirstColumn(); colIn <= range.getLastColumn();colIn++) {
               Cell cell = sheet.getRow(rowIn).getCell(colIn);
               double value = cell.getNumericCellValue();
               cell = sheet.getRow(rowIn+5).getCell(colIn);
               assertEquals("ArrayFormula:"+rowIn+","+colIn + resource(),cell.getNumericCellValue(),value, 0);
            }
        }
    }
    
    /**
     * Creates test case specific workbook instance - HSSF or XSSF.
     * @param file File used creating workbook, may be null.
     * @return New workbook instance.
     * @throws IOException if cannot read workbook from file.
     */
    abstract protected Workbook createWorkbook( File file ) throws IOException;

    @Override
    protected String getExpectedValueCellRef(int rowIndex) {
        final String columnExpected = "F";
        return columnExpected + "" + rowIndex;
    }

    @Override
    protected String getFormulaCellRef(int rowIndex) {
        final String columnFormula = "C";
        return columnFormula + "" + rowIndex;
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

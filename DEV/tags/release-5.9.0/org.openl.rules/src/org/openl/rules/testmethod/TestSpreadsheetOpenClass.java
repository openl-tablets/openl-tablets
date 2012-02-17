package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.calc.SpreadsheetResult;

import org.openl.syntax.impl.IdentifierNode;

/**
 * Open class for test that is testing {@link Spreadsheet}.<br><br>
 * 
 * In case spreadsheet returns {@link SpreadsheetResult} in its signature, it is possible to access<br>
 * any cell of spreadsheet for testing. By convention see {@link SpreadsheetStructureBuilder}.<br><br>
 * 
 * @author DLiauchuk
 *
 * TODO: can be renamed to be used for testing all beans including SpreadsheetResults.
 */
public class TestSpreadsheetOpenClass extends TestMethodOpenClass {
    
    /** identifiers for the spreadsheet cells that are used for testing in test table*/
    private List<IdentifierNode[]> spreadsheetCellsForTest;
    
    public TestSpreadsheetOpenClass(String tableName, Spreadsheet testedSpreadsheet, List<IdentifierNode[]> spreadsheetCellsForTest) {
        super(tableName, testedSpreadsheet);
        this.spreadsheetCellsForTest = new ArrayList<IdentifierNode[]>(spreadsheetCellsForTest);
    }
    
    public List<IdentifierNode[]> getSpreadsheetCellsForTest() {
        return new ArrayList<IdentifierNode[]>(spreadsheetCellsForTest);
    }
}

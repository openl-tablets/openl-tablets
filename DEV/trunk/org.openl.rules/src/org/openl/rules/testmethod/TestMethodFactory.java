package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObject;

public class TestMethodFactory {
    private TestMethodFactory(){};
    
    /**
     * Creates the appropriate {@link IOpenClass} for the test table
     * 
     * @param testedMethod method that is tested by test
     * @param tableName name of that test table
     * @param columnIdentifiers identifiers of the columns of the test table
     * 
     * @return appropriate {@link IOpenClass} for the test table
     */
    public static IOpenClass getTestMethodOpenClass(IOpenMethod testedMethod, String tableName, List<IdentifierNode[]> columnIdentifiers) {        
        if (testedMethod instanceof Spreadsheet) {
            /** Return special open class for the test method that is testing spreadsheet*/
            return new TestSpreadsheetOpenClass(tableName, (Spreadsheet)testedMethod, columnIdentifiers);
        }
        /** For all other cases, use common one*/
        return new TestMethodOpenClass(tableName, testedMethod);
    }    
    
    /**
     * Creates the list of test unit results. 
     * 
     * @param testedMethod method that is tested by test
     * @param testObj instance of the object that was used as input test data
     * @param runningResult result of running the test
     * @param ex exception during test running
     * @return list of test unit results
     */
    public static List<TestUnit> createTestUnits(IOpenMethod testedMethod, DynamicObject testObj, Object runningResult, Throwable ex) {
        List<TestUnit> testUnits = null;
        if (testedMethod instanceof Spreadsheet && ClassUtils.isAssignable(runningResult.getClass(), SpreadsheetResult.class, true)) {
            /** If the tested method is spreadsheet and running result is the child of SpreadsheetResult, create a list of test units.
             * Each test unit is testing one cell value. 
             */            
            SpreadsheetResult runningResultLocal = (SpreadsheetResult) runningResult;
            if (runningResultLocal != null) {
                TestSpreadsheetOpenClass openClass = (TestSpreadsheetOpenClass) testObj.getType();

                /**
                 * Creates a number of test units according to the testing cells
                 */
                testUnits = new ArrayList<TestUnit>(openClass.getSpreadsheetCellsForTest().size());

                for (int i = 0; i < openClass.getSpreadsheetCellsForTest().size(); i++) {
                    String spreadsheetCellName = openClass.getSpreadsheetCellsForTest().get(i)[0].getIdentifier();
                    TestUnit testUnit = new TestUnit(testObj, runningResultLocal.getFieldValue(spreadsheetCellName),
                        ex, spreadsheetCellName);
                    testUnits.add(testUnit);
                }
            }
        } else {
            /** For common case the will be only one test unit,testing result*/
            testUnits = new ArrayList<TestUnit>(1);
            testUnits.add(new TestUnit(testObj, runningResult, ex));
        }
        return testUnits;
    }
}

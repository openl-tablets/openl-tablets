package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

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
    
    public static boolean shouldBeConverted(TestUnit result) {
        return result.getTest().getTestedMethod() instanceof Spreadsheet && ClassUtils.isAssignable(result.getRunningResult()
            .getClass(),
            SpreadsheetResult.class,
            true);
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
    public static List<TestUnit> convertTestUnit(TestUnit result) {
        List<TestUnit> testUnits = null;
        SpreadsheetResult runningResultLocal = (SpreadsheetResult) result.getRunningResult();
        if (runningResultLocal != null) {
            TestSpreadsheetOpenClass openClass = (TestSpreadsheetOpenClass) result.getTest().getTestObject().getType();

            /**
             * Creates a number of test units according to the testing cells
             */
            testUnits = new ArrayList<TestUnit>(openClass.getSpreadsheetCellsForTest().size());

            for (int i = 0; i < openClass.getSpreadsheetCellsForTest().size(); i++) {
                String spreadsheetCellName = openClass.getSpreadsheetCellsForTest().get(i)[0].getIdentifier();
                TestUnit testUnit = new TestUnit(result.getTest(),
                    runningResultLocal.getFieldValue(spreadsheetCellName),
                    result.getException(),
                    spreadsheetCellName);
                testUnits.add(testUnit);
            }
        }
        return testUnits;
    }
}

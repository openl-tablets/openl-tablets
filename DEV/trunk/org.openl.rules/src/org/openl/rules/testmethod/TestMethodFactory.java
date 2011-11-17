package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;
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
            List<IdentifierNode[]> spreadsheetCellsForTest = new ArrayList<IdentifierNode[]>();
            if (columnIdentifiers != null) {
                /** From column identifiers in test table, take only those that follows after the parameters of the tested method.
                 *  It will be the names of the fields for testing.
                 * */
                for (int j = testedMethod.getSignature().getParameterTypes().length; j < columnIdentifiers.size(); j++) {
                    spreadsheetCellsForTest.add(columnIdentifiers.get(j));
                }
            }            
            /** Return special open class for the test method that is testing spreadsheet*/
            return new TestSpreadsheetOpenClass(tableName, (Spreadsheet)testedMethod, spreadsheetCellsForTest);
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
    public static TestUnit updateTestUnit(TestUnit testUnit) {        
        SpreadsheetResult runningResultLocal = (SpreadsheetResult) testUnit.getRunningResult();
        if (runningResultLocal != null) {
            TestSpreadsheetOpenClass openClass = (TestSpreadsheetOpenClass) testUnit.getTest().getTestObject().getType();
            
            List<String> fieldsToTest = new ArrayList<String>(openClass.getSpreadsheetCellsForTest().size());
            for (int i = 0; i < openClass.getSpreadsheetCellsForTest().size(); i++) {
                String fieldNameToTest = openClass.getSpreadsheetCellsForTest().get(i)[1].getIdentifier();
                fieldsToTest.add(fieldNameToTest);
            }
            
            TestResultComparator resultComparator = TestResultComparatorFactory.getBeanComparator(testUnit.getActualResult(), testUnit.getExpectedResult(), fieldsToTest);
            testUnit.setTestUnitResultComparator(new TestUnitResultComparator(resultComparator));
        }
        return testUnit;
    }
}

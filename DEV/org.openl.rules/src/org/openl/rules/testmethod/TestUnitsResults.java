package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.types.IMethodSignature;
import org.openl.util.ClassUtils;

/**
 * Test units results for the test table. Consist of the test suit method
 * itself. And a number of test units that were represented in test table.
 * 
 */
public class TestUnitsResults implements INamedThing {

    private TestSuite testSuite;
    private ArrayList<ITestUnit> testUnits = new ArrayList<>();

    public TestUnitsResults(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    @Override
    public String getName() {
        return testSuite.getDisplayName(INamedThing.SHORT);
    }

    @Override
    public String getDisplayName(int mode) {
        return testSuite.getDisplayName(mode);
    }

    public List<ITestUnit> getTestUnits() {
        return testUnits;
    }

    public List<ITestUnit> getFilteredTestUnits(boolean failuresOnly, int size) {
        if (testUnits != null && failuresOnly) {
            List<ITestUnit> failedUnits = new ArrayList<>();
            for (ITestUnit testUnit : testUnits) {
                if (testUnit.getResultStatus() != TestStatus.TR_OK // Failed unit
                        && (failedUnits.size() < size || size == -1)) {
                    failedUnits.add(testUnit);
                }
            }
            return failedUnits;
        }

        return testUnits;
    }

    public long getExecutionTime() {
        long executionTime = 0;
        if (testUnits != null) {
            for (ITestUnit testUnit : testUnits) {
                executionTime += testUnit.getExecutionTime();
            }
        }

        return executionTime;
    }

    void addTestUnit(ITestUnit testUnit) {
        testUnits.add(testUnit);
    }

    public int getNumberOfFailures() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).getResultStatus() != TestStatus.TR_OK) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfErrors() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).getResultStatus() == TestStatus.TR_EXCEPTION) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfAssertionFailures() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).getResultStatus() == TestStatus.TR_NEQ) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfTestUnits() {
        return testUnits.size();
    }

    public boolean hasDescription() {
        for (ITestUnit testUnit : testUnits) {
            if (testUnit.getTest().getDescription() != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasContext() {
        for (ITestUnit testUnit : testUnits) {
            if (testUnit.getTest().isRuntimeContextDefined()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasExpected() {
        for (ITestUnit testUnit : testUnits) {
            TestDescription test = testUnit.getTest();
            if (test.isExpectedResultDefined() || test.isExpectedErrorDefined()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSpreadsheetResultTester() {
        return ClassUtils.isAssignable(testSuite.getTestedMethod().getType().getInstanceClass(),
            SpreadsheetResult.class);
    }

    public boolean isRunmethod() {
        return testSuite.getTestSuiteMethod().isRunmethod();
    }

    public String[] getTestDataColumnDisplayNames() {
        String[] columnTechnicalNames = getTestDataColumnHeaders();
        String[] columnDisplayNames = new String[columnTechnicalNames.length];
        for (int i = 0; i < columnDisplayNames.length; i++) {
            TestSuiteMethod testSuiteMethod = testSuite.getTestSuiteMethod();
            String displayName = testSuiteMethod == null ? null : testSuiteMethod.getColumnDisplayName(columnTechnicalNames[i]);
            if (displayName != null){
                columnDisplayNames[i] = displayName;
            }else{
                columnDisplayNames[i] = columnTechnicalNames[i];
            }
        }
        return columnDisplayNames;
    }

    private String[] getColumnDisplayNames(String type) {
        List<String> displayNames = new ArrayList<>();
        TestSuiteMethod test = testSuite.getTestSuiteMethod();
        for (int i = 0; i < test.getColumnsCount(); i++) {
            String columnName = test.getColumnName(i);
            if (columnName != null && columnName.startsWith(type)) {
                displayNames.add(test.getColumnDisplayName(columnName));
            }
        }
        return displayNames.toArray(new String[displayNames.size()]);
    }

    public String[] getContextColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.CONTEXT_NAME);
    }

    public String[] getTestResultColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public String[] getTestDataColumnHeaders() {
        IMethodSignature testMethodSignature = testSuite.getTestedMethod().getSignature();

        int len = testMethodSignature.getParameterTypes().length;

        String[] res = new String[len];
        for (int i = 0; i < len; i++) {
            res[i] = testMethodSignature.getParameterName(i);
        }
        return res;
    }
}
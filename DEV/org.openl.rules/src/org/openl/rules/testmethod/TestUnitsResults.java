package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.util.ClassUtils;

/**
 * Test units results for the test table. Consist of the test suit method itself. And a number of test units that were
 * represented in test table.
 */
@RequiredArgsConstructor
public class TestUnitsResults implements INamedThing {

    @Getter
    private final TestSuite testSuite;
    private final ArrayList<ITestUnit> testUnits = new ArrayList<>();

    @Getter
    @Setter
    private boolean testedRulesHaveErrors = false;

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
        if (failuresOnly) {
            var failedUnits = new ArrayList<ITestUnit>();
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
        var executionTime = 0L;
        for (ITestUnit testUnit : testUnits) {
            executionTime += testUnit.getExecutionTime();
        }

        return executionTime;
    }

    void addTestUnit(ITestUnit testUnit) {
        testUnits.add(testUnit);
    }

    public int getNumberOfFailures() {
        if (testedRulesHaveErrors) {
            return getTestSuite().getTests().length;
        }
        var cnt = 0;
        for (var i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).getResultStatus() != TestStatus.TR_OK) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfErrors() {
        var cnt = 0;
        for (var i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).getResultStatus() == TestStatus.TR_EXCEPTION) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfAssertionFailures() {
        var cnt = 0;
        for (var i = 0; i < getNumberOfTestUnits(); i++) {
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
            var test = testUnit.getTest();
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
        return testSuite.getTestSuiteMethod().isRunMethod();
    }

    public String[] getTestDataColumnDisplayNames() {
        var columnTechnicalNames = getTestDataColumnHeaders();
        String[] columnDisplayNames = new String[columnTechnicalNames.length];
        for (var i = 0; i < columnDisplayNames.length; i++) {
            var testSuiteMethod = testSuite.getTestSuiteMethod();
            String displayName = testSuiteMethod == null ? null
                    : testSuiteMethod
                    .getColumnDisplayName(columnTechnicalNames[i]);
            if (displayName != null) {
                columnDisplayNames[i] = displayName;
            } else {
                columnDisplayNames[i] = columnTechnicalNames[i];
            }
        }
        return columnDisplayNames;
    }

    private String[] getColumnDisplayNames(String type) {
        var displayNames = new ArrayList<String>();
        var test = testSuite.getTestSuiteMethod();
        if (test != null) {
            for (var i = 0; i < test.getColumnsCount(); i++) {
                var columnName = test.getColumnName(i);
                if (columnName != null && columnName.startsWith(type)) {
                    displayNames.add(test.getColumnDisplayName(columnName));
                }
            }
        }
        return displayNames.toArray(new String[0]);
    }

    public String[] getContextColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.CONTEXT_NAME);
    }

    public String[] getTestResultColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public String[] getTestErrorColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.EXPECTED_ERROR);
    }

    public String[] getTestDataColumnHeaders() {
        var testMethodSignature = testSuite.getTestedMethod().getSignature();

        var len = testMethodSignature.getParameterTypes().length;

        String[] res = new String[len];
        for (var i = 0; i < len; i++) {
            res[i] = testMethodSignature.getParameterName(i);
        }
        return res;
    }
}

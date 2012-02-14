/**
 * Created Jan 5, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.types.IMethodSignature;

/**
 * Test units results for the test table.
 * Consist of the test suit method itself. And a number of test units that were represented in test table.
 * 
 *
 */
public class TestUnitsResults implements INamedThing {
    
    private TestSuite testSuite;
    private ArrayList<TestUnit> testUnits = new ArrayList<TestUnit>();

    public TestUnitsResults(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public String getName() {
        return testSuite.getDisplayName(INamedThing.SHORT);
    }

    public String getDisplayName(int mode) {
        return testSuite.getDisplayName(mode);
    }

    public ArrayList<TestUnit> getTestUnits() {
        return testUnits;
    }

    public void addTestUnit(TestUnit testUnit) {
        if (TestMethodFactory.shouldBeConverted(testUnit)) {
            testUnits.add(TestMethodFactory.updateTestUnit(testUnit));
        } else {
            testUnits.add(testUnit);
        }
    }
    
    public void addTestUnits(List<TestUnit> testUnits) {
        testUnits.addAll(testUnits);
    }
    
    @Deprecated
    public Object getExpected(int i) {
        return testUnits.get(i).getExpectedResult();
    }

    public int getNumberOfFailures() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() != TestStatus.TR_OK.getStatus()) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfTestUnits() {
        return testUnits.size();
    }
    
    public boolean isAnyUnitHasDescription() {
        for (TestUnit testUnit: testUnits) {
            if (!testUnit.getDescription().equals(TestUnit.DEFAULT_DESCRIPTION)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isSpreadsheetResultTester() {
        return ClassUtils.isAssignable(testSuite.getTestedMethod().getType().getInstanceClass(), SpreadsheetResult.class, false);
    }
    
    @Deprecated
    public Object getUnitResult(int i) {
        return testUnits.get(i).getActualResult();
    }
    
    @Deprecated
    public Object getUnitDescription(int i) {        
        return testUnits.get(i).getDescription();
    }
    
    public String[] getTestDataColumnDisplayNames(){
        String[] columnTechnicalNames = getTestDataColumnHeaders();
        String[] columnDisplayNames = new String[columnTechnicalNames.length];
        for(int i = 0; i < columnDisplayNames.length; i ++){
            columnDisplayNames[i] = testSuite.getTestSuiteMethod().getColumnDisplayName(columnTechnicalNames[i]);
        }
        return columnDisplayNames;
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
    
    @Deprecated
    public Object getTestValue(String fname, int i) {

        TestUnit testUnit = testUnits.get(i);

        return testUnit.getFieldValue(fname);
    }

    public StringBuilder printFailedUnits(StringBuilder sb) {
        sb.append(getName());
        if (getNumberOfFailures() == 0) {
            return sb.append(" - ").append(getNumberOfTestUnits()).append(" tests ALL OK!");
        }

        sb.append(" - ").append(getNumberOfTestUnits()).append(" tests / ").append(getNumberOfFailures())
                .append(" FAILED!");
        
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() != TestStatus.TR_OK.getStatus()) {
                sb.append('\n').append(i+1).append(". ").append(testUnits.get(i).getDescription()).append("\t").append(testUnits.get(i).getExpectedResult()).append(" / ").append(testUnits.get(i).getActualResult());
            }    
        }

        return sb;
    }
    
    @Override
    public String toString() {
        return printFailedUnits(new StringBuilder()).toString();
    }
}
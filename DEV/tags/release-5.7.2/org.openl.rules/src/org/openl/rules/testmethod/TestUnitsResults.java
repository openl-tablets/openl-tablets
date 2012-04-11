/**
 * Created Jan 5, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;

import org.openl.base.INamedThing;
import org.openl.types.IMethodSignature;
import org.openl.types.impl.DynamicObject;

/**
 * Test units results for the test table.
 * Consist of the test suit method itself. And a number of test units that were represented in test table.
 * 
 *
 */
public class TestUnitsResults implements INamedThing {
    
    private TestSuiteMethod testSuite;
    private ArrayList<TestUnit> testUnits = new ArrayList<TestUnit>();

    public TestUnitsResults(TestSuiteMethod testSuite) {
        this.testSuite = testSuite;
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

    public void addTestUnit(DynamicObject testObj, Object res, Throwable ex) {
        
        TestUnit testUnit = new TestUnit(testObj, res, ex);
        testUnits.add(testUnit);
    }
    
    @Deprecated
    public Object getExpected(int i) {
        return testUnits.get(i).getExpectedResult();
    }

    public int getNumberOfFailures() {

        int cnt = 0;

        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() != TestUnitResultComparator.TR_OK) {
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
    
    @Deprecated
    public Object getUnitResult(int i) {
        return testUnits.get(i).getActualResult();
    }
    
    @Deprecated
    public Object getUnitDescription(int i) {        
        return testUnits.get(i).getDescription();
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
            if (testUnits.get(i).compareResult() != TestUnitResultComparator.TR_OK) {
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
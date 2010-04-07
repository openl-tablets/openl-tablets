/**
 * Created Jan 5, 2007
 */
package org.openl.rules.testmethod;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.openl.base.INamedThing;
import org.openl.types.IMethodSignature;
import org.openl.types.impl.DynamicObject;
import org.openl.util.ArrayTool;

public class TestResult implements INamedThing {

    public static final int TR_OK = 0;
    public static final int TR_NEQ = 1;
    public static final int TR_EXCEPTION = 2;

    private TestSuiteMethod testSuite;
    private ArrayList<TestStruct> tests = new ArrayList<TestStruct>();

    public TestResult(TestSuiteMethod testSuite) {
        this.testSuite = testSuite;
    }

    public String getName() {
        return testSuite.getDisplayName(INamedThing.SHORT);
    }

    public String getDisplayName(int mode) {
        return testSuite.getDisplayName(mode);
    }

    public ArrayList<TestStruct> getTests() {
        return tests;
    }

    public void add(DynamicObject testObj, Object res, Throwable ex) {
        tests.add(new TestStruct(testObj, res, ex));
    }

    public int getCompareResult(int i) {

        TestStruct ts = tests.get(i);

        return ts.getEx() != null ? TR_EXCEPTION : (compareResult(getResult(i), getExpected(i)) ? TR_OK : TR_NEQ);
    }

    public Object getExpected(int i) {

        TestStruct ts = tests.get(i);

        return ts.getTestObj().getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public int getNumberOfFailures() {

        int cnt = 0;

        for (int i = 0; i < getNumberOfTests(); i++) {
            if (getCompareResult(i) != TR_OK) {
                ++cnt;
            }
        }

        return cnt;
    }

    public int getNumberOfTests() {
        return tests.size();
    }

    public Object getResult(int i) {
        TestStruct ts = tests.get(i);

        return ts.getEx() != null ? ts.getEx() : ts.getRes();
    }

    public String[] getTestHeaders() {

        IMethodSignature mm = testSuite.getTestedMethod().getSignature();

        int len = mm.getParameterTypes().length;

        String[] res = new String[len];
        for (int i = 0; i < len; i++) {
            res[i] = mm.getParameterName(i);
        }
        return res;
    }

    public Object getTestValue(String fname, int i) {

        TestStruct ts = tests.get(i);

        return ts.getTestObj().getFieldValue(fname);
    }

    public static boolean compareResult(Object res, Object expected) {

        if (res == expected) {
            return true;
        }

        if (res == null || expected == null) {
            return false;
        }

        if (res instanceof Comparable) {
            return ((Comparable<Object>) res).compareTo(expected) == 0;
        }

        if (res.equals(expected)) {
            return true;
        }

        if (res.getClass().isArray() && expected.getClass().isArray()) {
            return compareArrays(res, expected);
        }

        return false;
    }

    private static boolean compareArrays(Object res, Object expected) {

        int len = Array.getLength(res);
        if (len != Array.getLength(expected)) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            if (!compareResult(Array.get(res, i), Array.get(expected, i))) {
                return false;
            }
        }

        return true;
    }

}
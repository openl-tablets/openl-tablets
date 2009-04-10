/**
 * Created Jan 5, 2007
 */
package org.openl.rules.testmethod;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.openl.base.INamedThing;
import org.openl.types.IMethodSignature;
import org.openl.types.impl.DynamicObject;

public class TestResult implements INamedThing {

    static class TestStruct {

        DynamicObject testObj;
        Object res;
        Throwable ex;

        public TestStruct(DynamicObject obj, Object res, Throwable ex) {
            this.ex = ex;
            this.res = res;
            testObj = obj;
        }
    }

    static final public int TR_EXCEPTION = 2, TR_NEQ = 1, TR_OK = 0;

    TestMethodHelper helper;

    ArrayList<TestStruct> tests = new ArrayList<TestStruct>();

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

    static public boolean compareResult(Object res, Object expected) {
        if (res == expected) {
            return true;
        }

        if (res == null || expected == null) {
            return false;
        }

        if (res instanceof Comparable) {
            return ((Comparable) res).compareTo(expected) == 0;
        }

        if (res.equals(expected)) {
            return true;
        }

        if (res.getClass().isArray() && expected.getClass().isArray()) {
            return compareArrays(res, expected);
        }

        return false;//
    }

    public TestResult(TestMethodHelper helper) {
        this.helper = helper;
    }

    public void add(DynamicObject testObj, Object res, Throwable ex) {
        tests.add(new TestStruct(testObj, res, ex));
    }

    /**
     * @return
     */
    public int getCompareResult(int i) {
        TestStruct ts = tests.get(i);

        return ts.ex != null ? TR_EXCEPTION : (compareResult(getResult(i), getExpected(i)) ? TR_OK : TR_NEQ);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        return helper.getTestAll().getDisplayName(mode);
    }

    /**
     * @param i
     * @return
     */
    public Object getExpected(int i) {
        TestStruct ts = tests.get(i);

        return ts.testObj.getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return helper.getTestAll().getDisplayName(INamedThing.SHORT);
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

    /**
     * @return
     */
    public int getNumberOfTests() {
        return tests.size();
    }

    /**
     * @param i
     * @return
     */
    public Object getResult(int i) {
        TestStruct ts = tests.get(i);

        return ts.ex != null ? ts.ex : ts.res;
    }

    public String[] getTestHeaders() {

        IMethodSignature mm = helper.getTestedMethod().getSignature();

        int len = mm.getParameterTypes().length;

        String[] res = new String[len];
        for (int i = 0; i < len; i++) {
            res[i] = mm.getParameterName(i);
        }
        return res;
    }

    /**
     * @param string
     * @param i
     * @return
     */
    public Object getTestValue(String fname, int i) {
        TestStruct ts = tests.get(i);

        return ts.testObj.getFieldValue(fname);

    }
}
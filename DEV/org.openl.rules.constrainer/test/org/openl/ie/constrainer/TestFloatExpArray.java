package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.GoalFloatGenerate;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class TestFloatExpArray extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpArray");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpArray.class));
    }

    public TestFloatExpArray(String name) {
        super(name);
    }

    public void testMinMax() {
        FloatExpArray array = new FloatExpArray(C, 10);
        int[] min = { 1, 2, -21, 12, 14, -3, 0, 0, 4, -21 }, max = { 2, 3, -20, 15, 17, -2, 0, 17, 5, -21 };
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addFloatVar(min[i], max[i], "floatVar#" + i), i);
        }
        assertEquals(-21, array.min(), Constrainer.precision());
        assertEquals(17, array.max(), Constrainer.precision());
    }

    public void testSetGet() {
        FloatExpArray array = new FloatExpArray(C, 5);
        FloatExp[] testArray = new FloatExp[array.size()];
        for (int i = 0; i < array.size(); i++) {
            testArray[i] = C.addFloatVar(-5, 7, "floatVar#" + i);
            array.set(testArray[i], i);
        }

        for (int i = array.size() - 1; i >= 0; i--) {
            assertTrue(((Object) (array.get(i))).equals(testArray[i]));
        }
    }

    public void testSum() {
        FloatExpArray testArray = new FloatExpArray(C, 10);
        for (int i = 0; i < testArray.size(); i++) {
            testArray.set(C.addFloatVar(-5, 5, "floatVar#" + i), i);
        }

        try {
            C.postConstraint(testArray.sum().eq(45));
        } catch (Failure f) {
            fail("test failed");
        }

        try {
            testArray.get(0).setMax(-1);
            fail("test failed");
        } catch (Failure f) {
        }

        boolean flag = C.execute(new GoalFloatGenerate(testArray));
        assertTrue(flag);
        double sum = 0;
        for (int i = 0; i < testArray.size(); i++) {
            try {
                sum += testArray.get(i).value();
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.value()");
            }
        }
        assertEquals(45, sum, testArray.size() * Constrainer.precision());

        // event propagation test
        testArray = new FloatExpArray(C, 2);
        for (int i = 0; i < testArray.size(); i++) {
            testArray.set(C.addFloatVar(-5, 5, "floatVar#" + i), i);
        }
        try {
            C.postConstraint(testArray.sum().eq(0));
            testArray.get(0).setValue(-5);
            C.propagate();
            assertEquals(5, testArray.get(1).value(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }
}
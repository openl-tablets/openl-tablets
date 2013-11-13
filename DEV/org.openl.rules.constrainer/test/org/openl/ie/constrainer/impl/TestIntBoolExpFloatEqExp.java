package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatExpConst;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.GoalFloatGenerate;
import org.openl.ie.constrainer.impl.IntBoolExpFloatEqExp;

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
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class TestIntBoolExpFloatEqExp extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolExpFloatEqExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpFloatEqExp.class));
    }

    public TestIntBoolExpFloatEqExp(String name) {
        super(name);
    }

    public void testAll() {
        FloatVar floatvar1 = C.addFloatVar(-10.0989, 1.3652, "Floatvar1"), floatvar2 = C.addFloatVar(-1.3784, 89.123,
                "floatvar2");
        double max1 = floatvar1.max(), min1 = floatvar1.min();
        double max2 = floatvar2.max(), min2 = floatvar2.min();
        double delta = Constrainer.FLOAT_MIN;
        try {
            C.postConstraint(new IntBoolExpFloatEqExp(floatvar1, floatvar2, delta));
        } catch (Failure f) {
            fail("test failed!");
        }
        assertEquals(Math.max(min1, min2), floatvar1.min(), delta);
        assertEquals(Math.max(min1, min2), floatvar2.min(), delta);
        assertEquals(Math.min(max1, max2), floatvar2.max(), delta);
        assertEquals(Math.min(max1, max2), floatvar2.max(), delta);

        try {
            floatvar1 = C.addFloatVar(0, 10);
            floatvar2 = C.addFloatVar(0, 10);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of Constrainer.addFloatVar(int min, int max)");
        }
        double value = Math.PI;
        // event propagation test
        try {
            C.postConstraint(new IntBoolExpFloatEqExp(floatvar1, floatvar2, delta));
            floatvar1.setValue(value);
            floatvar1.propagate();
            assertEquals(value, floatvar2.value(), delta);
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testIntBoolExpFloatEqExp() {
        FloatExpArray array = new FloatExpArray(C, 10);
        for (int i = 0; i < array.size(); i++) {
            try {
                array.set(C.addFloatVar(0, 100), i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of FlaotExpArray.set(FloatExp, int)");
            }
        }

        try {
            for (int i = 1; i < array.size(); i++) {
                C.postConstraint(new IntBoolExpFloatEqExp(array.get(i), array.get(0), Constrainer.FLOAT_MIN));
            }
            C.postConstraint(new IntBoolExpFloatEqExp(new FloatExpConst(C, Math.sqrt(3) / 2), array.get(0),
                    Constrainer.FLOAT_MIN));
        } catch (Failure f) {
            fail("test failed!");
        }

        boolean flag = C.execute(new GoalFloatGenerate(array));
        assertTrue(flag);
        try {
            for (int i = 0; i < array.size(); i++) {
                assertEquals(Math.sqrt(3) / 2, array.get(i).value(), Constrainer.FLOAT_MIN);
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
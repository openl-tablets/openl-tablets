package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpMulExp;

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

public class TestFloatExpMulExp extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpAddExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpMulExp.class));
    }

    public TestFloatExpMulExp(String name) {
        super(name);
    }

    public void testAscendingEventPropagation() {
        FloatVar floatVar1 = C.addFloatVar(-5, 3, ""), floatVar2 = C.addFloatVar(-4, 7, "");
        FloatExp floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(-35, floatExp.min(), Constrainer.precision());
        assertEquals(21, floatExp.max(), Constrainer.precision());

        try {
            floatVar1.setMin(-1);
            floatVar2.setMin(-3);
            floatVar1.setMax(2);
            floatVar2.setMax(1);
            // floatVar1[-1, 2], floatVar2[-3, 1]
            C.propagate();
            assertEquals(-6, floatExp.min(), Constrainer.precision());
            assertEquals(3, floatExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testConstraintPropagation() {
        FloatVar floatVar1 = C.addFloatVar(-5, 3, ""), floatVar2 = C.addFloatVar(-4, 1, "");
        FloatExp floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        try {
            C.postConstraint(floatExp.le(-10));
            System.out.println(floatVar1.min());
            floatVar1.setMax(2);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testMinMax() {
        // max1 < 0 && max2< 0
        FloatVar floatVar1 = C.addFloatVar(-5, -1, ""), floatVar2 = C.addFloatVar(-5, -2, "");
        FloatExp floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(2, floatExp.min(), Constrainer.precision());
        assertEquals(25, floatExp.max(), Constrainer.precision());

        // max1 < 0, max2 = 0
        floatVar1 = C.addFloatVar(-5, -1, "");
        floatVar2 = C.addFloatVar(-5, 0, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(0, floatExp.min(), Constrainer.precision());
        assertEquals(25, floatExp.max(), Constrainer.precision());

        // max1<0 , min2 < 0 < max2
        floatVar1 = C.addFloatVar(-5, -1, "");
        floatVar2 = C.addFloatVar(-5, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(-25, floatExp.min(), Constrainer.precision());
        assertEquals(25, floatExp.max(), Constrainer.precision());

        // max1 < 0 , min2 =0
        floatVar1 = C.addFloatVar(-5, -1, "");
        floatVar2 = C.addFloatVar(0, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(-25, floatExp.min(), Constrainer.precision());
        assertEquals(0, floatExp.max(), Constrainer.precision());

        // max1 < 0 , min2 >0
        floatVar1 = C.addFloatVar(-5, -1, "");
        floatVar2 = C.addFloatVar(0.1, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(-25, floatExp.min(), Constrainer.precision());
        assertEquals(-0.1, floatExp.max(), Constrainer.precision());

        // min1 < 0 < max1, min2 < 0 < max2
        floatVar1 = C.addFloatVar(-5, 0.1, "");
        floatVar2 = C.addFloatVar(-0.1, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);

        assertEquals(-25, floatExp.min(), Constrainer.precision());
        assertEquals(0.5, floatExp.max(), Constrainer.precision());

        floatVar1 = C.addFloatVar(-4, 0.1, "");
        floatVar2 = C.addFloatVar(-0.1, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);
        assertEquals(-20, floatExp.min(), Constrainer.precision());
        assertEquals(0.5, floatExp.max(), Constrainer.precision());

        floatVar1 = C.addFloatVar(-400, 0.1, "");
        floatVar2 = C.addFloatVar(-0.1, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);
        assertEquals(-400 * 5, floatExp.min(), Constrainer.precision());
        assertEquals(-400 * (-0.1), floatExp.max(), Constrainer.precision());

        // min1 > 0 , min2 > 0
        floatVar1 = C.addFloatVar(0.01, 0.1, "");
        floatVar2 = C.addFloatVar(2, 5, "");
        floatExp = new FloatExpMulExp(floatVar1, floatVar2);
        assertEquals(0.02, floatExp.min(), Constrainer.precision());
        assertEquals(5 * 0.1, floatExp.max(), Constrainer.precision());

    }

}
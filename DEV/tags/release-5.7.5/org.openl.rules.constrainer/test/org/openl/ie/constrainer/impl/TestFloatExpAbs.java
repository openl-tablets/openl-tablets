package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpAbs;

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

public class TestFloatExpAbs extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpAbs");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpAbs.class));
    }

    public TestFloatExpAbs(String name) {
        super(name);
    }

    public void testMinMax() {
        // min < 0 < max
        // 1)abs(min) > max
        FloatVar floatVar = C.addFloatVar(-8, 5, "");
        FloatExp absExp = new FloatExpAbs(floatVar);
        assertEquals(0, absExp.min(), Constrainer.precision());
        assertEquals(8, absExp.max(), Constrainer.precision());

        // 2)abs(min) < max
        floatVar = C.addFloatVar(-3, 5, "");
        absExp = new FloatExpAbs(floatVar);
        assertEquals(0, absExp.min(), Constrainer.precision());
        assertEquals(5, absExp.max(), Constrainer.precision());

        // min < max < 0
        floatVar = C.addFloatVar(-5, -1, "");
        absExp = new FloatExpAbs(floatVar);
        assertEquals(1, absExp.min(), Constrainer.precision());
        assertEquals(5, absExp.max(), Constrainer.precision());

        // max > min > 0
        floatVar = C.addFloatVar(1, 5, "");
        absExp = new FloatExpAbs(floatVar);
        assertEquals(1, absExp.min(), Constrainer.precision());
        assertEquals(5, absExp.max(), Constrainer.precision());
    }

    public void testSetMinSetMax() {
        FloatVar floatVar = C.addFloatVar(-8, 5, "");
        FloatExp absExp = new FloatExpAbs(floatVar);
        // ascending event propagation
        // setMin
        try {
            assertEquals(0, absExp.min(), Constrainer.precision());
            assertEquals(8, absExp.max(), Constrainer.precision());
            floatVar.setMin(-3);
            assertEquals(-3, floatVar.min(), Constrainer.precision());
            C.propagate();
            assertEquals(0, absExp.min(), Constrainer.precision());
            assertEquals(5, absExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatVar.setMin(double)");
        }

        floatVar = C.addFloatVar(-8, 5, "");
        absExp = new FloatExpAbs(floatVar);
        // setMax
        try {
            assertEquals(0, absExp.min(), Constrainer.precision());
            assertEquals(8, absExp.max(), Constrainer.precision());
            floatVar.setMax(-3);
            assertEquals(-3, floatVar.max(), Constrainer.precision());
            C.propagate();
            assertEquals(3, absExp.min(), Constrainer.precision());
            assertEquals(8, absExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatVar.setMin(double)");
        }

        floatVar = C.addFloatVar(-8, 5, "");
        absExp = new FloatExpAbs(floatVar);

        try {
            C.postConstraint(absExp.ge(3));
            try {
                floatVar.setValue(3 - 2 * Constrainer.precision());
                C.propagate();
                fail("failure!!!");
            } catch (Failure f) {
                /** that's ok */
            }
        } catch (Failure f) {
            fail("");
        }

        try {
            floatVar.setValue(3 - Constrainer.precision());
            C.propagate();
        } catch (Failure f) {
            fail("");
        }

        // descending event propagation
        floatVar = C.addFloatVar(-8, 5, "");
        absExp = new FloatExpAbs(floatVar);
        // setMin
        try {
            absExp.setMax(4);
            C.propagate();
            assertEquals(-4, floatVar.min(), Constrainer.precision());
            assertEquals(4, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed : setMin(double)");
        }
    }
}
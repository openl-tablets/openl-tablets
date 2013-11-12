package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpInverse;

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

public class TestFloatExpInverse extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpInverse");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpInverse.class));
    }

    public TestFloatExpInverse(String name) {
        super(name);
    }

    public void testAscendantEventPropagation() {
        FloatVar floatVar = C.addFloatVar(-1, 1, "");
        FloatExp floatExp = new FloatExpInverse(floatVar);
        try {
            floatVar.setMin(0.5);
            C.propagate();
            assertEquals(2, floatExp.max(), Constrainer.precision());
            assertEquals(1, floatExp.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        floatVar = C.addFloatVar(-1, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatVar.setMax(-0.5);
            C.propagate();
            assertEquals(-2, floatExp.min(), Constrainer.precision());
            assertEquals(-1, floatExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testMax() {
        // (min < 0) && (max <0) : inverse.max = 1/min;
        FloatVar floatVar = C.addFloatVar(-12, -0.5, "");
        FloatExp floatExp = new FloatExpInverse(floatVar);
        assertEquals(1 / (floatVar.min()), floatExp.max(), Constrainer.precision());

        // (min < 0) && (max = 0) : inverse.max = 1/min;
        floatVar = C.addFloatVar(-12, 0, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(1 / (floatVar.min()), floatExp.max(), Constrainer.precision());

        // min = 0 && max > 0 : inverse.max = Double.POSITIVE_INFIFNITY
        floatVar = C.addFloatVar(0, 0.75, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(Double.POSITIVE_INFINITY, floatExp.max(), Constrainer.precision());

        // min > 0 : inverse.max = 1/min
        floatVar = C.addFloatVar(0.001, 1.5, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(1 / (floatVar.min()), floatExp.max(), Constrainer.precision());

        // min < 0 && max > 0 : inverse.max = Double.POSITIVE_INFINITY
        floatVar = C.addFloatVar(-12, 12, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(Double.POSITIVE_INFINITY, floatExp.max(), Constrainer.precision());
    }

    public void testMin() {
        // (min < 0) && (max <0) : inverse.min = 1/max;
        FloatVar floatVar = C.addFloatVar(-12, -0.5, "");
        FloatExp floatExp = new FloatExpInverse(floatVar);
        assertEquals(1 / (floatVar.max()), floatExp.min(), Constrainer.precision());

        // (min < 0) && (max = 0) : inverse.min = Double.NEGATIVE_INFINITY;
        floatVar = C.addFloatVar(-12, 0, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(Double.NEGATIVE_INFINITY, floatExp.min(), Constrainer.precision());

        // min = 0 && max > 0 : inverse.min = 1/max
        floatVar = C.addFloatVar(0, 0.75, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(1 / floatVar.max(), floatExp.min(), Constrainer.precision());

        // min > 0 : inverse.min = 1/max
        floatVar = C.addFloatVar(0.001, 1.5, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(1 / (floatVar.max()), floatExp.min(), Constrainer.precision());

        // min < 0 && max > 0 : inverse.min = Double.NEGATIVE_INFINITY
        floatVar = C.addFloatVar(-12, 12, "");
        floatExp = new FloatExpInverse(floatVar);
        assertEquals(Double.NEGATIVE_INFINITY, floatExp.min(), Constrainer.precision());
    }

    public void testPostConstraint() {
        FloatVar floatVar = C.addFloatVar(-1, 1, "");
        FloatExp floatExp = new FloatExpInverse(floatVar);
        try {
            C.postConstraint(floatExp.lt(0.5));
            floatVar.setValue(1);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testSetMax() {
        // min < 0 && max < 0
        FloatVar floatVar = C.addFloatVar(-12, -1, "");
        FloatExp floatExp = new FloatExpInverse(floatVar);

        try {
            floatExp.setMax(-1.0 / 2);
            C.propagate();
            assertEquals(-2, floatVar.min(), Constrainer.precision());
            assertEquals(-1, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min < 0 && max = 0
        floatVar = C.addFloatVar(-12, 0, "");
        floatExp = new FloatExpInverse(floatVar);

        try {
            floatExp.setMax(-1.0 / 2);
            C.propagate();
            assertEquals(-2, floatVar.min(), Constrainer.precision());
            assertEquals(0, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min < 0 < max
        floatVar = C.addFloatVar(-1, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMax(2);
            C.propagate();
            assertEquals(-1, floatVar.min(), Constrainer.precision());
            assertEquals(1, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        floatVar = C.addFloatVar(-1, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMax(-2);
            C.propagate();
            assertEquals(-1.0 / 2, floatVar.min(), Constrainer.precision());
            assertEquals(0, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min = 0 < max
        floatVar = C.addFloatVar(0, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMax(2);
            C.propagate();
            assertEquals(1.0 / 2, floatVar.min(), Constrainer.precision());
            assertEquals(1, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min > 0
        floatVar = C.addFloatVar(1, 2, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMax(0.75);
            C.propagate();
            assertEquals(4.0 / 3, floatVar.min(), Constrainer.precision());
            assertEquals(2, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetMin() {
        // min < 0 && max < 0
        FloatVar floatVar = C.addFloatVar(-12, -1, "");
        FloatExp floatExp = new FloatExpInverse(floatVar);

        try {
            floatExp.setMin(-1.0 / 2);
            C.propagate();
            assertEquals(-2, floatVar.max(), Constrainer.precision());
            assertEquals(-12, floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min < 0 && max = 0
        floatVar = C.addFloatVar(-12, 0, "");
        floatExp = new FloatExpInverse(floatVar);

        try {
            floatExp.setMin(-1.0 / 2);
            C.propagate();
            assertEquals(-2, floatVar.max(), Constrainer.precision());
            assertEquals(-12, floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min < 0 < max
        floatVar = C.addFloatVar(-1, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMin(2);
            C.propagate();
            assertEquals(0, floatVar.min(), Constrainer.precision());
            assertEquals(1.0 / 2, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        floatVar = C.addFloatVar(-1, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMin(-2);
            C.propagate();
            assertEquals(-1, floatVar.min(), Constrainer.precision());
            assertEquals(1, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min = 0 < max
        floatVar = C.addFloatVar(0, 1, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMin(2);
            C.propagate();
            assertEquals(1.0 / 2, floatVar.max(), Constrainer.precision());
            assertEquals(0, floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        // min > 0
        floatVar = C.addFloatVar(1, 2, "");
        floatExp = new FloatExpInverse(floatVar);
        try {
            floatExp.setMin(0.75);
            C.propagate();
            assertEquals(4.0 / 3, floatVar.max(), Constrainer.precision());
            assertEquals(1, floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
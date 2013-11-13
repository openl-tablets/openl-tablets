package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpExponent;

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

public class TestFloatExpExponent extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpExponent");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpExponent.class));
    }

    public TestFloatExpExponent(String name) {
        super(name);
    }

    public void testAscendantEventPropagation() {
        FloatVar floatVar = C.addFloatVar(-4, 5, "");
        FloatExp floatExp = new FloatExpExponent(floatVar);
        try {
            floatVar.setMin(0);
            C.propagate();
            assertEquals(1, floatExp.min(), Constrainer.precision());

            floatVar.setMax(1);
            C.propagate();
            assertEquals(Math.E, floatExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testPostConstraint() {
        FloatVar floatVar = C.addFloatVar(-4, 5, "");
        FloatExp floatExp = new FloatExpExponent(floatVar);
        try {
            try {
                C.postConstraint(floatExp.lt(1));
            } catch (Failure f) {
                fail("Constrainer.postConstraint(IntBoolExp) failed");
            }
            floatVar.setValue(2 * Constrainer.precision());
            C.propagate();
            fail("test failed");
        } catch (Failure f) {/* that's ok */
        }
    }

    public void testSetMax() {
        FloatVar floatVar = C.addFloatVar(-4, 5, "");
        FloatExp floatExp = new FloatExpExponent(floatVar);
        assertEquals(Math.exp(floatVar.min()), floatExp.min(), Constrainer.precision());
        assertEquals(Math.exp(floatVar.max()), floatExp.max(), Constrainer.precision());
        try {
            floatExp.setMax(Math.exp(4));
            assertEquals(4, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetMin() {
        FloatVar floatVar = C.addFloatVar(-4, 5, "");
        FloatExp floatExp = new FloatExpExponent(floatVar);
        assertEquals(Math.exp(floatVar.min()), floatExp.min(), Constrainer.precision());
        assertEquals(Math.exp(floatVar.max()), floatExp.max(), Constrainer.precision());
        try {
            floatExp.setMin(2);
            assertEquals(Math.log(2), floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
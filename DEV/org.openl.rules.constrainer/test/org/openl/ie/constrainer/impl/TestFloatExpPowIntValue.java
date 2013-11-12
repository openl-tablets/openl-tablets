package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpPowIntValue;

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

public class TestFloatExpPowIntValue extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpPowIntValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpPowIntValue.class));
    }

    public TestFloatExpPowIntValue(String name) {
        super(name);
    }

    public void testAscendingEventPropagation() {
        FloatVar floatVar = C.addFloatVar(-3, 5, "");
        FloatExp floatExp = new FloatExpPowIntValue(floatVar, 3);
        try {
            floatVar.setMin(0);
            floatVar.setMax(3);
            C.propagate();
            assertEquals(0, floatExp.min(), Constrainer.precision());
            assertEquals(27, floatExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }

        floatVar = C.addFloatVar(-4, 3, "");
        floatExp = new FloatExpPowIntValue(floatVar, 4);
        try {
            floatVar.setMin(1);
            floatVar.setMax(2);
            C.propagate();
            assertEquals(1, floatExp.min(), Constrainer.precision());
            assertEquals(16, floatExp.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testConstraintPropagation() {
        FloatVar floatVar = C.addFloatVar(-3, 5, "");
        FloatExp floatExp = new FloatExpPowIntValue(floatVar, 3);
        try {
            C.postConstraint(floatExp.le(-0.278));
            floatVar.setMin(Math.pow(-0.278, 1 / 3) + 2 * Constrainer.precision());
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testMinMax() {
        FloatVar floatVar = C.addFloatVar(-3, 5, "");
        FloatExp floatExp = new FloatExpPowIntValue(floatVar, 3);
        assertEquals(-27, floatExp.min(), Constrainer.precision());
        assertEquals(125, floatExp.max(), Constrainer.precision());

        floatVar = C.addFloatVar(-3, 2, "");
        floatExp = new FloatExpPowIntValue(floatVar, 4);
        assertEquals(0, floatExp.min(), Constrainer.precision());
        assertEquals(81, floatExp.max(), Constrainer.precision());
    }
}
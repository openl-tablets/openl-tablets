package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpLog;

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

public class TestFloatExpLog extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpLog");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpLog.class));
    }

    public TestFloatExpLog(String name) {
        super(name);
    }

    public void testAscendantEventPropagation() {
        FloatVar floatVar = C.addFloatVar(0, 5, "");
        FloatExp floatExp = new FloatExpLog(floatVar);
        try {
            floatVar.setMin(1);
            C.propagate();
            assertEquals(0, floatExp.min(), Constrainer.precision());
            C.postConstraint(floatExp.gt(Math.log(4) + 2 * Constrainer.precision()));
            try {
                floatVar.setMax(4);
                C.propagate();
                fail("test failed");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testPostConstraint() {
        FloatVar floatVar = C.addFloatVar(0, 100, "");
        FloatExp floatExp = new FloatExpLog(floatVar);
        try {
            try {
                C.postConstraint(floatExp.gt(Math.log(101)));
                floatVar.setValue(99);
                C.propagate();
                fail("Constrainer.postConstraint(IntBoolExp) failed");
            } catch (Failure f) {
            }

            try {
                C.postConstraint(floatExp.gt(Math.log(50)));
            } catch (Failure f) {
            }

            floatVar.setValue(49);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {/* that's ok */
        }
    }

    public void testSetMax() {
        FloatVar floatVar = C.addFloatVar(0, 100, "");
        FloatExp floatExp = new FloatExpLog(floatVar);
        assertEquals(Math.log(floatVar.min()), floatExp.min(), Constrainer.precision());
        assertEquals(Math.log(floatVar.max()), floatExp.max(), Constrainer.precision());
        try {
            floatExp.setMin(2);
            assertEquals(Math.exp(2), floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetMin() {
        FloatVar floatVar = C.addFloatVar(0, 100, "");
        FloatExp floatExp = new FloatExpLog(floatVar);
        assertEquals(Math.log(floatVar.min()), floatExp.min(), Constrainer.precision());
        assertEquals(Math.log(floatVar.max()), floatExp.max(), Constrainer.precision());
        try {
            floatExp.setMax(Math.log(4));
            assertEquals(4, floatVar.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
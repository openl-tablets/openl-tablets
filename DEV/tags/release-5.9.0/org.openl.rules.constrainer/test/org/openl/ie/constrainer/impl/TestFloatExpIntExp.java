package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.FloatExpIntExp;

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

public class TestFloatExpIntExp extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpIntExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpIntExp.class));
    }

    public TestFloatExpIntExp(String name) {
        super(name);
    }

    public void testSetMax() {
        IntVar intVar = C.addIntVar(0, 10, "");
        FloatExp floatExp = new FloatExpIntExp(intVar);
        try {
            floatExp.setMax(0 + 2 * Constrainer.precision());
            assertEquals(0, intVar.max());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetMin() {
        IntVar intVar = C.addIntVar(0, 10, "");
        FloatExp floatExp = new FloatExpIntExp(intVar);
        try {
            floatExp.setMin(10 - 2 * Constrainer.precision());
            assertEquals(10, intVar.min());
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
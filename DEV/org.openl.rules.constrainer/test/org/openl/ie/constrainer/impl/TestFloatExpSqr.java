package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpSqr;

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

public class TestFloatExpSqr extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpSqr");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpSqr.class));
    }

    public TestFloatExpSqr(String name) {
        super(name);
    }

    public void testConstraintPropagation() {
        FloatVar floatVar = C.addFloatVar(0.125, 4, "");
        FloatExp floatExp = new FloatExpSqr(floatVar);
        try {
            C.postConstraint(floatExp.le(1));
            floatVar.setMin(1.1);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }
}
package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.FloatVar;

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
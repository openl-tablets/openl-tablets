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

public class TestFloatExpMultiplyPositive extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpMultiplyPositive");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpMultiplyPositive.class));
    }

    public TestFloatExpMultiplyPositive(String name) {
        super(name);
    }

    public void testConstraintPropagation() {
        FloatVar floatVar = C.addFloatVar(-3, 4, "");
        FloatExp floatExp = new FloatExpMultiplyPositive(floatVar, 3.4);
        try {
            C.postConstraint(floatExp.le(-1));
            floatVar.setValue(2);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

}
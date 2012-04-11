package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatExpMultiplyPositive;

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
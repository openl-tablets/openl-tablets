package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntVar;

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
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class TestIntBoolExpEqValue extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolExpEqValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpEqValue.class));
    }

    public TestIntBoolExpEqValue(String name) {
        super(name);
    }

    public void testIntExpLessValue() {
        IntVar intvar1 = C.addIntVar(-10, 0), intvar2 = C.addIntVar(5, 10);
        IntBoolExp correctExp = new IntBoolExpLessValue(intvar1, -9);
        IntBoolExp wrongExp = new IntBoolExpLessValue(intvar2, 5);
        try {
            C.postConstraint(correctExp);
        } catch (Failure f) {
            fail("test failed");
        }

        try {
            C.postConstraint(wrongExp);
            fail("test failed");
        } catch (Failure f) {
        }

        intvar1 = C.addIntVar(0, 10);
        try {
            C.postConstraint(new IntBoolExpLessValue(intvar1, 7));
            assertEquals(6, intvar1.max());
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
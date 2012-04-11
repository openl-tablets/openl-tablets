package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntBoolExpLessValue;

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
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class TestIntBoolExpLessValue extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolExpLessValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpLessValue.class));
    }

    public TestIntBoolExpLessValue(String name) {
        super(name);
    }

    public void testAll() {
        IntVar intvar = C.addIntVar(0, 10);
        IntBoolExpLessValue boolexp = new IntBoolExpLessValue(intvar, 8);
        // test execute
        try {
            C.postConstraint(boolexp);
            assertEquals(8 - 1, intvar.max());
            assertEquals(0, intvar.min());
        } catch (Failure f) {
            fail("test failed");
        }
        boolexp = new IntBoolExpLessValue(intvar, 0);
        try {
            C.postConstraint(boolexp);
        } catch (Failure f) {/* that's ok! */
        }
    }

}
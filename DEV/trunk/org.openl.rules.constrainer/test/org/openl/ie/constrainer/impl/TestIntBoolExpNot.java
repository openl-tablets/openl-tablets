package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntBoolExpLessExp;
import org.openl.ie.constrainer.impl.IntBoolExpNot;

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
 * @author Sergej Vanskov
 * @version 1.0
 */

public class TestIntBoolExpNot extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolExpNot");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpNot.class));
    }

    public TestIntBoolExpNot(String name) {
        super(name);
    }

    public void testIntBoolExpNot() {
        IntBoolExp exp1 = new IntBoolExpNot(new IntBoolExpConst(C, true));
        assertTrue(!exp1.isTrue());
        exp1 = new IntBoolExpNot(new IntBoolExpConst(C, false));
        assertTrue(exp1.isTrue());

        IntVar intvar1 = C.addIntVar(0, 20), intvar2 = C.addIntVar(5, 15);
        try {
            C.postConstraint(new IntBoolExpLessExp(intvar1, intvar2).not());
            assertEquals(5, intvar1.min());
            intvar2.setMin(7);
            C.propagate();
            assertEquals(7, intvar1.min());
            intvar1.setMax(9);
            C.propagate();
            assertEquals(9, intvar2.max());
        } catch (Failure f) {
            fail("test failed");
        }
    }
}
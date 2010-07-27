package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntExpConst;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntBoolExpEqExp;

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

public class TestIntBoolExpEqExp extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolExpEqExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpEqExp.class));
    }

    public TestIntBoolExpEqExp(String name) {
        super(name);
    }

    public void testAll() {
        IntVar intvar1 = C.addIntVar(0, 10), intvar2 = C.addIntVar(-10, -1);

        IntBoolExp boolexp = new IntBoolExpEqExp(intvar1, intvar2);
        try {
            C.postConstraint(boolexp);
            fail("test failed");
        } catch (Failure f) {
        }

        intvar1 = C.addIntVar(0, 10);
        intvar2 = C.addIntVar(10, 20);
        try {
            C.postConstraint(new IntBoolExpEqExp(intvar1, intvar2));
            boolean flag = C.execute(new GoalGenerate(new IntExpArray(C, intvar1, intvar2)));
            assertTrue(flag);
            assertEquals(10, intvar1.value());
            assertEquals(10, intvar2.value());
        } catch (Failure f) {
            fail("test failed");
        }

        intvar1 = C.addIntVar(0, 10);
        intvar2 = C.addIntVar(5, 15);
        try {
            C.postConstraint(new IntBoolExpEqExp(intvar1, intvar2));
            try {
                intvar2.setMin(11);
                fail("test failed");
            } catch (Failure f) {
            }
            try {
                intvar2.setValue(11);
                fail("test failed");
            } catch (Failure f) {
            }
            try {
                intvar1.setValue(4);
                fail("test failed");
            } catch (Failure f) {
            }
            try {
                intvar1.setMax(4);
                fail("test failed");
            } catch (Failure f) {
            }
            try {
                intvar1.removeValue(10);
                intvar1.removeValue(9);
                intvar1.removeValue(8);
                intvar1.removeValue(7);
                intvar1.removeValue(6);
                intvar1.removeValue(5);
                fail("test failed");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed");
        }

        intvar1 = C.addIntVar(0, 10);
        intvar2 = C.addIntVar(0, 10);
        // event propagation test
        try {
            C.postConstraint(new IntBoolExpEqExp(intvar1, intvar2));
            intvar1.setValue(7);
            intvar1.propagate();
            assertEquals(7, intvar2.value());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testIntBoolExpEqExp() {
        IntExpArray array = new IntExpArray(C, 10, 0, 10, "array");
        int value = 5;
        try {
            IntExp exp = array.get(0);
            for (int i = 1; i < array.size(); i++) {
                C.postConstraint(new IntBoolExpEqExp(exp, array.get(i)));
            }
            C.postConstraint(new IntBoolExpEqExp(new IntExpConst(C, value), array.get(0)));
            boolean flag = C.execute(new GoalGenerate(array));
            assertTrue(flag);
            for (int i = 0; i < array.size(); i++) {
                assertEquals(value, array.get(i).value());
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }
}
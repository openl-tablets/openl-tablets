package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntBoolExpAnd;

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

public class TestIntBoolExpAnd extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolExpAnd");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpAnd.class));
    }

    public TestIntBoolExpAnd(String name) {
        super(name);
    }

    public void testAll() {
        IntBoolExp boolexp1 = new IntBoolExpConst(C, false), boolexp2 = new IntBoolExpConst(C, true);
        IntBoolExp andexp = new IntBoolExpAnd(boolexp1, boolexp2);
        try {
            C.postConstraint(andexp);
            fail("test failed");
        } catch (Failure f) {
        }

        boolexp1 = new IntBoolExpConst(C, true);
        boolexp2 = new IntBoolExpConst(C, false);
        andexp = new IntBoolExpAnd(boolexp1, boolexp2);
        try {
            C.postConstraint(andexp);
            fail("test failed");
        } catch (Failure f) {
        }

        boolexp1 = new IntBoolExpConst(C, false);
        boolexp2 = new IntBoolExpConst(C, false);
        andexp = new IntBoolExpAnd(boolexp1, boolexp2);
        try {
            C.postConstraint(andexp);
            fail("test failed");
        } catch (Failure f) {
        }

        boolexp1 = new IntBoolExpConst(C, true);
        boolexp2 = new IntBoolExpConst(C, true);
        andexp = new IntBoolExpAnd(boolexp1, boolexp2);
        try {
            C.postConstraint(andexp);
        } catch (Failure f) {
            fail("test failed");
        }

        IntVar intvar1 = C.addIntVar(0, 10);
        IntVar intvar2 = C.addIntVar(0, 10);

        boolexp1 = intvar1.gt(5);
        boolexp2 = intvar2.le(5);
        andexp = new IntBoolExpAnd(boolexp1, boolexp2);
        try {
            C.postConstraint(andexp);
        } catch (Failure f) {
            fail("test failed");
        }

        try {
            intvar1.setMax(4);
            intvar1.propagate();
            fail("test failed");
        } catch (Failure f) {
        }

        try {
            intvar2.setMin(6);
            intvar2.propagate();
            fail("test failed");
        } catch (Failure f) {
        }

    }

    public void testIntBoolExpAnd() {
        IntExpArray array = new IntExpArray(C, 10, 0, 9, "array");
        IntBoolExp boolexp = new IntBoolExpConst(C, true);
        for (int i = 0; i < array.size() - 1; i++) {
            for (int j = i + 1; j < array.size(); j++) {
                boolexp = new IntBoolExpAnd(boolexp, array.get(i).ne(array.get(j)));
            }
        }
        try {
            C.postConstraint(boolexp);
            boolean flag = C.execute(new GoalGenerate(array));
            assertTrue(flag);
            int[] valarray = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                valarray[i] = array.get(i).value();
            }
            assertTrue(TestUtils.isAllDiff(valarray));
        } catch (Failure f) {
            fail("test failed");
        }
    }
}
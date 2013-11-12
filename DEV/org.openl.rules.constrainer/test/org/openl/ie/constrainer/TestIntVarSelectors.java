package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntVarSelectorFirstUnbound;
import org.openl.ie.constrainer.IntVarSelectorMaxSize;
import org.openl.ie.constrainer.IntVarSelectorMinMin;
import org.openl.ie.constrainer.IntVarSelectorMinSize;
import org.openl.ie.constrainer.impl.TestUtils;

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

public class TestIntVarSelectors extends TestCase {
    private Constrainer C = new Constrainer("TestIntVarSelectors");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntVarSelectors.class));
    }

    public TestIntVarSelectors(String name) {
        super(name);
    }

    public void testIntVarSelectorFirstUnbound() {
        IntExpArray array = new IntExpArray(C, 10, 0, 9, "array");
        try {
            array.get(0).setValue(3);
            array.get(1).setValue(3);
            array.get(2).setValue(3);
            array.get(4).setValue(3);
            int index = new IntVarSelectorFirstUnbound(array).select();
            assertEquals(3, index);
            array.get(index).setValue(3);
            index = new IntVarSelectorFirstUnbound(array).select();
            assertEquals(5, index);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of IntVar.setValue(int)");
        }
    }

    public void testIntVarSelectorMaxSize() {
        int[] ub = { 2, 1, 5, 4, 3, 8, 7, 9, 6 };
        IntExpArray array = new IntExpArray(C, 9);
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addIntVar(0, ub[i]), i);
        }
        for (int i = 0; i < array.size(); i++) {
            int test_index = new IntVarSelectorMaxSize(array).select();
            int index = new TestUtils.Finder(ub, new TestUtils.IntEqualsTo(9 - i)).findFirst();
            assertEquals(index, test_index);
            try {
                array.get(index).setValue(1);
            } catch (Failure f) {
                fail("test failed due to incorrect behaviour of IntVar.setValue(int)");
            }
        }
    }

    public void testIntVarSelectorMinMin() {
        IntExpArray array = new IntExpArray(C, 9);
        IntVarSelectorMinMin selector = new IntVarSelectorMinMin(array);
        int[] ub = { 2, 1, 5, 4, 3, 8, 7, 9, 6 };
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addIntVar(-ub[i], 0), i);
        }
        for (int i = 0; i < array.size(); i++) {
            int test_index = new IntVarSelectorMinMin(array).select();
            int index = new TestUtils.Finder(ub, new TestUtils.IntEqualsTo(9 - i)).findFirst();
            assertEquals(index, test_index);
            try {
                array.get(index).setValue(-1);
            } catch (Failure f) {
                fail("test failed due to incorrect behaviour of IntVar.setValue(int)");
            }
        }
    }

    public void testIntVarSelectorMinSize() {
        int[] ub = { 2, 1, 5, 4, 3, 8, 7, 9, 6 };
        IntExpArray array = new IntExpArray(C, 9);
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addIntVar(0, ub[i]), i);
        }
        try {
            C.postConstraint(C.allDiff(array));
        } catch (Failure f) {
            fail("test failed due to incorrect work of Constrainer.allDiff(IntExpArray)");
        }
        boolean flag = C.execute(new GoalGenerate(array, new IntVarSelectorMinSize(array), new IntValueSelectorMin()));
        assertTrue(flag);
        for (int i = 0; i < array.size(); i++) {
            int index = new TestUtils.Finder(ub, new TestUtils.IntEqualsTo(i + 1)).findFirst();
            try {
                assertEquals(i, array.get(index).value());
            } catch (Failure f) {
                fail("test failed");
            }
        }
    }

}
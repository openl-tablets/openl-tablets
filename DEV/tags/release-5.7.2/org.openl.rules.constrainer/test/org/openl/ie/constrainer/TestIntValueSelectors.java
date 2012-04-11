package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelectorMax;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntValueSelectorMinMax;

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

public class TestIntValueSelectors extends TestCase {
    private Constrainer C = new Constrainer("TestIntValueSelectors");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntValueSelectors.class));
    }

    public TestIntValueSelectors(String name) {
        super(name);
    }

    public void testIntValueSelectorMax() {
        IntExpArray array = new IntExpArray(C, 101, -50, 50, "array");
        int min = array.min(), max = array.max();
        try {
            C.postConstraint(C.allDiff(array));
            boolean flag = C.execute(new GoalGenerate(array, null, new IntValueSelectorMax()));
            assertTrue(flag);
            for (int i = 0; i < array.size(); i++) {
                assertEquals(max - i, array.get(i).value());
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testIntValueSelectorMin() {
        IntExpArray array = new IntExpArray(C, 101, -50, 50, "array");
        int min = array.min(), max = array.max();
        try {
            C.postConstraint(C.allDiff(array));
            boolean flag = C.execute(new GoalGenerate(array, null, new IntValueSelectorMin()));
            assertTrue(flag);
            for (int i = 0; i < array.size(); i++) {
                assertEquals(min + i, array.get(i).value());
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testIntValueSelectorMinMax() {
        IntExpArray array = new IntExpArray(C, 101, -50, 50, "array");
        int min = array.min(), max = array.max();
        try {
            C.postConstraint(C.allDiff(array));
            boolean flag = C.execute(new GoalGenerate(array, null, new IntValueSelectorMinMax()));
            assertTrue(flag);
            int counter = 0;
            for (int i = 0; i < array.size(); i += 2) {
                assertEquals(min + counter, array.get(i).value());
                counter++;
            }
            counter = 0;
            for (int i = 1; i < array.size(); i += 2) {
                assertEquals(max - counter, array.get(i).value());
                counter++;
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }
}
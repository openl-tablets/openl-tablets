package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntArray;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpElementAt;

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

public class TestIntExpElementAt extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpElementAt");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpElementAt.class));
    }

    public TestIntExpElementAt(String name) {
        super(name);
    }

    public void testContainsMinMax() {
        int[] array = { -100, 75, -50, 25, 0, -25, 50, -75, 100 };
        IntArray intarray = new IntArray(C, array);

        IntVar cursor = C.addIntVar(0, array.length);
        IntExp elemAt = new IntExpElementAt(intarray, cursor);

        // decreasing domain of cursor by successive increasing of it's minimum
        for (int i = 0; i < array.length; i++) {
            try {
                cursor.setMin(i);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect behsviour of IntVar.setMin(int)");
            }

            // check min() max()
            assertEquals(TestUtils.min(TestUtils.subArray(array, i, array.length - 1)), elemAt.min());
            assertEquals(TestUtils.max(TestUtils.subArray(array, i, array.length - 1)), elemAt.max());

            for (int j = i; j < array.length; j++) {
                assertTrue("cursor.setMin caused crush", elemAt.contains(array[j]));
            }
            for (int j = 0; j < i; j++) {
                assertTrue("cursor.setMin caused crush", !elemAt.contains(array[j]));
            }
        }
        // ---------------------------------------------------------------------------------
        cursor = C.addIntVar(0, array.length, "cursor", IntVar.DOMAIN_BIT_FAST);
        elemAt = new IntExpElementAt(intarray, cursor);
        // decreasing domain of cursor by successive removals of it's values
        for (int i = 0; i < array.length - 1; i++) {
            try {
                cursor.removeValue(i);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect behaviour of IntVar.removeValue(int)");
            }

            // check min() max()
            assertEquals(TestUtils.min(TestUtils.subArray(array, i + 1, array.length - 1)), elemAt.min());
            assertEquals(TestUtils.max(TestUtils.subArray(array, i + 1, array.length - 1)), elemAt.max());

            for (int j = i + 1; j < array.length; j++) {
                assertTrue("cursor.removeValue caused crush ", elemAt.contains(array[j]));
            }
            for (int j = 0; j <= i; j++) {
                assertTrue("cursor.removeValue caused crush ", !elemAt.contains(array[j]));
            }
        }
        // ---------------------------------------------------------------------------------
        cursor = C.addIntVar(0, array.length - 1);
        elemAt = new IntExpElementAt(intarray, cursor);
        // decreasing domain of cursor by successive decreasing of it's maximum
        for (int i = 0; i < array.length; i++) {
            try {
                cursor.setMax(array.length - 1 - i);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect behsviour of IntVar.setMax(int)");
            }

            // check min() max()
            assertEquals(TestUtils.min(TestUtils.subArray(array, 0, array.length - 1 - i)), elemAt.min());
            assertEquals(TestUtils.max(TestUtils.subArray(array, 0, array.length - 1 - i)), elemAt.max());

            for (int j = array.length - i; j < array.length; j++) {
                assertTrue("cursor.setMax caused crush", !elemAt.contains(array[j]));
            }
            for (int j = 0; j < array.length - i; j++) {
                assertTrue("cursor.setMax caused crush", elemAt.contains(array[j]));
            }
        }
    }

    public void testIntExpElementAt() {
        try {
            IntArray array = new IntArray(C, new int[] { -12, -122, -30, 14, 25, 56, 87, 134, 12 });

            IntVar cursor1 = C.addIntVar(0, array.size() - 1);
            IntVar cursor2 = C.addIntVar(0, array.size() - 1);
            IntVar cursor3 = C.addIntVar(0, array.size() - 1);

            IntExp exp1 = new IntExpElementAt(array, cursor1);
            IntExp exp2 = new IntExpElementAt(array, cursor2);
            IntExp exp3 = new IntExpElementAt(array, cursor3);

            C.postConstraint(exp1.add(exp2).eq(exp3));
            boolean flag = C.execute(new GoalGenerate(new IntExpArray(C, cursor1, cursor2, cursor3)));
            assertTrue(flag);
            assertTrue((array.get(cursor1.value()) + array.get(cursor2.value())) == array.get(cursor3.value()));
            System.out.println(cursor1.value());
            System.out.println(cursor2.value());
            System.out.println(cursor3.value());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testSetMinSetMaxSetValueRemove() {
        int[] array = { -100, -75, -50, -25, 0, 25, 50, 75, 100 };
        IntArray intarray = new IntArray(C, array);

        IntVar cursor = C.addIntVar(0, array.length);
        IntExp elemAt = new IntExpElementAt(intarray, cursor);

        // decreasing domain of cursor by successive increasing of it's minimum
        for (int i = 0; i < array.length; i++) {
            try {
                elemAt.setMin(array[i]);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect behsviour of IntVar.setMin(int)");
            }

            // check min() max()
            assertEquals(TestUtils.min(TestUtils.subArray(array, i, array.length - 1)), elemAt.min());
            assertEquals(TestUtils.max(TestUtils.subArray(array, i, array.length - 1)), elemAt.max());
            assertEquals(array.length - i, elemAt.size());

            for (int j = i; j < array.length; j++) {
                assertTrue("setMin works incorrect", elemAt.contains(array[j]));
            }
            for (int j = 0; j < i; j++) {
                assertTrue("setMin works incorrect", !elemAt.contains(array[j]));
            }
        }
        // ---------------------------------------------------------------------------------
        cursor = C.addIntVar(0, array.length, "cursor", IntVar.DOMAIN_BIT_FAST);
        elemAt = new IntExpElementAt(intarray, cursor);
        // decreasing domain of cursor by successive removals of it's values
        for (int i = 0; i < array.length - 1; i++) {
            try {
                elemAt.removeValue(array[i]);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect behaviour of IntVar.removeValue(int)");
            }

            // check min() max()
            assertEquals(TestUtils.min(TestUtils.subArray(array, i + 1, array.length - 1)), elemAt.min());
            assertEquals(TestUtils.max(TestUtils.subArray(array, i + 1, array.length - 1)), elemAt.max());
            assertEquals(array.length - i - 1, elemAt.size());

            for (int j = i + 1; j < array.length; j++) {
                assertTrue("removeValue works incorrect", elemAt.contains(array[j]));
            }
            for (int j = 0; j <= i; j++) {
                assertTrue("removeValue works incorrect", !elemAt.contains(array[j]));
            }
        }
        // ---------------------------------------------------------------------------------
        cursor = C.addIntVar(0, array.length - 1);
        elemAt = new IntExpElementAt(intarray, cursor);
        // decreasing domain of cursor by successive decreasing of it's maximum
        for (int i = 0; i < array.length; i++) {
            try {
                elemAt.setMax(array[array.length - 1 - i]);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect behsviour of IntVar.setMax(int)");
            }

            // check min() max()
            assertEquals(TestUtils.min(TestUtils.subArray(array, 0, array.length - 1 - i)), elemAt.min());
            assertEquals(TestUtils.max(TestUtils.subArray(array, 0, array.length - 1 - i)), elemAt.max());
            assertEquals(array.length - i, elemAt.size());

            for (int j = array.length - i; j < array.length; j++) {
                assertTrue("setMax works incorrect", !elemAt.contains(array[j]));
            }
            for (int j = 0; j < array.length - i; j++) {
                assertTrue("setMax works incorrect", elemAt.contains(array[j]));
            }
        }
    }

}
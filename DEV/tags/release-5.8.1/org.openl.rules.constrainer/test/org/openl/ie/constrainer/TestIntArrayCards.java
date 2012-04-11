package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntArrayCards;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

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

public class TestIntArrayCards extends TestCase {

    private Constrainer C = new Constrainer("TestIntArrayCards");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntArrayCards.class));
    }

    public TestIntArrayCards(String name) {
        super(name);
    }

    public void testIntArrayCards() {
        IntExpArray array = new IntExpArray(C, 10, 0, 9, "array");
        int min = array.min();
        int max = array.max();
        int nbRepetitions = 1;
        try {
            IntArrayCards cards = array.cards();
            for (int i = 0; i < array.size(); i++) {
                C.postConstraint(cards.cardAt(i).eq(nbRepetitions));
            }
        } catch (Failure f) {
        }
        boolean flag = C.execute(new GoalGenerate(array));
        assertTrue(flag);
        for (int i = min; i <= max; i++) {
            assertEquals(nbRepetitions, valCounter(array, i));
        }

        /*
         * IntExpArray array1 = new IntExpArray(C, 10, 0, 9, "array"); min =
         * array1.min(); max = array1.max(); try{ IntArrayCards cards =
         * array1.cards(); for (int i=0;i<array1.size();i++)
         * C.postConstraint(cards.cardAt(i).gt(nbRepetitions)); boolean result =
         * C.execute(new GoalGenerate(array1)); assertTrue(result); //fail("test
         * failed!"); }catch(Failure f){}
         */
    }

    public void testMisc() {
        IntExpArray array = new IntExpArray(C, 10, 0, 9, "array");
        try {
            IntArrayCards cards = array.cards();
            C.postConstraint(cards.cardAt(0).eq(array.size()));
            for (int i = 0; i < array.size(); i++) {
                assertEquals(array.min(), array.get(i).value());
            }
        } catch (Failure f) {
            fail("test failed!");
        }

        array = new IntExpArray(C, 10, 0, 9, "array");
        try {
            IntArrayCards cards = array.cards();
            C.postConstraint(cards.cardAt(5).eq(array.size() - 1));
            array.get(4).removeRange(2, 7);
            C.propagate();
            for (int i = 0; i < array.size(); i++) {
                if (i != 4) {
                    assertEquals(5, array.get(i).value());
                }
            }
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    private int valCounter(IntExpArray array, int val) {
        int counter = 0;
        for (int i = 0; i < array.size(); i++) {
            IntExp exp = array.get(i);
            if (exp.bound()) {
                try {
                    if (exp.value() == val) {
                        counter++;
                    }
                } catch (Failure f) {
                }
            }
        }
        return counter;
    }

}
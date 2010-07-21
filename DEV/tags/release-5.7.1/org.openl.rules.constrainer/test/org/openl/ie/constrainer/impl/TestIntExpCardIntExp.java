package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpCardIntExp;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestIntExpCardIntExp extends TestCase {

    private Constrainer C = new Constrainer("TestIntExpCardIntExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpCardIntExp.class));
    }

    public TestIntExpCardIntExp(String name) {
        super(name);
    }

    public void testMinMax() {
        int[] maxVals = { -2, -1, 0, 1, 2, 3, 4, 5, 6, 7 };
        int maxMax = 7;
        int minMax = -2;
        // initialization of IntExpArray
        IntExpArray array = new IntExpArray(C, maxVals.length);
        for (int i = 0; i < maxVals.length; i++) {
            array.set(C.addIntVar(-10, maxVals[i]), i);
        }

        IntVar expCard = C.addIntVar(minMax, maxMax);
        IntExpCardIntExp intexpcard = null;
        try {
            intexpcard = new IntExpCardIntExp(array, expCard);
        } catch (Failure f) {
            fail("test failed");
        }

        for (int i = 0; i < maxVals.length; i++) {
            try {
                expCard.setMin(minMax + i);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int)");
            }
            System.out.println(expCard.min() + "..." + intexpcard.max());
        }
    }
}
package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;

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
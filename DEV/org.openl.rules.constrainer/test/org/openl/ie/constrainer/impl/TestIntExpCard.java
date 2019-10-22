package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestIntExpCard extends TestCase {

    private Constrainer C = new Constrainer("TestIntExpCard");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpCard.class));
    }

    public TestIntExpCard(String name) {
        super(name);
    }

    public void testAttachDetachObserver() {
        try {
            IntExpCard expcard = new IntExpCard(C, new IntExpArray(C, 10, 0, 5, "array"), 4);
            class TestObserver extends Observer {
                private int counter = 0;

                @Override
                public Object master() {
                    return null;
                }

                @Override
                public int subscriberMask() {
                    return MIN | MAX | VALUE;
                }

                @Override
                public void update(Subject exp, EventOfInterest event) throws Failure {
                    counter++;
                }

                public int updtCounter() {
                    return counter;
                }
            } // end of TestObserver
            TestObserver observer1 = new TestObserver(), observer2 = new TestObserver();
            expcard.attachObserver(observer1);
            expcard.attachObserver(observer2);
            assertEquals(0, observer1.updtCounter());
            assertEquals(0, observer2.updtCounter());
            try {
                expcard.addValueIndex(3);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMax(int)");
            }
            assertEquals(1, observer1.updtCounter());
            assertEquals(1, observer1.updtCounter());

            expcard.detachObserver(observer2);
            try {
                expcard.addValueIndex(3);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMax(int)");
            }

            assertEquals(2, observer1.updtCounter());
            // observer2.update() hasn't to be invoked!
            assertEquals(1, observer2.updtCounter());
        } catch (Failure f) {
            fail("test failed.");
        }
    } // end of testAttachDetachObserver

    public void testBindAll() {
        final int cardValue = 5;
        IntExpArray array = new IntExpArray(C, 5, 0, 5, "array");
        IntExpCard card = null;
        try {
            card = new IntExpCard(C, array, cardValue);
        } catch (Failure f) {
            fail("test failed");
        }
        try {
            card.bindAll();
        } catch (Failure f) {
            fail("test of IntExpCard.bindAll() failed.");
        }

        for (int i = 0; i < array.size(); i++) {
            try {
                assertEquals(cardValue, array.get(i).value());
            } catch (Failure f) {
                fail("test failed due to incorrect behaviour of IntVar.value()");
            }
        }
    }

    public void testIntExpCard() {
        try {
            IntExpArray array = new IntExpArray(C, 5, 0, 5, "array");

            IntExpCard card = new IntExpCard(C, array, array.max() + 1);
            // there couldn't be elements with value greater then array.max()
            assertEquals(0, card.value());

            IntExpCard card1 = new IntExpCard(C, array, array.min() - 1);
            assertEquals(0, card1.value());

            IntExpCard card2 = new IntExpCard(C, array, 5);
            int max_possible = array.size();
            int min_possible = 0;
            assertEquals(max_possible, card2.max());
            assertEquals(min_possible, card2.min());

            array.get(1).setMax(3);
            C.propagate();
            IntExpCard card3 = new IntExpCard(C, array, 5);
            max_possible = max_possible - 1; // has to decrease by one
            // min_possible = min_possible; //don't has to change
            assertEquals(max_possible, card3.max());
            assertEquals(min_possible, card3.min());

            array.get(2).setValue(5);
            C.propagate();
            IntExpCard card4 = new IntExpCard(C, array, 5);
            // max_possible = max_possible; //don't has to change
            min_possible = min_possible + 1; // has to increase by one
            assertEquals(max_possible, card4.max());
            assertEquals(min_possible, card4.min());

            array.get(3).setMin(3);
            C.propagate();
            IntExpCard card5 = new IntExpCard(C, array, 5);
            // nothing has had to be changed!!!
            assertEquals(max_possible, card5.max());
            assertEquals(min_possible, card5.min());

            array.get(0).removeValue(5);
            C.propagate();
            IntExpCard card6 = new IntExpCard(C, array, 5);
            max_possible = max_possible - 1; // has to decrease by one
            // min_possible = min_possible; //don't has to change
            assertEquals(max_possible, card6.max());
            assertEquals(min_possible, card6.min());

        } catch (Failure f) {
            fail("test failed due to some reason.");
        }
    }

    public void testRemoveIndex() {
        final int cardValue = 5;
        final int[] indexToBeRemoved = { 0, 1 };
        IntExpArray array = new IntExpArray(C, 5, 0, 5, "array");
        IntExpCard card = null;
        try {
            card = new IntExpCard(C, array, cardValue);
        } catch (Failure f) {
            fail("test failed");
        }

        int oldmax = card.max();
        // remove index
        try {
            card.removeIndex(indexToBeRemoved[0]);
            card.removeIndex(indexToBeRemoved[1]);
            C.propagate();
        } catch (Failure f) {
            fail("test failed: removeIndex(int idx) works incorrectly");
        }
        assertEquals(oldmax - 2, card.max());

        try {
            card.bindAll();
        } catch (Failure f) {
            fail("test of IntExpCard.bindAll() failed.");
        }

        for (int i = 0; i < array.size(); i++) {
            if ((i != indexToBeRemoved[0]) && (i != indexToBeRemoved[1])) {
                try {
                    assertEquals(cardValue, array.get(i).value());
                } catch (Failure f) {
                    fail("test failed due to incorrect behaviour of IntVar.value()");
                }
            } else {
                assertTrue(!array.get(i).bound());
            }
        }
    }

    public void testRemoveUnbounds() {
        final int cardValue = 5;
        final int[] indexToBeRemoved = { 0, 1 };
        IntExpArray array = new IntExpArray(C, 5, 0, 5, "array");
        IntExpCard card = null;
        try {
            card = new IntExpCard(C, array, cardValue);
        } catch (Failure f) {
            fail("IntExpCard(Constrainer, IntExpArray, card_value) failed");
        }

        try {
            array.get(indexToBeRemoved[0]).setValue(5);
            array.get(indexToBeRemoved[1]).setValue(5);
            array.get(2).setValue(3);
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setValue()");
        }

        try {
            card.removeUnbounds();
        } catch (Failure f) {
            fail("test failed");
        }

        // all variables except those having been bounded does not have to
        // contain cardValue
        // in their domains
        for (int i = 0; i < array.size(); i++) {
            if ((i != indexToBeRemoved[0]) && (i != indexToBeRemoved[1])) {
                assertTrue(!array.get(i).contains(cardValue));
            } else {
                assertTrue(array.get(i).contains(cardValue));
            }
        }

        array = new IntExpArray(C, 5, 0, 5, "array");
        try {
            card = new IntExpCard(C, array, cardValue);
        } catch (Failure f) {
            fail("IntExpCard(Constrainer, IntExpArray, card_value) failed");
        }

        try {
            card.removeIndex(indexToBeRemoved[0]);
            card.removeIndex(indexToBeRemoved[1]);
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntExpCard.removeIndex(int idx)");
        }

        try {
            card.removeUnbounds();
        } catch (Failure f) {
            fail("test failed");
        }
        // all variables except those their indices has been removed does not
        // have to contain cardValue
        for (int i = 0; i < array.size(); i++) {
            if ((i != indexToBeRemoved[0]) && (i != indexToBeRemoved[1])) {
                assertTrue(!array.get(i).contains(cardValue));
            } else {
                assertTrue(array.get(i).contains(cardValue));
            }
        }

    }

    public void testSetValue() {
        final int cardValue = 5;
        final int[] indexToBeRemoved = { 0, 1 };
        IntExpArray array = new IntExpArray(C, 5, 0, 5, "array");
        IntExpCard card = null;
        try {
            card = new IntExpCard(C, array, cardValue);
        } catch (Failure f) {
            fail("IntExpCard(Constrainer, IntExpArray, card_value) failed");
        }
    }
}
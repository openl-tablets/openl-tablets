package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.ConstraintExpLessValue;
import org.openl.ie.constrainer.impl.ConstraintExpMoreValue;
import org.openl.ie.constrainer.impl.ConstraintExpNotValue;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestConstraintExpLessValue extends TestCase {
    private static final int value = 5;
    private Constrainer C = new Constrainer("TestConstraintExpLessValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestConstraintExpLessValue.class));
    }

    public TestConstraintExpLessValue(String name) {
        super(name);
    }

    public void testExecute() {
        IntVar intvar = C.addIntVar(0, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        try {
            C.postConstraint(new ConstraintExpLessValue(intvar, value));
            assertTrue("the maximal value of the variable is greater then the constant value it has to be lesser then",
                    intvar.max() < value);
            try {
                C.postConstraint(new ConstraintExpMoreValue(intvar, value - 1));
                fail("ConstraintExpMoreValue doesn't work properly");
            } catch (Failure f) {
            }

            C.postConstraint(new ConstraintExpMoreValue(intvar, value - 2));
            assertEquals(value - 1, intvar.value());

            try {
                C.postConstraint(new ConstraintExpNotValue(intvar, value - 1));
                fail("ConstraintExpNotValue doesn't work properly");
            } catch (Failure f) {
            }

        } catch (Failure f) {
            fail("test failed!!!");
        }

    }
}
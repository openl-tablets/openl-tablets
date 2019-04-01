package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestConstraintExpLessExp extends TestCase {
    private Constrainer C = new Constrainer("TestConstraintExpLessExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestConstraintExpLessExp.class));
    }

    public TestConstraintExpLessExp(String name) {
        super(name);
    }

    public void testExecute() {
        IntVar var = C.addIntVar(0, 10, "var1", IntVar.DOMAIN_BIT_FAST), var2 = C.addIntVar(-2, -1, "var2"),
                var3 = C.addIntVar(0, 10, "var3", IntVar.DOMAIN_BIT_FAST);
        try {
            C.postConstraint(new ConstraintExpLessExp(var, var2, 0));
            fail("the second variable is always less then the first one");
        } catch (Failure f) {
        }

        try {
            C.postConstraint(new ConstraintExpLessExp(var, var3, 0));
            // decrease maximum of the greater variable
            var3.setMax(7);
            var3.propagate();
            assertTrue("The greater variable \"setMax(int)\" wasn't traced", var.max() <= var3.max());
            // increase minimum of the lesser variable
            var.setMin(3);
            var.propagate();
            assertTrue("The greater variable \"setMin(int)\" wasn't traced", var.min() <= var3.min());
        } catch (Failure f) {
            fail("test failed due to incorrect work of setMin(int) or setMax(int)");
        }

        for (int i = var3.min() + 1; i <= var3.max(); i++) {
            try {
                var3.removeValue(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.removeValue(int)");
            }
        }
        try {
            var.setMin(var3.min() + 1);
            var.propagate();
            fail("minimal value of the lesser variable is greater than that of the bigger one");
        } catch (Failure f) {
        }
    }
}
package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestConstraintExpEqualsValue extends TestCase {
    Constrainer C = new Constrainer("TestConstraintExpEqualsValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestConstraintExpEqualsValue.class));
    }

    public TestConstraintExpEqualsValue(String name) {
        super(name);
    }

    public void testExecute() {
        IntVar var = C.addIntVar(0, 10, "intvar");
        try {
            C.postConstraint(new ConstraintExpEqualsValue(var, var.min() - 1));
            fail("test failed: the value is out of the variable's domain");
        } catch (Failure f) {
        }
        try {
            C.postConstraint(new ConstraintExpEqualsValue(var, var.max() + 1));
            fail("test failed: the value is out of the variable's domain");
        } catch (Failure f) {
        }
        try {
            var.removeValue(3);
            var.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue()");
        }
        try {
            C.postConstraint(new ConstraintExpEqualsValue(var, 3));
            fail("test failed: the value is missing in the variable's domain");
        } catch (Failure f) {
        }
        try {
            C.postConstraint(new ConstraintExpEqualsValue(var, 7));
            assertEquals("variable has incorrect value", 7, var.value());
        } catch (Failure f) {
            fail("test failed");
        }
    }

}
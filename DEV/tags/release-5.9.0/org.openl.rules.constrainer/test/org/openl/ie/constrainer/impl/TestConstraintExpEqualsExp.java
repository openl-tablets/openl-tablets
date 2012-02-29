package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.ConstraintExpEqualsExp;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestConstraintExpEqualsExp extends TestCase {
    Constrainer C = new Constrainer("TestConstraintExpEqualsExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestConstraintExpEqualsExp.class));
    }

    public TestConstraintExpEqualsExp(String name) {
        super(name);
    }

    public void testExecute() {
        IntVar var1 = C.addIntVar(0, 10, "var1", IntVar.DOMAIN_BIT_FAST), var2 = C.addIntVar(-1, 11, "var2",
                IntVar.DOMAIN_BIT_FAST);
        IntExpArray array1 = new IntExpArray(C, 10, 0, 9, "array1"), array2 = new IntExpArray(C, 10, 0, 9, "array1");
        try {
            C.postConstraint(new ConstraintExpEqualsExp(var1, var2));
            assertEquals(var1.max(), var2.max());
            assertEquals(var1.min(), var2.min());
            var1.setMax(9);
            var1.propagate();
            assertEquals("setMax wasn't traced", var1.max(), var2.max());
            assertEquals("setMax wasn't traced", var1.min(), var2.min());
            var1.setMin(2);
            var1.propagate();
            assertEquals("setMin wasn't traced", var1.max(), var2.max());
            assertEquals("setMin wasn't traced", var1.min(), var2.min());
            var1.removeValue(9);
            var1.propagate();
            assertEquals("removeValue (max) wasn't traced", var1.max(), var2.max());
            assertEquals("removeValue (max) wasn't traced", var1.min(), var2.min());
            var1.removeValue(2);
            var1.propagate();
            assertEquals("removeValue (min) wasn't traced", var1.max(), var2.max());
            assertEquals("removeValue (min) wasn't traced", var1.min(), var2.min());
            var2.setValue(7);
            var2.propagate();
            assertEquals("setValue (max) wasn't traced", var1.value(), var2.value());

            for (int i = 0; i < array1.size(); i++) {
                C.postConstraint(new ConstraintExpEqualsExp(array1.get(i), array2.get(i)));
            }
            C.allDiff(array1);
            boolean flag = C.execute(new GoalGenerate(array1));
            assertTrue("ConstraintAllDiff works incorrect", flag);
            for (int i = 0; i < array1.size(); i++) {
                assertEquals("test failed", array1.get(i).value(), array2.get(i).value());
            }

            var1 = C.addIntVar(0, 10, "var11", IntVar.DOMAIN_BIT_FAST);
            var2 = C.addIntVar(0, 10, "var12", IntVar.DOMAIN_BIT_FAST);
            try {
                var1.removeValue(5);
            } catch (Failure f) {
                fail("IntVar.removeValue(int) doesn't work properly");
            }
            var1.propagate();
            try {
                C.postConstraint(new ConstraintExpEqualsExp(var1, var2));
            } catch (Failure f) {
                fail("test failed");
            }
            try {
                var2.setValue(5);
                var2.propagate();
                fail("test failed: allows to set the value to the variable that missing in"
                        + "the domain of the variable this one should be equal to");
            } catch (Failure f) {
            }

        } catch (Failure e) {
            fail("test failed!");
        }

    }

}
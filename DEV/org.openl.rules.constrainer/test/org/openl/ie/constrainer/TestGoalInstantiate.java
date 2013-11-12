package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelectorMax;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestGoalInstantiate extends TestCase {
    Constrainer C = new Constrainer("test GoalInstantiate");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestGoalInstantiate.class));
    }

    public TestGoalInstantiate(String name) {
        super(name);
    }

    public void testExecute() {
        try {
            IntExpArray array = new IntExpArray(C, 10, 0, 9, "ciphers");
            C.postConstraint(C.allDiff(array));
            for (int i = 0; i < array.size(); i++) {
                Goal g = new GoalInstantiate((IntVar) array.get(i), new IntValueSelectorMin(), true);
                C.execute(g);
                assertEquals(array.name() + "[" + i + "] != " + i + "!!!!!", array.get(i).value(), i);
            }
            IntExpArray array1 = new IntExpArray(C, 10, 0, 9, "ciphers");
            C.postConstraint(C.allDiff(array1));
            for (int i = 0; i < array1.size(); i++) {
                Goal g = new GoalInstantiate((IntVar) array1.get(i), new IntValueSelectorMax(), true);
                C.execute(g);
                // System.out.println(array1.get(i));
                assertEquals(array.name() + "[" + i + "] != " + (9 - i) + "!!!!!", array1.get(i).value(), (9 - i));
            }
        } catch (Failure f) {
            f.printStackTrace();
        }
        try {
            IntVar var = C.addIntVar(0, 0, IntVar.DOMAIN_PLAIN);
            assertTrue(C.execute(new GoalInstantiate(var)));
            assertEquals(var.value(), 0);
        } catch (Failure f) {
            fail("GoalInstantiate test failed: " + f);
        }

    }

}
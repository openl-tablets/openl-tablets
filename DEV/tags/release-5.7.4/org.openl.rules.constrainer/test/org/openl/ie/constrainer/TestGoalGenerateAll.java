package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerateAll;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestGoalGenerateAll extends TestCase {
    private Constrainer C = new Constrainer("test GoalGenerateAll");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestGoalGenerateAll.class));
    }

    public TestGoalGenerateAll(String name) {
        super(name);
    }

    public void testExecute() {
        try {
            IntVar x = C.addIntVar(-1000, 1000, "x", IntVar.DOMAIN_PLAIN);
            IntVar y = C.addIntVar(-1000, 1000, "y", IntVar.DOMAIN_PLAIN);
            IntExpArray yx = new IntExpArray(C, x, y);

            C.postConstraint(y.ge(x.abs().sub(15)));
            C.postConstraint(y.le(x.abs().neg().add(15)));
            C.postConstraint(x.sqr().sub(225).ge(y.sqr()).or(y.sqr().sub(225).ge(x.sqr())));
            System.out.println("The problem situation runs as follows:\n"
                    + "1)y>|x|-15,\n2)y<-|x|+15\n3)x^2-255>=y^2 or y^2-255>=x^2\n"
                    + "The problem has four solutions: {(0,15), (0,-15), (15,0), (-15,0)\n");
            System.out.print("The solution obtained using GoalGenerateAll:");
            Goal gen = new GoalGenerateAll(yx);
            C.execute(gen);
        } catch (Failure f) {
            f.printStackTrace();
        }
    }
}
package com.exigen.ie.constrainer;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author unascribed
 * @version 1.0
 */

public class TestGoalFloatMinimize extends TestCase {
  private Constrainer C = new Constrainer("TestGoalFloatMinimize");
  public TestGoalFloatMinimize(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestGoalFloatMinimize.class));
  }

  public void testExecute(){
    FloatExpArray array = new FloatExpArray(C, 2);
    FloatVar x = C.addFloatVar(-100, 100, "x"),
             y = C.addFloatVar(0, 100, "y");

    array.set(x, 0);
    array.set(y, 1);
    // find minimum of -x-y
    // subjected to:
    // 1)y - x^4 > 0,
    // 2)y - exp(x) < 0;
    FloatExp cost = x.neg().sub(y);
    try{
      C.postConstraint(y.ge(x.pow(4.0)));
      C.postConstraint(y.le(x.exp()));
    }catch(Failure f){fail("Constrainer.postConstraint(IntBoolExpFloatLessExp) failed");}

    boolean flag = C.execute(new GoalFloatMinimize(new GoalFloatGenerate(array), cost));
    assertTrue(flag);
    System.out.println(x);
    System.out.println(y);
    assertEquals(1.4296, x.min(), 1e-3);
    assertEquals(Math.exp(1.4296), y.min(), 1e-3);
  }

}
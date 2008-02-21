package com.exigen.ie.constrainer;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestGoalFail extends TestCase {

  public TestGoalFail(String name) {super(name);}

  public static void main(String[] args) {
    junit.textui.TestRunner.run(new TestSuite(TestGoalFail.class));
  }

  public void testExecute(){
    Constrainer C = new Constrainer("test GoalFail");
    Goal gfail = new GoalFail(C);
    try{
      gfail.execute();
      fail("Goal fail doesn't work properly!!!");
    }
    catch(Failure f){}
    catch(Throwable ex){
      fail ("Unexpected exception:" + ex);
    }
  }
}
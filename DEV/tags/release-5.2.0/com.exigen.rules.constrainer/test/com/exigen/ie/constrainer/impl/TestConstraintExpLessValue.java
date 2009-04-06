package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntVar;


public class TestConstraintExpLessValue extends TestCase {
  private Constrainer C = new Constrainer("TestConstraintExpLessValue");
  private static final int value = 5;
  public TestConstraintExpLessValue(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestConstraintExpLessValue.class));
  }

  public void testExecute(){
    IntVar intvar = C.addIntVar(0, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
    try{
      C.postConstraint(new ConstraintExpLessValue(intvar,value));
      assertTrue("the maximal value of the variable is greater then the constant value it has to be lesser then",
                 intvar.max() < value);
      try{
        C.postConstraint(new ConstraintExpMoreValue(intvar,value-1));
        fail("ConstraintExpMoreValue doesn't work properly");
      }catch(Failure f){}

      C.postConstraint(new ConstraintExpMoreValue(intvar,value-2));
      assertEquals(value-1, intvar.value());

      try{
        C.postConstraint(new ConstraintExpNotValue(intvar, value-1));
        fail("ConstraintExpNotValue doesn't work properly");
      }catch(Failure f){}

    }
    catch(Failure f){fail("test failed!!!");}


  }
}
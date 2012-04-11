package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntVar;

public class TestConstraintExpEqualsValue extends TestCase {
  Constrainer C = new Constrainer("TestConstraintExpEqualsValue");
  public TestConstraintExpEqualsValue(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestConstraintExpEqualsValue.class));
  }

  public void testExecute(){
    IntVar var = C.addIntVar(0, 10, "intvar");
    try{
      C.postConstraint(new ConstraintExpEqualsValue(var, var.min()-1));
      fail("test failed: the value is out of the variable's domain");
    }
    catch(Failure f){}
    try{
      C.postConstraint(new ConstraintExpEqualsValue(var, var.max()+1));
      fail("test failed: the value is out of the variable's domain");
    }
    catch(Failure f){}
    try{
      var.removeValue(3);
      var.propagate();
    }
    catch(Failure f){fail("test failed due to incorrect work of IntVar.removeValue()");}
    try{
      C.postConstraint(new ConstraintExpEqualsValue(var, 3));
      fail("test failed: the value is missing in the variable's domain");
    }
    catch(Failure f){}
    try{
      C.postConstraint(new ConstraintExpEqualsValue(var, 7));
      assertEquals("variable has incorrect value", 7, var.value());
    }
    catch(Failure f){fail("test failed");}
  }

}
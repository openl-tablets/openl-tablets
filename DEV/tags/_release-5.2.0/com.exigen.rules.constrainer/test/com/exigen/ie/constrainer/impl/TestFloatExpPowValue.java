package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.FloatExpConst;
import com.exigen.ie.constrainer.FloatVar;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author unascribed
 * @version 1.0
 */

public class TestFloatExpPowValue extends TestCase {
  private Constrainer C = new Constrainer("TestFloatExpPowValue");
  public TestFloatExpPowValue(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestFloatExpPowValue.class));
  }

  public void testConstraintPropagation(){
    FloatVar floatVar = C.addFloatVar(0.125, 4, "");
    FloatExp floatExp = new FloatExpPowValue(floatVar, 0.5);
    try{
      new IntBoolExpFloatLessExp(new FloatExpConst(C,2.0), floatExp);
      C.postConstraint(floatExp.ge(2));
      floatVar.setValue(3);
      C.propagate();
      fail("test failed");
    }catch(Failure f){}
  }
}
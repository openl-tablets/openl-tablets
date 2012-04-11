package com.exigen.ie.constrainer.impl;

import java.util.HashMap;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.FloatVar;
import com.exigen.ie.constrainer.NonLinearExpression;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author unascribed
 * @version 1.0
 */

public class TestFloatExpAddValue extends TestCase {
  private Constrainer C = new Constrainer("TestFloatExpAddValue");
  public TestFloatExpAddValue(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestFloatExpAddValue.class));
  }

  public void testAscendantEventPropagation(){
    FloatVar floatVar1 = C.addFloatVar(0, 10, "");

    FloatExp floatExp = new FloatExpAddValue(floatVar1, 5);

    try{
      floatVar1.setMax(5);
      C.propagate();
      assertEquals(10 ,floatExp.max(), Constrainer.precision());

      floatVar1.setMin(2);
      C.propagate();
      assertEquals(7 ,floatExp.min(), Constrainer.precision());

    }catch(Failure f){fail("test failed");}
  }


  public void testSetMinSetMax(){
    FloatVar floatVar1 = C.addFloatVar(0, 10, "");
    FloatExp addExp = new FloatExpAddValue(floatVar1, 5);

    //descendant event propagation : setMax()
    try{
      assertEquals(15, addExp.max(), Constrainer.precision());
      assertEquals(5, addExp.min(), Constrainer.precision());
      addExp.setMax(9);
      C.propagate();
      assertEquals(4, floatVar1.max(), Constrainer.precision());
    }catch(Failure f){fail("test failed due to incorrect work of FloatVar.setMin(double)");}

    // constraint propagation
    floatVar1 = C.addFloatVar(0, 10, "");
    addExp = new FloatExpAddValue(floatVar1, 5);
    try{
      C.postConstraint(addExp.le(10));
      C.propagate();
      assertEquals(5, floatVar1.max(), Constrainer.precision());
    }catch(Failure f){  fail("test failed: Constraint propagation");}
  }

  public void testCalcCoeffs(){
    FloatVar floatVar1 = C.addFloatVar(0, 10, "");
    FloatExp floatExp = new FloatExpAddValue(floatVar1, 5);
    HashMap map = new HashMap();
    try{
      assertEquals(5, (int)floatExp.calcCoeffs(map));
    }catch(NonLinearExpression e){fail("FloatExpAddValue treated as NonLinearExpression");}
    assertEquals(1, map.size());
    assertEquals(1, (int)((Double)(map.get(floatVar1))).doubleValue());
  }
}
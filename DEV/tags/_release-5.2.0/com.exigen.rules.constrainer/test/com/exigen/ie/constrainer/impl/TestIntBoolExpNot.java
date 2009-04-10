package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntBoolExpConst;
import com.exigen.ie.constrainer.IntVar;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author Sergej Vanskov
 * @version 1.0
 */

public class TestIntBoolExpNot extends TestCase {
  private Constrainer C = new Constrainer("TestIntBoolExpNot");
  public TestIntBoolExpNot(String name){super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestIntBoolExpNot.class));
  }

  public void testIntBoolExpNot(){
    IntBoolExp exp1 = new IntBoolExpNot(new IntBoolExpConst(C, true));
    assertTrue(!exp1.isTrue());
    exp1 = new IntBoolExpNot(new IntBoolExpConst(C, false));
    assertTrue(exp1.isTrue());

   IntVar intvar1 = C.addIntVar(0,20), intvar2 = C.addIntVar(5,15);
   try{
      C.postConstraint(new IntBoolExpLessExp(intvar1, intvar2).not());
      assertEquals(5, intvar1.min());
      intvar2.setMin(7);
      C.propagate();
      assertEquals(7, intvar1.min());
      intvar1.setMax(9);
      C.propagate();
      assertEquals(9, intvar2.max());
   }catch(Failure f) {fail("test failed");}
  }
}
package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntVar;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class TestIntBoolExpLessValue extends TestCase {
  private Constrainer C = new Constrainer("TestIntBoolExpLessValue");
  public TestIntBoolExpLessValue(String name){super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestIntBoolExpLessValue.class));
  }

  public void testAll(){
    IntVar intvar = C.addIntVar(0,10);
    IntBoolExpLessValue boolexp = new IntBoolExpLessValue(intvar, 8);
    // test execute
    try{
      C.postConstraint(boolexp);
      assertEquals(8-1, intvar.max());
      assertEquals(0, intvar.min());
    }catch(Failure f){fail("test failed");}
    boolexp = new IntBoolExpLessValue(intvar, 0);
    try{
      C.postConstraint(boolexp);
    }catch(Failure f){/*that's ok!*/}
  }

}
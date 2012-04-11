package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author Sergej Vanskov
 * @version 1.0
 */

public class TestIntBoolExpLessExp extends TestCase {
  private Constrainer C = new Constrainer("TestIntBoolExpLessExp");
  public TestIntBoolExpLessExp(String name){super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestIntBoolExpLessExp.class));
  }

  public void testIntBoolExpLessExp(){
    IntExpArray array = new IntExpArray(C, 0, 9, 10, "array");
    for (int i=0;i<array.size()-1;i++)
      try{
         C.postConstraint(new IntBoolExpLessExp(array.get(i), array.get(i+1)));
      }catch(Failure f){fail("test failed");}
    boolean flag = C.execute(new GoalGenerate(array));
    assertTrue(flag);
    for (int i=0;i<array.size();i++){
      try{
        assertEquals(i, array.get(i).value());
      }catch(Failure f){fail("test failed due to incorrect work of IntVar.value()");}
    }
  }

  public void testAll(){
    IntVar intvar1 = C.addIntVar(0,10),
    intvar2 = C.addIntVar(-10, 0) ;

    IntBoolExp boolexp = new IntBoolExpLessExp(intvar1, intvar2);
    try{
      C.postConstraint(boolexp);
      fail("test failed");
    }catch(Failure f){}

    intvar1 = C.addIntVar(0, 10);
    intvar2 = C.addIntVar(-2, 1);
    try{
      C.postConstraint(new IntBoolExpLessExp(intvar1, intvar2));
      boolean flag = C.execute(new GoalGenerate(new IntExpArray(C, intvar1, intvar2)));
      assertTrue(flag);
      assertEquals(0, intvar1.value());
      assertEquals(1, intvar2.value());
    }catch(Failure f){fail("test failed");}

    intvar1 = C.addIntVar(5, 15);
    intvar2 = C.addIntVar(0, 10);
    try{
      C.postConstraint(new IntBoolExpLessExp(intvar1, intvar2));
    }catch(Failure f) {fail("test failed");}
    assertEquals(9, intvar1.max());
    assertEquals(6, intvar2.min());

    intvar1 = C.addIntVar(0,10);
    intvar2 = C.addIntVar(0,10);
    // event propagation test
    try{
      C.postConstraint(new IntBoolExpLessExp(intvar1, intvar2));
      intvar2.setMax(8);
      //intvar1.propagate();
      C.propagate();
      assertEquals(7, intvar1.max());
      intvar1.setMin(7);
      //intvar1.propagate();
      C.propagate();
      assertEquals(8, intvar2.value());
    }catch(Failure f){fail("test failed");}
  }

}
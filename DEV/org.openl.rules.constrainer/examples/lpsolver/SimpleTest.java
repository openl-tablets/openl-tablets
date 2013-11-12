package lpsolver;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002 </p>
 * <p>Company: unascribed</p>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

import org.openl.ie.constrainer.*;
import org.openl.ie.constrainer.impl.*;
import org.openl.ie.constrainer.lpsolver.*;
import org.openl.ie.constrainer.lpsolver.impl.*;
import java.util.*;

public class SimpleTest {
  Double a = new Double(5);
  public SimpleTest() {
  }
  public static void main(String[] args) {
    try{
    Constrainer c = new Constrainer("test");
    FloatExp x1 = new FloatVarImpl(c, 0, 10, "x1");
    FloatExp x2 = new FloatVarImpl(c, 0, 10, "x2");
    FloatExp exp = (x1.add(x1).add(5).add(6).add(x2).mul(2).neg());
    FloatExpArray arr = new FloatExpArray(c,5);
    arr.set(x1,0);
    arr.set(x2,1);
    for (int i=2;i<arr.size();i++){
      arr.set(new FloatVarImpl(c,0,10,"x"+(i+1)),i);
    }
    exp = exp.add(4);
    exp = exp.add(arr.sum());
    exp = exp.div(2);
    FloatExp exp1 = x2.add(5).mul(2).neg().add(x2).add(x1);
    FloatExp exp2 = arr.get(2).add(x1).add(x1).mul(12);
    IntBoolExp bexp = exp1.ge(exp);
    IntBoolExp bexp1 = exp2.eq(exp);

    c.postConstraint(bexp);

    ConstrainerLP soplex = new LPProblemImpl(exp, true, c, "");
    soplex.addConstraint(bexp, false);
    soplex.addConstraint(bexp1, true);

    LPConstraint lpc = soplex.getLPConstraint(0);
    LPConstraint lpc1 = soplex.getLPConstraint(1);
    FloatExpArray farr = new FloatExpArray(c,5);

    for (int i=0;i<5;i++){
      farr.set(arr.get(i),i);
    }
    Map mapa1 = new HashMap();
    double freeTerm = exp.calcCoeffs(mapa1);
    System.out.println();
    System.out.println(mapa1 + " freeTerm="+new Double(freeTerm));
    mapa1 = new HashMap();
    freeTerm = exp1.calcCoeffs(mapa1);
    System.out.println(mapa1 + " freeTerm="+new Double(freeTerm));
    mapa1 = new HashMap();
    freeTerm = exp2.calcCoeffs(mapa1);
    System.out.println(mapa1 + " freeTerm="+new Double(freeTerm));

    System.out.println(lpc);
    System.out.println(lpc1);


  /*  Goal gen = new GoalFloatGenerate(farr);
    boolean flag = c.execute(gen);
    if (flag)
      System.out.println(arr);
    else
      System.out.println("Nothing to be done!");*/
    }
    catch(Exception ex){
      ex.printStackTrace();
    }
  }
}
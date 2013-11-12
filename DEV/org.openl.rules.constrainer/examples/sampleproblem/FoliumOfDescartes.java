package sampleproblem;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:
 * @author
 * @version 1.0
 */
import org.openl.ie.constrainer.*;

public class FoliumOfDescartes
{
  static Constrainer C = new Constrainer("FoliumOfDescartes");

  public FoliumOfDescartes()
  {
  }
  public static void main(String[] argv){
    try{
      FloatExp exp = new FloatExpConst(C,Math.exp(1));
      FloatVar x = C.addFloatVar(-1,1,"x");
      FloatVar y = C.addFloatVar(-1,1,"y");
      FloatExp exp1 = (x.mul(x).div(y)).add((y.mul(y).div(x)));
      exp1.name("exp1");
      C.postConstraint(exp1.eq(2));
      C.postConstraint(y.eq(exp.pow(x.mul(-1))));

      FloatExpArray vec = new FloatExpArray(C,2);
      vec.set(x,0);
      vec.set(y,1);
      Goal gen = new GoalFloatGenerate(vec);
      boolean flag = C.execute(gen);
      if (flag){
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.print(exp1.value());
      }
      else{
        System.out.print("Can't find any solution");
      }
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
}
package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: MyGoalInstantiate<br>
 * Description: The example of goal programming. MyGoalInstantiate is
 * an implementation of GoalInstantiate functionality with the aid
 * of "trivial" goals. <br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 1.0
 */

public class MyGoalInstantiate extends GoalImpl {
  private IntVar           _intvar;
  public MyGoalInstantiate(IntVar intvar)  {
    super (intvar.constrainer());
    _intvar = intvar;
  }
  public Goal execute() throws Failure {
    if (_intvar.bound())
      return null;
    return new GoalOr (
      new GoalSetValue(_intvar, _intvar.min()),
      new GoalAnd (new GoalSetMin(_intvar, _intvar.min() + 1), this)
    );
  }
  public static void main(String[] args) {
    try {
      Constrainer C = new Constrainer("MyGoalInstantiate");
      IntVar i = C.addIntVar(0,10);

      C.postConstraint(i.gt(2));
      C.postConstraint(i.lt(5));

      C.execute(new MyGoalInstantiate(i));

      System.out.println(i);
    } catch (Failure e) {
      System.out.println(e);
    }
  }
}
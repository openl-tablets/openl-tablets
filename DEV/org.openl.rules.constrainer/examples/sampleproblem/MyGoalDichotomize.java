package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: MyGoalDichotomize<br>
 * Description: The example of goal programming. MyGoalDichotomize is
 * an implementation of GoalDichotomize functionality with the aid
 * of "trivial" goals. <br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 1.0
 */

public class MyGoalDichotomize extends GoalImpl {
  private IntVar           _intvar;
  public MyGoalDichotomize(IntVar intvar)  {
    super (intvar.constrainer());
    _intvar = intvar;
  }
  public Goal execute() throws Failure {
    int mid = (_intvar.min() + _intvar.min()) / 2;
    if (_intvar.bound())
      return null;
    return new GoalOr (
      new GoalAnd (new GoalSetMax(_intvar, mid), this),
      new GoalAnd (new GoalSetMin(_intvar, mid + 1), this)
    );
  }
  public static void main(String[] args) {
    try {
      Constrainer C = new Constrainer("MyGoalDichotomize");
      IntVar i = C.addIntVar(0,10);

      C.postConstraint(i.gt(2));
      C.postConstraint(i.lt(5));

      C.execute(new MyGoalDichotomize(i));

      System.out.println(i);
    } catch (Failure e) {
      System.out.println(e);
    }
  }
}
package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: UndoableAction<br>
 * Description:<br>
 * The sample demonstrates the usage of the {@link Constrainer#addUndoableAction}
 * - undoable action. This demo animates the dichotomized search and
 * shoud be run in a console window that supports "\b" as 'backspace'
 * to demonstrate correctly erasing in undoable actions.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class UndoableAction
{
  static void delay(int msec)
  {
    try{Thread.sleep(msec);}catch(Exception e){}
  }

  /**
   * Eraser for undoable action.
   */
  static class GoalErase extends GoalImpl
  {
    int _n;
    public GoalErase(Constrainer c, int n)
    {
      super(c);
      _n=n;
    }

    public Goal execute() throws Failure
    {
      for (int i=0; i<_n; i++)
      {
        System.out.print("\b \b");
      }
      delay(2000);
      return null;
    }
  }

  /**
   * Animated dichotomized search.
   */
  static class GoalSearch extends GoalImpl
  {
    IntVar _x;

    public GoalSearch(IntVar x)
    {
      super(x.constrainer());
      _x = x;
    }

    public Goal execute()
    {
      String s = "->" + _x;
      System.out.print(s);
      delay(2000);

      constrainer().addUndoableAction(new GoalErase(constrainer(),s.length()));

      if(_x.bound()) return new GoalFail(constrainer());

      int min = _x.min();
      int max = _x.max();
      int mid = (min+max)/2;
      if(mid == max) mid = max-1;

      return new GoalAnd(new GoalOr(_x.lessOrEqual(mid),_x.more(mid)), this);
    }
  }

  public static void main(String[] args) throws Exception
  {
    // Create the constrainer.
    Constrainer c = new Constrainer("UndoableAction");

    IntVar x = c.addIntVar(-2,2,"x");

    delay(2000);
    c.execute(new GoalSearch(x));
  }

} // ~UndoableAction

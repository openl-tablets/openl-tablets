package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: UndoableIntSample<br>
 * Description:<br>
 * The sample demonstrates usage of the {@link UndoableInt} - undoable
 * integer value.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class UndoableIntSample
{
  public static class GoalIncUI extends GoalImpl
  {
    UndoableInt ui;
    int i;

    /**
     * Constructor with a given values.
     */
    public GoalIncUI(Constrainer c, UndoableInt ui, int i)
    {
      super(c,"GoalIncUI");
      this.ui = ui;
      this.i = i;
    }

    /**
     * Increments the values and pring them.
     */
    public Goal execute()
    {
      ui.setValue( ui.value() + 1 );
      i = i + 1;
      System.out.println(this);
      return null;
    }

    /**
     * Returns the String representation for this goal.
     */
    public String toString()
    {
      return ui + ", " + "i["+i+"]";
    }

  }

  public static void main(String[] args) throws Exception
  {
    // Create the constrainer.
    Constrainer c = new Constrainer("UndoableIntSample");

    // Create the goal for undoable int.
    UndoableInt ui = c.addUndoableInt(0,"ui"); ui.name("ui");
    GoalIncUI g = new GoalIncUI(c,ui,0);

    // Print the goal: initial state.
    System.out.println(g + " initial");

    // Execute and print the goal: don't restore the value.
    c.execute(g,false);
    System.out.println(g + " not restored");

    // Execute and print the goal: restore the value.
    c.execute(g,true);
    System.out.println(g + " restored");
  }

}
/*
Program output:
ui[0], i[0] initial
ui[1], i[1]
ui[1], i[1] not restored
ui[2], i[2]
ui[1], i[2] restored
*/

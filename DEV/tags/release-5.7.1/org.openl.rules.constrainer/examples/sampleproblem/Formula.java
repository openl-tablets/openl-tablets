package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: Formula<br>
 * Problem Description:<br>
 * Solve the system:<br>
 * <samp>
 * &nbsp;&nbsp;X**2 + Y**2 < 20<br>
 * &nbsp;&nbsp;X + Y = 5<br>
 * </samp>
 * where X and Y are positive integers.<br>
 * <br>
 * The program output:<br>
 * <samp>
 * Solution 1: X[1], Y[4]<br>
 * Solution 2: X[2], Y[3]<br>
 * Solution 3: X[3], Y[2]<br>
 * Solution 4: X[4], Y[1]<br>
 * </samp>
 * <br>
 * Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 1.0
*/

public class Formula
{
  public static void main(String[] args) {
    try {
      Constrainer c = new Constrainer("Formula");

      // define variables
      IntVar X = c.addIntVar(0, 100, "X");
      IntVar Y = c.addIntVar(0, 100, "Y");

      // add "formula" constraints
//      c.addConstraint("X**2 + Y**2 < 20"); // in symbolic forms
      c.addConstraint(X.mul(X).add(Y.mul(Y)).lt(20)); // in direct form

//      c.addConstraint("X + Y == 5"); // in symbolic forms
      c.addConstraint(X.add(Y).eq(5)); // in direct form

      // post all the constraints
      c.postConstraints();

      // Search for all the solutions
      IntExpArray vars = new IntExpArray(c, X, Y);
      Goal goal = new GoalGenerateAll(vars);
      c.execute(goal);
    } catch(Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }
}

package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: PellEquation<br>
 * Description: The general Pell's equation has the form:<br>
 * <samp>&nbsp;&nbsp;x**2 - N*y**2 = D<br></samp>
 * In the example we solve the equation:<br>
 * <samp>&nbsp;&nbsp;x**2 - 2*y**2 = 2<br></samp>
 * <br>
 * The solutions of the problem are<br>
 * <UL>
 *  <LI>Solution 1: x[2], y[1]</LI>
 *  <LI>Solution 2: x[10], y[7]</LI>
 *  </UL>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class PellEquation
 {
  public static void main(String[] args)
  {
    try
    {
      Constrainer c = new Constrainer("PellEquation");

      // define variables
      IntVar x = c.addIntVar(0, 10, "x");
      IntVar y = c.addIntVar(0, 10, "y");

      // add constraints
//      c.addConstraint("x**2 - 2*y**2 == 2");
      c.addConstraint(x.mul(x).sub(y.mul(y).mul(2)).eq(2));

      // post all constraints
      c.postConstraints();

      // Search for all solutions
      IntExpArray vars = new IntExpArray(c, x, y);
      Goal goal = new GoalGenerateAll(vars);

      c.execute(goal);
    }
    catch(Exception e)
    {
      System.out.println(e);
      e.printStackTrace();
    }
  }

}

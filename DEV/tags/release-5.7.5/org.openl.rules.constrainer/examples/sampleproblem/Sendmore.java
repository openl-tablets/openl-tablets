package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: Sendmore<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * Solve the cryptarithm:<br>
 * <br><samp>
 * &nbsp;&nbsp;&nbsp; S E N D<br>
 * &nbsp;           + M O R E<br>
 * &nbsp;           ---------<br>
 * &nbsp;           M O N E Y<br>
 * </samp><br>
 * where each letter represents a different digit.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class Sendmore
{
  public static void main(String[] args)
  {
    try
    {
      Constrainer c = new Constrainer("Sendmory");

      // define variables
      IntVar S = c.addIntVar(1, 9, "S");
      IntVar E = c.addIntVar(0, 9, "E");
      IntVar N = c.addIntVar(0, 9, "N");
      IntVar D = c.addIntVar(0, 9, "D");
      IntVar M = c.addIntVar(1, 9, "M");
      IntVar O = c.addIntVar(0, 9, "O");
      IntVar R = c.addIntVar(0, 9, "R");
      IntVar Y = c.addIntVar(0, 9, "Y");

      IntExpArray vars = new IntExpArray(c, S, E, N, D, M, O, R, Y);

      // post "all different" constraint
      Constraint constraintAllDiff = c.allDiff(vars);
      constraintAllDiff.execute();

      // define expression SEND
      int coef1[] = { 1000, 100, 10, 1 };
      IntExpArray send_vars = new IntExpArray(c, S, E, N, D);
      IntExp SEND = c.scalarProduct(send_vars,coef1);

      // define expression MORE
      IntExpArray more_vars = new IntExpArray(c, M, O, R, E);
      IntExp MORE = c.scalarProduct(more_vars, coef1);

      // define expression MONEY
      IntExpArray money_vars = new IntExpArray(c, M, O, N, E, Y);
      int coef2[] = { 10000, 1000, 100, 10, 1 };
      IntExp MONEY = c.scalarProduct(money_vars, coef2);

      // post constraint send + more == money;
      MONEY.equals(SEND.add(MORE)).execute();

      // Goals
      IntValueSelector value_selector = new IntValueSelectorMin();
      IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
      Goal search_goal = new GoalGenerate(vars,var_selector,value_selector);
      Goal print_goal = new GoalPrint(vars);
      Goal goal = new GoalAnd(search_goal,print_goal);

      c.printInformation();
      c.execute(goal);
    }
    catch(Exception e)
    {
      System.out.println(e);
      e.printStackTrace();
    }
  }
}

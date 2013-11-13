package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: SolverExampleEq10Java<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * An implementation of the linear system involving 7 variables
 * and 10 equations.
 * <samp>
 * <UL>
 *   <LI>
 *     98527*X1 + 34588*X2 + 5872*X3 + 59422*X5 + 65159*X7
 *     == 1547604 + 30704*X4 + 29649*X6
 *   </LI>
 *   <LI>
 *     98957*X2 + 83634*X3 + 69966*X4 + 62038*X5 + 37164*X6 + 85413*X7
 *     == 1823553 + 93989*X1
 *   </LI>
 *   <LI>
 *     900032 + 10949*X1 + 77761*X2 + 67052*X5
 *     == 80197*X3 + 61944*X4 + 92964*X6 + 44550*X7
 *   </LI>
 *   <LI>
 *     73947*X1 + 84391*X3 + 81310*X5
 *     == 1164380 + 96253*X2 + 44247*X4 + 70582*X6 + 33054*X7
 *   </LI>
 *   <LI>
 *     13057*X3 + 42253*X4 + 77527*X5 + 96552*X7
 *     == 1185471 + 60152*X1 + 21103*X2 + 97932*X6
 *   </LI>
 *   <LI>
 *     1394152 + 66920*X1 + 55679*X4
 *     == 64234*X2 + 65337*X3 + 45581*X5 + 67707*X6 + 98038*X7
 *   </LI>
 *   <LI>
 *     68550*X1 + 27886*X2 + 31716*X3 + 73597*X4 + 38835*X7
 *     == 279091 + 88963*X5 + 76391*X6
 *   </LI>
 *   <LI>
 *     76132*X2 + 71860*X3 + 22770*X4 + 68211*X5 + 78587*X6
 *     == 480923 + 48224*X1 + 82817*X7
 *   </LI>
 *   <LI>
 *     519878 + 94198*X2 + 87234*X3 + 37498*X4
 *     == 71583*X1 + 25728*X5 + 25495*X6 + 70023*X7
 *   </LI>
 *   <LI>
 *     361921 + 78693*X1 + 38592*X5 + 38478*X6
 *     == 94129*X2 + 43188*X3 + 82528*X4 + 69025*X7
 *   </LI>
 * </UL>
 * </samp>
 * The equations in the source code are expressed using Java notation. <br>
 * <br>
 * The solution:<br>
 * <samp>
 * [X1,X2,X3,X4,X5,X6,X7]<br>
 * [ 6, 0, 8, 4, 9, 3, 9]<br>
 * </samp>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class SolverExampleEq10Java
{
  public static void main(String[] args)
  {
    try
    {
      Constrainer c = new Constrainer("");

      // define variables
      IntVar X1 = c.addIntVar(0, 10, "X1");
      IntVar X2 = c.addIntVar(0, 10, "X2");
      IntVar X3 = c.addIntVar(0, 10, "X3");
      IntVar X4 = c.addIntVar(0, 10, "X4");
      IntVar X5 = c.addIntVar(0, 10, "X5");
      IntVar X6 = c.addIntVar(0, 10, "X6");
      IntVar X7 = c.addIntVar(0, 10, "X7");

      // add constraints
      Constraint ct;

      ct = X1.mul(98527).add(X2.mul(34588)).add(X3.mul(5872)).add(X5.mul(59422)).add(X7.mul(65159))
          .equals(X4.mul(30704).add(X6.mul(29649)).add(1547604));
      c.addConstraint(ct);

      ct = X2.mul(98957).add(X3.mul(83634)).add(X4.mul(69966)).add(X5.mul(62038)).add(X6.mul(37164)).add(X7.mul(85413))
          .equals(X1.mul(93989).add(1823553));
      c.addConstraint(ct);

      ct = X1.mul(10949).add(X2.mul(77761)).add(X5.mul(67052)).add(900032)
          .equals(X3.mul(80197).add(X4.mul(61944)).add(X6.mul(92964)).add(X7.mul(44550)));
      c.addConstraint(ct);

      ct = X1.mul(73947).add(X3.mul(84391)).add(X5.mul(81310))
          .equals(X2.mul(96253).add(X4.mul(44247)).add(X6.mul(70582)).add(X7.mul(33054)).add(1164380));
      c.addConstraint(ct);

      ct = X3.mul(13057).add(X4.mul(42253)).add(X5.mul(77527)).add(X7.mul(96552))
          .equals(X1.mul(60152).add(X2.mul(21103)).add(X6.mul(97932)).add(1185471));
      c.addConstraint(ct);

      ct = X1.mul(66920).add(X4.mul(55679)).add(1394152)
          .equals(X2.mul(64234).add(X3.mul(65337)).add(X5.mul(45581))
                  .add(X6.mul(67707)).add(X7.mul(98038)));
      c.addConstraint(ct);

      ct = X1.mul(68550).add(X2.mul(27886)).add(X3.mul(31716)).add(X4.mul(73597)).add(X7.mul(38835))
          .equals(X5.mul(88963).add(X6.mul(76391)).add(279091));
      c.addConstraint(ct);

      ct = X2.mul(76132).add(X3.mul(71860)).add(X4.mul(22770)).add(X5.mul(68211)).add(X6.mul(78587))
          .equals(X1.mul(48224).add(X7.mul(82817)).add(480923));
      c.addConstraint(ct);

      ct = X2.mul(94198).add(X3.mul(87234)).add(X4.mul(37498)).add(519878)
          .equals(X1.mul(71583).add(X5.mul(25728)).add(X6.mul(25495)).add(X7.mul(70023)));
      c.addConstraint(ct);

      ct = X1.mul(78693).add(X5.mul(38592)).add(X6.mul(38478)).add(361921)
          .equals(X2.mul(94129).add(X3.mul(43188)).add(X4.mul(82528)).add(X7.mul(69025)));
      c.addConstraint(ct);

      // post all constraints
      c.postConstraints();

      // Search for all solutions
      IntExpArray vars = new IntExpArray(c, X1, X2, X3, X4, X5, X6, X7);
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

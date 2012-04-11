package chess;

/**
 * Title:  Virtual Chess Tournament
 * Problem Description:
 *  Kasparov, Karpov and Fisher played 7 games against each other.
 *  Kasparov won the most games.
 *  Karpov lost the least games.
 *  Fisher became a champion.
 *  Find a final score.
 * Copyright:    Copyright (c) 2002
 * Company:    Exigen Group
 * @author Jacob Feldman
 * @version 1.0
 */

import org.openl.ie.constrainer.*;

public class Chess
{
  final static int N = 7; // number of games played played against each other

  public static void main(String[] args)
  {
    try
    {
      Constrainer c = new Constrainer("Chess");

      // Define mutual Victories, Losses and Draws
      IntVar V12 = c.addIntVar(0,N,"Kasparov won against Karpov ");
      IntVar L12 = c.addIntVar(0,N,"Kasparov lost against Karpov");
      IntVar D12 = c.addIntVar(0,N,"Kasparov drew against Karpov");

      IntVar V13 = c.addIntVar(0,N,"Kasparov won against Fisher ");
      IntVar L13 = c.addIntVar(0,N,"Kasparov lost against Fisher");
      IntVar D13 = c.addIntVar(0,N,"Kasparov drew against Fisher");

      IntVar V23 = c.addIntVar(0,N,"Karpov won against Fisher   ");
      IntVar L23 = c.addIntVar(0,N,"Karpov lost against Fisher  ");
      IntVar D23 = c.addIntVar(0,N,"Karpov drew against Fisher  ");

      // Post constraint "Each pair played 7 games"
      c.postConstraint(V12.add(L12).add(D12).eq(N)); // V12+L12+D12==7
      c.postConstraint(V13.add(L13).add(D13).eq(N)); // V13+L13+D13==7
      c.postConstraint(V23.add(L23).add(D23).eq(N)); // V23+L23+D23==7

      // Define personal Victories, Losses and Draws
      IntExp V1 = V12.add(V13); V1.name("Kasparov wins ");
      IntExp L1 = L12.add(L13); L1.name("Kasparov loses");
      IntExp D1 = D12.add(D13); D1.name("Kasparov draws");
      IntExp V2 = L12.add(V23); V2.name("Karpov wins   ");
      IntExp L2 = V12.add(L23); L2.name("Karpov loses  ");
      IntExp D2 = D12.add(D23); D2.name("Karpov draws  ");
      IntExp V3 = L13.add(L23); V3.name("Fisher wins   ");
      IntExp L3 = V13.add(V23); L3.name("Fisher loses  ");
      IntExp D3 = D13.add(D23); D3.name("Fisher draws  ");

      // Kasparov won the most games
      c.postConstraint(V1.gt(V2).and(V1.gt(V3)));
      // Karpov lost the least games
      c.postConstraint(L2.lt(L1).and(L2.lt(L3)));

      // Define personal Points
      IntExp P1 = V1.mul(2).add(D1); // P1 = 2*V1 + D1
      P1.name("Kasparov's points");
      IntExp P2 = V2.mul(2).add(D2); // P2 = 2*V2 + D2
      P2.name("Karpov's points  ");
      IntExp P3 = V3.mul(2).add(D3); // P3 = 2*V3 + D3
      P3.name("Fisher's points  ");

      // post "champion" constraint
      c.postConstraint(P3.gt(P1).and(P3.gt(P2))); // P3>P1 && P3>P2

      // Search for all solutions
      IntExpArray vars = new IntExpArray(c, V12,L12,D12,V13,L13,D13,V23,L23,D23);
      Goal search_goal = new GoalGenerate(vars);
      Goal print_goal_vars = new GoalPrint(vars,"\nMutual results",true);
      IntExpArray results = new IntExpArray(c, V1,L1,D1,V2,L2,D2,V3,L3,D3);
      Goal print_goal_results = new GoalPrint(results,"Victories, Loses, Draws",true);
      IntExpArray points = new IntExpArray(c, P1,P2,P3);
      Goal print_goal_points = new GoalPrint(points,"Points",true);
      Goal goal = new GoalAnd(search_goal,
                              new GoalPrintSolutionNumber(c),
                              print_goal_vars,
                              print_goal_results,
                              print_goal_points);
      c.executeAll(goal);
    }
    catch(Exception e)
    {
      System.out.println(e);
      e.printStackTrace();
    }
  }
}
/*
Solution 1
Mutual results(9):
Kasparov won against Karpov [2]
Kasparov lost against Karpov[1]
Kasparov drew against Karpov[4]
Kasparov won against Fisher [3]
Kasparov lost against Fisher[4]
Kasparov drew against Fisher[0]
Karpov won against Fisher   [0]
Karpov lost against Fisher  [0]
Karpov drew against Fisher  [7]
Victories, Loses, Draws(9):
Kasparov wins [5]
Kasparov loses[5]
Kasparov draws[4]
Karpov wins   [1]
Karpov loses  [2]
Karpov draws  [11]
Fisher wins   [4]
Fisher loses  [3]
Fisher draws  [7]
Points(3):
Kasparov's points[14]
Karpov's points  [13]
Fisher's points  [15]

Solution 2
Mutual results(9):
Kasparov won against Karpov [2]
Kasparov lost against Karpov[2]
Kasparov drew against Karpov[3]
Kasparov won against Fisher [3]
Kasparov lost against Fisher[4]
Kasparov drew against Fisher[0]
Karpov won against Fisher   [0]
Karpov lost against Fisher  [0]
Karpov drew against Fisher  [7]
Victories, Loses, Draws(9):
Kasparov wins [5]
Kasparov loses[6]
Kasparov draws[3]
Karpov wins   [2]
Karpov loses  [2]
Karpov draws  [10]
Fisher wins   [4]
Fisher loses  [3]
Fisher draws  [7]
Points(3):
Kasparov's points[13]
Karpov's points  [14]
Fisher's points  [15]
*/


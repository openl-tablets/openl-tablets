package com.exigen.ie.constrainer.test;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalFastMinimize;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.GoalPrintSolution;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.impl.GoalFastMinimizeOld;

/**
 * Incorrect implementation in the GoalFastMinimizeOld
 * and correct one in the GoalFastMinimize.
 *
 * Minimization problem depends on x only.
 * GoalFastMinimizeOld continues the search instantiating full domains of the other variables.
 * GoalMinimize do instantiation of other variables only one time.
 */
public class FastMinimizeOld
{
  static Constrainer c;
  static IntExpArray vars;
  static IntExp cost;
  static Goal generate;

  static void problem() throws Exception
  {
    c = new Constrainer("");

    int trace = 0;
//    int trace = IntVarImplTrace.TRACE_ALL;
    IntVar x = c.addIntVarTrace(0, 1, "x", IntVar.DOMAIN_BIT_FAST , trace);
    IntVar y = c.addIntVarTrace(0, 4, "y", IntVar.DOMAIN_BIT_FAST, trace);
    IntVar z = c.addIntVarTrace(0, 4, "z", IntVar.DOMAIN_BIT_FAST, trace);

    vars = new IntExpArray(c, x, y, z);
    vars.name("vars");

    cost = (IntExp)x.neg();
//    cost = x;

    generate = new GoalAnd(new GoalGenerate(vars),new GoalPrintSolution(vars));

//    c.traceFailures();
//    c.trace(vars);
    c.printInformation();
  }

  static void fastMinimizeOld() throws Exception
  {
    System.out.print("\n*** FastMinimizeOld");
    problem();
    c.execute(new GoalFastMinimizeOld(generate,cost));
  }

  static void fastMinimize() throws Exception
  {
    System.out.print("\n*** FastMinimize");
    problem();
    c.execute(new GoalFastMinimize(generate,cost));
  }

   public static void main(String[] args)
    {
      try
      {
        fastMinimize();
        fastMinimizeOld();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }

} // ~FastMinimizeOld

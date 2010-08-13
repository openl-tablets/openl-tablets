package lpsolver;

/**
 * <p>Title: <b>Gas production</b></p>
 * <p>Description: (the description was taken from Ilog's OPL Studio)
 * Consider a Belgian company Volsay, which specializes in producing ammoniac
 * gas (N H3) and ammonium  chloride (N H4 Cl). Volsay has at its disposal 50
 * units of nitrogen (N), 180 units of  hydrogen (H), and 40 units of chlorine
 * (Cl). The company  makes a profit of 40 Belgian  francs for each sale of an
 * ammoniac gas unit and 50 Belgian francs for each sale of an ammonium chloride
 * unit. Volsay would like a production  plan  maximizing its  profits given its
 * available stocks.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigengroup</p>
 * @author unascribed
 * @version 1.0
 */

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;
import org.openl.ie.constrainer.lpsolver.GoalSimplexSolve;
import org.openl.ie.constrainer.lpsolver.impl.LPProblemImpl;

public class GasProduction {

  public GasProduction() {
  }
  static public void main(String[] argv){
    try{
      Constrainer C = new Constrainer("GasProdction");
      // variables
      FloatVar gas = C.addFloatVar(0, Double.MAX_VALUE, "gas");
      FloatVar chloride = C.addFloatVar(0, Double.MAX_VALUE, "chloride");
      // cost function
      FloatExp costFunc = gas.mul(40).add(chloride.mul(50));
      // constraints
      IntBoolExp nitrogenLimit = gas.add(chloride).le(50);
      IntBoolExp hydrogenLimit = gas.mul(3).add(chloride.mul(4)).le(180);
      IntBoolExp chlorideLimit = chloride.le(40);
      // LP Problem
      ConstrainerLP lpx = new LPProblemImpl(costFunc, true);
      lpx.addConstraint(nitrogenLimit, false);
      lpx.addConstraint(hydrogenLimit, false);
      lpx.addConstraint(chlorideLimit, false);
      // solution
      Goal solve = new GoalSimplexSolve(C, lpx);
      boolean flag = C.execute(solve);
      if (flag){
        System.out.println("Optiomal cost function value: " + costFunc.value());
        System.out.println("Variables:");
        System.out.println(gas);
        System.out.println(chloride);
      }
      else
        System.out.println("The problem has no solution");
    }
    catch(Failure f){
      System.out.println(f);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  };
}
package lpsolver;

/**
 * <p>Description: (The description was taken from Ilog's OPL Studio) An oil company manufactures three types of gasoline: super, regular, and diesel.
 * Each type of gasoline is produced by blending three types of crude oil: crude1, crude2, and crude3.
 * The gasoline must satisfy some quality criteria with respect to their lead content and their octane ratings,
 * thus constraining the possible blendings. The company must also satisfy its customer demand,
 * which is 3,000 barrels a day of super, 2,000 of regular, and 1,000 of diesel. The company can
 * purchase 5,000 barrels of each type of crude oil per day and can process at most 14,000 barrels a day.
 * In addition, the company has the option of advertising a gasoline, in which case the demand for this type
 * of gasoline increases by ten barrels for every dollar spent. Finally, it costs four dollars
 * to transform a barrel of oil into a barrel of gasoline </p>
 * <p>Copyright: Copyright (c) 2002 </p>
 * <p>Company: Exigengroup</p>
 * @author unascribed
 * @version 1.0
 */

import java.io.DataOutputStream;
import java.io.FileOutputStream;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.impl.FloatVarImpl;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;
import org.openl.ie.constrainer.lpsolver.GoalSimplexSolve;
import org.openl.ie.constrainer.lpsolver.impl.LPProblemImpl;

public class BlendingProblem {

  public BlendingProblem() {
  }
  public static void main(String[] args) {
  try{

    String oilTypes[] = {"Crude1", "Crude2", "Crude3"};
    String gasTypes[] = {"Super","Regular","Diesel"};
    double demands[] = {3000,2000,1000};
    double capacities[] = {5000, 5000, 5000};
    double gas_octane[] = {10,8,6};
    double oil_octane[] = {12,6,8};
    double gas_lead[] = {1,2,1};
    double oil_lead[] = {0.5,2,3};
    double gas_prices[] = {70,60,50};
    double oil_prices[] = {45,35,25};
    int maxProduction = 14000;
    double prodCost = 4;


    Constrainer C = new Constrainer("BlendingProblem");

    FloatVar blendsTbl[][] = new FloatVar[3][3];
    // It would be 9 variables corresponding to the appropriate blending values
    FloatExpArray blends = new FloatExpArray(C, 9);
    double[] income = new double[9];
    for (int i=0;i<oilTypes.length;i++){
      for (int j=0;j<gasTypes.length;j++){
        FloatVar var = new FloatVarImpl(C, 0, maxProduction, oilTypes[i] + "->" + gasTypes[j]);
        blendsTbl[i][j] = var;
        blends.set(var,gasTypes.length*i+j);
        income[gasTypes.length*i+j] = gas_prices[j] - oil_prices[i] - prodCost;
      }
    }
    // and three variables corresponding to the amount of gasoline consumed for advertisement purposes.
    FloatExpArray adv = new FloatExpArray(C, 3);
    for (int i=0;i<gasTypes.length;i++){
      FloatVar var = new FloatVarImpl(C, 0, maxProduction, "adv["+gasTypes[i]+"]");
      adv.set(var, i);
    }

    // cost function to be maximized (pure income)
    FloatExp cost = C.scalarProduct(blends, income).sub(adv.sum());
    ConstrainerLP smpl = new LPProblemImpl(cost, true);

    //demand constraints
    for (int j=0;j<gasTypes.length;j++){
      FloatExpArray tmp = new FloatExpArray(C, oilTypes.length);
      for (int i=0;i<oilTypes.length;i++){
        tmp.set(blendsTbl[i][j], i);
      }
      smpl.addConstraint(tmp.sum().eq(adv.get(j).mul(10).add(demands[j])), true);
    }

    //purchase limitation
    for (int i=0;i<oilTypes.length;i++){
      FloatExpArray tmp = new FloatExpArray(C, gasTypes.length);
      for (int j=0;j<gasTypes.length;j++){
        tmp.set(blendsTbl[i][j], j);
      }
      smpl.addConstraint(tmp.sum().le(capacities[i]), false);
    }

    //quality constraints:
    for (int j=0;j<gasTypes.length;j++){
      FloatExpArray tmp = new FloatExpArray(C, oilTypes.length);
      double[] octaneDiff = new double[oilTypes.length];
      double[] leadDiff = new double[oilTypes.length];
      for (int i=0;i<oilTypes.length;i++){
        tmp.set(blendsTbl[i][j], i);
        octaneDiff[i] = oil_octane[i] - gas_octane[j];
        leadDiff[i] = oil_lead[i] - gas_lead[j];
      }
      //1. by octane rating
      smpl.addConstraint(C.scalarProduct(tmp, octaneDiff).ge(0), false);
      //2. by leading content
      smpl.addConstraint(C.scalarProduct(tmp, leadDiff).le(0), false);
    }
    //capacity limitation
    smpl.addConstraint(blends.sum().le(maxProduction), false);

    DataOutputStream fstr = new DataOutputStream(new FileOutputStream("out.txt"));
    fstr.writeBytes("Variables: \n");
    for (int i=0;i<smpl.nbVars();i++){
      fstr.writeBytes("" + i + ":" + smpl.getVar(i).name()+"\n");
    }
    fstr.writeBytes("Constraints: \n");
    for (int i=0; i<smpl.nbConstraints(); i++){
      fstr.writeBytes(smpl.getLPConstraint(i)+"\n");
    }

    Goal g1 = new GoalSimplexSolve(C,smpl);
    boolean flag = C.execute(g1);
    if (flag){
      fstr.writeBytes("Solution: \n");
      fstr.writeBytes(blends + "\n" + adv + "\n");
      fstr.writeBytes("Cost function value: " + cost.value());
    }
    else
      fstr.writeBytes("The problem has no solution: \n");
  }
  catch(NonLinearExpression e){
    System.err.println("Can't solve nonlinear problem");
  }
  catch(Failure f){
    System.err.println("Constrainer failed");
  }
  catch(Exception e){
    e.printStackTrace();
  }

  }
}
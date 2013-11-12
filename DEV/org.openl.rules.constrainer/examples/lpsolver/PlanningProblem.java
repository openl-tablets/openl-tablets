package lpsolver;

/**
 * <p>Title: <b>A multi-period production planning problem</b></p>
 * <p>Description:(The description was taken from Ilog's OPL Studio) The company aims at
 * minimizing the production cost for a number of products
 * while satisfying customer demand. Each product can be produced either inside the company
 * or outside,  at a higher  cost. The inside  production is constrained by  the  company's
 * resources, while outside production is considered unlimited. The model first declares the
 * products and the  resources. The data consists  of the description of the products, i.e.,
 * the demand,  the inside and outside costs, and the resource consumption, and the capacity
 * of the various  resources. The  variables for this  problem are the inside and  outside
 * production for each product. It is also possible to generalize the problem by considering
 * the demand for the products over several periods and allowing the company to  produce more
 * than the demand in a given  period. Of course, there is an inventory cost associated  with
 * storing the additional production</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigengroup</p>
 * @author unascribed
 * @version 1.0
 */
import java.io.DataOutputStream;
import java.io.FileOutputStream;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.impl.FloatVarImpl;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;
import org.openl.ie.constrainer.lpsolver.FloatExpTable;
import org.openl.ie.constrainer.lpsolver.GoalSimplexSolve;
import org.openl.ie.constrainer.lpsolver.impl.LPProblemImpl;

public class PlanningProblem {

  public PlanningProblem() {
  }
  public static void main(String[] args) {
   try{
    String[] products = { "kluski", "capellini", "fettucine"};
    String[] resources = {"flour", "eggs"};
//    int KLUSKI=0, CAPPELINI = 1, FETTUCINI = 2;
//    int FLOUR = 0, EGGS = 1;
    int nbPeriods = 3;
    double[][] consumption = {{0.5, 0.4, 0.3},{0.2, 0.4, 0.6}};
    int[] capacity = {20, 40};
    int[][] demand = {{10, 100, 50},
                      {20, 200, 100},
                      {50, 100, 100}};
    int[] inventory = {0, 0, 0};
    double[] invCost = {0.1, 0.2, 0.1};
    double[] inCost = {0.4, 0.6, 0.1};
    double[] outCost = {0.8, 0.9, 0.4};
//    PlanningProblem pp = new PlanningProblem();


  Constrainer C = new Constrainer("A Multi Period Production Planning Problem");
  FloatExpTable inside = new FloatExpTable(C, products.length, nbPeriods);
  FloatExpTable outside = new FloatExpTable(C,products.length, nbPeriods);
  FloatExpTable inv = new FloatExpTable(C, inventory.length, nbPeriods);
  for (int t=0; t<nbPeriods; t++){
    for (int p=0; p<products.length; p++){
      FloatVar invar = new FloatVarImpl(C, 0, Double.MAX_VALUE, products[p]+"_"+t+"_in");
      inside.set(invar, t, p);
      FloatVar outvar = new FloatVarImpl(C, 0, Double.MAX_VALUE, products[p]+"_"+t+"_out");
      outside.set(outvar, t, p);
      FloatVar invvar = new FloatVarImpl(C, 0,  Double.MAX_VALUE, products[p]+"_"+t+"_inv");
      inv.set(invvar, t, p);
    }
  }

  // cost function
  /*minimize
    sum(p in Products, t in Periods)
    (inCost[p]*inside[p,t] + outCost[p]*outside[p,t] + invCost[p]*inv[p,t])*/
  FloatExp cost = inside.mul(inCost).sum().add(
                        outside.mul(outCost).sum()).add(
                        inv.mul(invCost).sum());

  ConstrainerLP smpl = new LPProblemImpl(cost, false);

  // capacity constraint
  /*
   forall(r in Resources, t in Periods)
     sum(p in Products) consumption[r,p] * inside[p,t] <= capacity[r];*/
  for (int t=0;t<nbPeriods;t++){
    for (int r=0;r<resources.length;r++){
      smpl.addConstraint(C.scalarProduct(inside.getRow(t), consumption[r]).le(capacity[r]), false);
    }
  }

  // demand constraint
  /*  forall(p in Products, t in Periods)
        inv[p,t-1] + inside[p,t] + outside[p,t] = demand[p,t] + inv[p,t];*/
  for (int t=1;t<nbPeriods;t++){
    for (int p=0;p<products.length;p++){
      smpl.addConstraint(inv.get(t-1, p).add(inside.get(t, p)).add(outside.get(t, p)).eq(
                         inv.get(t, p).add(demand[p][t])), true);
    }
  }

  /*forall(p in Products)
   inv[p,0] = inventory[p];*/

  for (int p=0; p<products.length;p++){
    smpl.addConstraint(inside.get(0, p).add(outside.get(0, p)).add(inventory[p]).eq(
                       inv.get(0, p).add(demand[p][0])), true);
  }

/* Plan plan[p in Products, t in Periods] = <inside[p,t],outside[p,t],inv[p,t]>;
 display plan;*/
    DataOutputStream odstr = new DataOutputStream(new FileOutputStream("test2out.txt"));
    odstr.writeBytes(((LPProblemImpl)smpl).toString());
    String[] strt = new String[smpl.nbVars()];
    for (int i=0;i<smpl.nbVars();i++){
      strt[i] =smpl.getVar(i).name();
    };
    //Arrays.sort(strt);
    for (int i=0;i<strt.length;i++){
      odstr.writeBytes("\""+strt[i] + "\",\n");
    }

    Goal g = new GoalSimplexSolve(C, smpl);
    boolean flag = C.execute(g);

    // results output
    if (!flag){
      System.out.println("There are no solutions!");
      return;
    }
   for (int p=0; p<products.length;p++){
      for (int t=0;t<nbPeriods;t++){
        System.out.println("plan["+products[p] + ","+ (t+1) + "]=" +
                           " <inside:" + inside.get(t, p).value() +
                           ", outside:" + outside.get(t, p).value() +
                           ", inv:" + inv.get(t, p).value() + ">");
      }
    }
    System.out.println("Cost value:" + cost.value());
  }
  catch(Exception e){
    e.printStackTrace();
  }
  }
}
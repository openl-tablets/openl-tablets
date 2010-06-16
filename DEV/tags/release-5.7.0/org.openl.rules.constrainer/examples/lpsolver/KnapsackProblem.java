package lpsolver;

/**
 * <p>Title: <b>Knapsack problem</b></p>
 * <p>Description: We have a knapsack with a fixed capacity (an integer)
 * and a number of items. Each item has an associated weight (an integer)
 * and an  associated value (another integer). The  problem  consists of
 * filling the knapsack without exceeding its capacity, while maximizing
 * the overall value of its contents</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigengroup</p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.lpsolver.ConstrainerMIP;
import org.openl.ie.constrainer.lpsolver.GoalIntSimplexSolve;
import org.openl.ie.constrainer.lpsolver.impl.LPIntegerProblemImpl;

public class KnapsackProblem {

  public KnapsackProblem() {
  }

  public static void main(String[] argv){
    try{
      int nbResources = 7;
      int nbItems = 12;
      int[] capacity= {18209, 7692, 1333, 924, 26638, 61188, 13360};
      int[] value= {96, 76, 56, 11, 86, 10, 66, 86, 83, 12, 9, 81};
      int[][] use = {
       {19, 1, 10, 1, 1, 14, 152, 11, 1, 1, 1, 1},
       {0, 4, 53, 0, 0, 80, 0, 4, 5, 0, 0, 0},
       {4, 660, 3, 0, 30, 0, 3, 0, 4, 90, 0, 0},
       {7, 0, 18, 6, 770, 330, 7, 0, 0, 6, 0, 0},
       {0, 20, 0, 4, 52, 3, 0, 0, 0, 5, 4, 0},
       {0, 0, 40, 70, 4, 63, 0, 0, 60, 0, 4, 0},
       {0, 32, 0, 0, 0, 5, 0, 3, 0, 660, 0, 9}};
    /* var int take[Items] in 0..maxValue;
       maximize
         sum(i in Items) value[i] * take[i]
       subject to
         forall(r in Resources)
          sum(i in Items) use[r,i] * take[i] <= capacity[r];    */
      Constrainer C =new Constrainer("knapsack problem");
      // variables
      IntExpArray take = new IntExpArray(C, nbItems, 0, 61188, "take");
      // costFunction
      IntExp costFunc = C.scalarProduct(take, value);

      ConstrainerMIP lpi = new LPIntegerProblemImpl(costFunc, true);
      // capacity due constraints
      for (int r=0;r<nbResources;r++){
         lpi.addConstraint(C.scalarProduct(take, use[r]).le(capacity[r]), false);
      }

      Goal solve = new GoalIntSimplexSolve(C, lpi);
      boolean flag = C.execute(solve);
      if (!flag){
         System.out.println("The problem has no optimal solution!");
         return;
      }
      System.out.println("Optimized cost function value: " + costFunc);
      System.out.println("Variables: ");
      for (int i=0;i<take.size();i++){
         System.out.println("take["+i+"] = " + take.get(i).value());
      }
    }catch(Failure f){
      System.out.println(f);
      f.printStackTrace();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}
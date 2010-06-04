package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: ColorMin<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * We want to maximize the sum:<br>
 * <samp>
 * &nbsp;&nbsp;&nbsp;&nbsp;257 * (France != Luxembourg)<br>
 * &nbsp;+ 9043 * (Luxembourg != Germany)<br>
 * &nbsp;+&nbsp;&nbsp;568 * (Luxembourg != Belgium)<br>
 * </samp>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class ColorMin
{
  static final int MAX_COLORS_NUM = 2;
  static final String[] colors = {"blue", "white", "red", "green"};
  public ColorMin()
  {
  }
  public static void main(String[] args)
  {
      try{

        Constrainer C = new Constrainer("Map-coloring");
        IntVar Belgium = C.addIntVar(0,3,"Belgium");
        IntVar Denmark = C.addIntVar(0,3,"Denmark");
        IntVar France = C.addIntVar(0,3,"France");
        IntVar Germany = C.addIntVar(0,3,"Germany");
        IntVar Netherlands = C.addIntVar(0,3,"Netherland");
        IntVar Luxemburg = C.addIntVar(0,3,"Luxemburg");

        IntExpArray allVariables = new IntExpArray(C,Belgium,Denmark,France,Germany,Netherlands,Luxemburg);

        long time = System.currentTimeMillis();

        C.postConstraint((France.ne(Belgium)).and(France.ne(Germany)));
        C.postConstraint(Belgium.ne(Netherlands));
        C.postConstraint((Germany.ne(Denmark)).and(Germany.ne(Netherlands)));

        IntExp weightedSum = ((France.ne(Luxemburg)).mul(257)).add(
                              (Luxemburg.ne(Germany)).mul(9043)).add(
                              (Luxemburg.ne(Belgium)).mul(568)).mul(-1);



        for (int i=0;i<allVariables.size();i++){
            C.postConstraint((allVariables.get(i)).le(MAX_COLORS_NUM-1));
        }


        Goal all = new GoalFastMinimize(new GoalGenerate(allVariables),weightedSum);

        C.printInformation();
        C.execute(all);

        time = System.currentTimeMillis()-time;

        for (int i=0;i<allVariables.size();i++){
          System.out.println(allVariables.get(i).name() + " : " + colors[allVariables.get(i).value()]);
        }
        System.out.println ();
        System.out.println ("Calculation takes " + (time) + " milliseconds");
        System.out.println ("Maximum WeightedSum value is : " + (-1)*weightedSum.value ());
        System.out.println ();

      }
      catch(Failure ex){
        System.out.println(" There is no solution...");
        ex.toString();
      }
      catch(Exception ex){
        ex.printStackTrace();
      }
  }
}
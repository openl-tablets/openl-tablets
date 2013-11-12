package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title:  Map—oloring<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * A map-coloring problem involves choosing colors for the
 * countries on a map in such a way that at most four colors are
 * used and no two neighboring countries are the same color.
 * For our example, we will consider six countries: Belgium,
 * Denmark, France, Germany, Netherlands, and Luxembourg.  The
 * colors can be blue, white, red or green. <br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class MapColoring{
  static final String[] colors = {"red","green","blue","yellow"};

  static public void main (String[] argv){
    try{

      Constrainer C = new Constrainer("Map-coloring");
      IntVar Belgium = C.addIntVar(0,3,"Belgium");
      IntVar Denmark = C.addIntVar(0,3,"Denmark");
      IntVar France = C.addIntVar(0,3,"France");
      IntVar Germany = C.addIntVar(0,3,"Germany");
      IntVar Netherlands = C.addIntVar(0,3,"Netherland");
      IntVar Luxemburg = C.addIntVar(0,3,"Luxemburg");

      IntExpArray allVariables = new IntExpArray(C,Belgium,Denmark,France,Germany,Netherlands,Luxemburg);
      C.postConstraint( ((France.ne(Belgium)).and(France.ne(Luxemburg))).and(France.ne(Germany)) );
      C.postConstraint( (Luxemburg.ne(Germany)).and(Luxemburg.ne(Belgium)) );
      C.postConstraint(  Belgium.ne(Netherlands) );
      C.postConstraint( (Germany.ne(Netherlands)).and(Germany.ne(Denmark)) );

      C.execute( new GoalAnd(new GoalGenerateAll(allVariables),
                                new GoalPrint(allVariables)));
    }
   catch (Exception ex) {
    ex.printStackTrace();
   }
  }
}


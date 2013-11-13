package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: Queens <br>
 * Description: The eight-queens problem involves placing eight queens
 * on a chess board in such a way that none of them can capture any
 * other using the conventional moves allowed to a queen. In other words,
 * the problem is to select eight squares on a chess board so that any
 * pair of selected squares is never aligned vertically, horizontally,
 * nor diagonally. Of course, the problem can be generalized to a board
 * of any size. In general terms, we have to select N squares on a board
 * with N squares on each side, still respecting the constraints of
 * non-alignment.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 1.0
 */

public class Queens {

    static class GoalCounter extends GoalImpl
    {  
	public GoalCounter(Constrainer c)
	{
	    super(c);
	}
	long cnt = 0;
	public Goal execute() throws Failure
	{
	    ++cnt;
	    return null;
	}
	
    }
    
    
    public static void main(String[] args) {
    try {
      String arg = (args.length==0)?"24":args[0];

      // board size and simultaneously the number of queens
      int board_size = Integer.parseInt(arg);
      Constrainer C = new Constrainer("Queens");

      // array of queens rows
      IntExpArray x = new IntExpArray(C, board_size);
      // auxillary arrays
      IntExpArray xplus = new IntExpArray(C, board_size);
      IntExpArray xminus = new IntExpArray(C, board_size);

      for(int i=0; i < board_size; i++) {
        IntVar variable =
          C.addIntVar(0, board_size-1, "q"+i, IntVar.DOMAIN_BIT_SMALL);
        x.set(variable, i);
        xplus.set(variable.add(i), i);
        xminus.set(variable.sub(i), i);
      }

      // all rows are different
      C.postConstraint (C.allDiff(x));
      // x[i] + i != x[j] + j
      C.postConstraint (C.allDiff(xplus));
      // x[i] - i != x[j] - j
      C.postConstraint (C.allDiff(xminus));

      GoalCounter gc = new GoalCounter(C);
      
      C.printInformation();
      // searching for solution
      // C.execute (new GoalGenerate(x)); // not optimized search
      // optimized search with variable selector and dichotomize
      C.execute (
	      new GoalAnd(
	      new GoalGenerate(x,new IntVarSelectorMinSizeMin(x), true), 
	      gc,
	      new GoalFail(C))
	      );

      // print the found solution
//      System.out.println(x);
      System.out.println("Solutions: " + gc.cnt);
    } catch(Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

    
    
    
    
  }
}
package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: Magic Square example<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * A magic square is a square matrix where the sum of every row,
 * column, and diagonal is equal to the same value; the numbers is
 * the magic square are consecutive and start with 1. <br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 1.0
 */

public class MagicSquare {
  public static void main(String[] args) {
    try {
      String arg = (args.length==0)?"5":args[0];
      int n = Integer.parseInt(arg);
      int sum = n*(n*n+1)/2;

      int i, j;
      // create Constrainer instance
      Constrainer C = new Constrainer ("Magic Square");
      // create all magic square elements
      IntExpArray vars = new IntExpArray(C, n*n, 1, n*n, "vars");
      // all elements must be unique
      C.postConstraint(C.allDiff(vars));

      // create arrays of rows and columns
      IntExpArray[] rows = new IntExpArray[n];
      IntExpArray[] columns = new IntExpArray[n];

      // create arrays for diagonals
      IntExpArray diagonal1 = new IntExpArray(C, n);
      IntExpArray diagonal2 = new IntExpArray(C, n);

      for (i = 0; i < n; i++) {
        // create arrays for the i-th row and column
        rows[i] = new IntExpArray(C, n);
        columns[i] = new IntExpArray(C, n);
        // populate the arrays
        for (j = 0; j < n; j++) {
          rows[i].set(vars.get(i * n + j), j);
          columns[i].set(vars.get(j * n + i), j);
        }
        diagonal1.set(vars.get(i*n + i), i);
        diagonal2.set(vars.get(i*n + (n - i - 1)), i);
        // the i-th row and colunm are populated, let's impose constraint on their sums
        C.postConstraint(rows [i].sum().eq(sum));
        C.postConstraint(columns [i].sum().eq(sum));
      }

      // the diagonals are populated, let's impose constraint on their sums
      C.postConstraint(diagonal1.sum().eq(sum));
      C.postConstraint(diagonal1.sum().eq(sum));

      // search a solution
      if (C.execute(new GoalGenerate (vars))) {
        // print the solution found
        for (i = 0; i < n; i++) {
          for (j = 0; j < n; j++) {
            System.out.print(vars.get(i * n + j).value()+" ");
          }
          System.out.println();
        }
      } else {
        System.out.println("No solutions");
      }
    } catch (Failure e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }
}
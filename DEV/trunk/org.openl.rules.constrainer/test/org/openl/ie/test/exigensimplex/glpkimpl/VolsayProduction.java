package org.openl.ie.test.exigensimplex.glpkimpl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import org.openl.ie.exigensimplex.LPProblem;
import org.openl.ie.exigensimplex.MatrixRow;
import org.openl.ie.exigensimplex.NoSolutionException;
import org.openl.ie.exigensimplex.SearchDirection;
import org.openl.ie.exigensimplex.VariableType;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;

public class VolsayProduction {

    public static void main(String[] args) {
        /**
         * creates a new empty lp problem use GLPK implementation
         */
        LPProblem lp = new GLPKLPProblem();
        try {
            /**
             * Add two auxilliary variable to the problem object: the first is
             * an amount of Nitrogen used and another is that of hydrogen
             */
            lp.addRows(2);
            /**
             * Add two structural variable to the problem object: the first is
             * an amount of gas produced and another is that of chloride
             */
            lp.addColumns(2);
            /**
             * set an objevtive coefficient for the first structural variable
             * (the volume of ammoniac gas to be produced)
             */
            lp.setColumnCoeff(0, 40);
            /**
             * Impose a constraints on a first structural variable It's quite
             * obvious that the volume of gas produced coudn't be assigned to a
             * negative value so one should prevent the solver from checking
             * such a solutions by putting a constraint type of the variable to
             * VariableType.BOUNDED_BELOW and setting zero as it's lower bound
             * (the value for upper bound has to be omitted by the solver in
             * this case)
             */
            lp.setColumnBounds(0, VariableType.BOUNDED_BELOW, 0, 0);
            lp.setColumnName(0, "gas");
            /**
             * set an objective coefficient for the second structural variable
             * (the volume of ammonium chloride to be produced)
             */
            lp.setColumnCoeff(1, 50);
            /**
             * impose constraints on the second structural variable As the
             * company has at it's disposal only 40 units of chlorine it is not
             * able to produce more than 40 units of ammonium chloride and the
             * negative values are not acceptable either so the appropriate
             * variable should be double bounded (VariableType.DOUBLE_BOUNDED)
             * with 0 as it's lower bound and 40 as the upper one.
             */
            lp.setColumnBounds(1, VariableType.DOUBLE_BOUNDED, 0, 40);
            lp.setColumnName(1, "chloride");

            double[] vals = { 1, 1 };
            /**
             * create the first row of a constraint matrix
             */
            lp.setMatrixRow(0, new MatrixRow(vals));
            /**
             * The first auxilliary variable is an amount of nitrogen used so
             * it's value is bounded within the range [0..50] . From the one
             * hand the negative values are infeasible from the other one the
             * value of a variable coudn't exceed that of being avilable for the
             * company (the company has only 50 units of nitrogen at it's
             * disposal according to the problem situation)
             */
            lp.setRowBounds(0, VariableType.DOUBLE_BOUNDED, 0, 50);

            vals[0] = 3;
            vals[1] = 4;
            /**
             * create the second row of a constraint matrix
             */
            lp.setMatrixRow(1, new MatrixRow(vals));
            /**
             * set bounds for the second auxilliary variable which is an amount
             * of hydrogen used.
             */
            lp.setRowBounds(1, VariableType.BOUNDED_ABOVE, 0, 180);

            /**
             * try to find a solution of a given lp maximizing an objective
             * function
             */
            int error = lp.solveLP(SearchDirection.MAXIMIZATION);
            if (error != 0) {
                throw new NoSolutionException(error);
            }
            System.out.println("Objective value:" + lp.getObjValue());
            System.out.println("Variables:");
            for (int i = 0; i < lp.getNumColumns(); i++) {
                System.out.println(lp.getColumnName(i) + " : " + lp.getColumnValue(i));
            }
        } catch (NoSolutionException ex) {
            int lastError = lp.getLastLPError();
            if (lastError == 0) {
                System.out.println(lp.errorAsString(ex.getErrorCode()));
            } else {
                System.out.println("Solver fails with message: " + lp.errorAsString(lastError));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            /**
             * free memory
             */
            lp.deleteCurrentLP();
        }
    }

    public VolsayProduction() {
    }
}
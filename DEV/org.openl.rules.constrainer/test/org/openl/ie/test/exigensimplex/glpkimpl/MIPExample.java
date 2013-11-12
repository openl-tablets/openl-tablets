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
import org.openl.ie.exigensimplex.glpkimpl.Algorithm;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;
import org.openl.ie.exigensimplex.glpkimpl.MIPAlgorithm;
import org.openl.ie.exigensimplex.glpkimpl.Parameters;

public class MIPExample extends Object {

    static public void main(String[] argv) {
        LPProblem lp = new GLPKLPProblem();
        /**
         * When using GLPK library the command below allows to prevent the
         * solver from outputing intermediate results
         */
        lp.setIntParam(Parameters.MSG_LEV, 0);
        try {
            int nbResources = 7;
            int nbItems = 12;
            int[] capacity = { 18209, 7692, 1333, 924, 26638, 61188, 13360 };
            int[] value = { 96, 76, 56, 11, 86, 10, 66, 86, 83, 12, 9, 81 };
            int[][] use = { { 19, 1, 10, 1, 1, 14, 152, 11, 1, 1, 1, 1 }, { 0, 4, 53, 0, 0, 80, 0, 4, 5, 0, 0, 0 },
                    { 4, 660, 3, 0, 30, 0, 3, 0, 4, 90, 0, 0 }, { 7, 0, 18, 6, 770, 330, 7, 0, 0, 6, 0, 0 },
                    { 0, 20, 0, 4, 52, 3, 0, 0, 0, 5, 4, 0 }, { 0, 0, 40, 70, 4, 63, 0, 0, 60, 0, 4, 0 },
                    { 0, 32, 0, 0, 0, 5, 0, 3, 0, 660, 0, 9 } };

            lp.setProblemName("MIPExample");
            lp.addColumns(value.length);
            lp.addRows(nbResources);
            /**
             * Tell the solver to treat the problem as being of MIP-kind
             */
            lp.ascribeMIPStatus();

            // cost function
            for (int i = 0; i < value.length; i++) {
                lp.setColumnCoeff(i, value[i]);
                lp.setColumnBounds(i, VariableType.BOUNDED_BELOW, 0, 0);
                lp.setColumnName(i, "take[" + i + "]");
                lp.markColumnAsIntVar(i);
            }

            double[] vals = new double[value.length];
            for (int r = 0; r < nbResources; r++) {
                for (int i = 0; i < value.length; i++) {
                    vals[i] = use[r][i];
                }
                lp.setMatrixRow(r, new MatrixRow(vals));
                lp.setRowBounds(r, VariableType.BOUNDED_ABOVE, 0, capacity[r]);
            }
            /**
             * Choose an algorithm of solving LP problems
             */
            lp.setAlgorithm(Algorithm.TWO_PHASED_REVISED_SIMPLEX);
            /**
             * Choose an algorithm of solving MIP problems
             */
            lp.setMIPAlgorithm(MIPAlgorithm.BRANCH_AND_BOUNDS);
            int status = lp.solveMIP(SearchDirection.MAXIMIZATION);
            /**
             * The result's code being equal to zero indicates that the problem
             * is successfully solved
             */
            if (status == 0) {
                System.out.println("Objective function: " + lp.getMIPObjVal());
                for (int i = 0; i < lp.getNumColumns(); i++) {
                    System.out.println(lp.getColumnName(i) + "=" + lp.getMIPColumnValue(i));
                }
                /**
                 * Write a solution of the last MIP problem to the file
                 * specified
                 */
                lp.printMIPSolutionToFile("MIPExample.sol");
            }

        } catch (NoSolutionException ex) {
            System.out.println(lp.errorAsString(ex.getErrorCode()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            /**
             * free memory
             */
            lp.deleteCurrentLP();
        }
    }

    public MIPExample() {
    }
}
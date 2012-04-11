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
import org.openl.ie.exigensimplex.glpkimpl.Algorithm;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;
import org.openl.ie.exigensimplex.glpkimpl.Parameters;

public class WorkingWithFiles {
    public static void main(String[] args) {
        LPProblem lp = new GLPKLPProblem();
        /**
         * suppress output
         */
        lp.setIntParam(Parameters.MSG_LEV, 0);

        try {
            String path = "..\\..\\ProblemsArchieve\\";
            String lpfile = "Problem.lp";
            /**
             * read the problem data written in a CPLEX's LP format from a file
             * specified by it's symbollic name
             */
            lp.readLP(path + lpfile);
            lp.setAlgorithm(Algorithm.TWO_PHASED_REVISED_SIMPLEX);
            /**
             * Attempt to solve a problem using two pahased revised simplex
             * method
             */
            int status = lp.solveLP();
            if (status != 0) {
                /**
                 * if failed try interior point algoritm
                 */
                lp.setAlgorithm(Algorithm.INTERIOR_POINT);
                status = lp.solveLP();
            }

            if (status == 0) {
                /**
                 * if succeeded output the results
                 */
                System.out.println(lp.getProblemStatus());
                System.out.println("The problem was successfully solved");
                lp.printSolutionToFile("WorkingWithFiles.sol");
            } else {
                System.out.println("The problem hasn't been solved!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            lp.deleteCurrentLP();
        }
    }

}
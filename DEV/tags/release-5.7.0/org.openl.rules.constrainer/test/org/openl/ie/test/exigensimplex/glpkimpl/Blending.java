package org.openl.ie.test.exigensimplex.glpkimpl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Arrays;

import org.openl.ie.exigensimplex.LPProblem;
import org.openl.ie.exigensimplex.MatrixRow;
import org.openl.ie.exigensimplex.NoSolutionException;
import org.openl.ie.exigensimplex.SearchDirection;
import org.openl.ie.exigensimplex.VariableType;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;


public class Blending {

    static public void main(String[] argv) {
        LPProblem lp = new GLPKLPProblem();
        try {
            boolean output_task = false;
            String oilTypes[] = { "Crude1", "Crude2", "Crude3" };
            String gasTypes[] = { "Super", "Regular", "Diesel" };
            double demands[] = { 3000, 2000, 1000 };
            double capacities[] = { 5000, 5000, 5000 };
            double gas_octane[] = { 10, 8, 6 };
            double oil_octane[] = { 12, 6, 8 };
            double gas_lead[] = { 1, 2, 1 };
            double oil_lead[] = { 0.5, 2, 3 };
            double gas_prices[] = { 70, 60, 50 };
            double oil_prices[] = { 45, 35, 25 };
            int maxProduction = 14000;

            // LPX.loadLibrary(new File("").getAbsolutePath()+"\\",
            // "glpDll.dll");
            lp.setProblemName("Blending_Problem");
            lp.addRows(13);
            // 13 - is an amount of constraints in the problem:
            // 3 demand constraints
            // 3 constraints for purchase limitation to be included
            // 1 is a capacity limitation
            // 6 is for quality criteria

            lp.addColumns(9);
            // 9 variables
            for (int i = 0; i < 9; i++) {
                lp.setColumnName(i, oilTypes[i / 3] + "_" + gasTypes[i % 3]);
                lp.setColumnBounds(i, VariableType.BOUNDED_BELOW, 0, 0); // all
                                                                            // variables
                                                                            // are
                                                                            // bounded
                                                                            // below
            }
            // advertising values
            lp.addColumns(3);
            for (int i = 9; i <= 11; i++) {
                lp.setColumnName(i, "adv" + i);
                lp.setColumnBounds(i, VariableType.BOUNDED_BELOW, 0, 0); // all
                                                                            // variables
                                                                            // are
                                                                            // bounded
                                                                            // below
            }

            // the coefficients in the particlar row of the constraint matrix
            double vals[] = new double[12];

            int rownum = 0;
            // demands
            for (int i = 0; i < gasTypes.length; i++) {
                vals[i] = 1; // crude1_gasType
                vals[i + gasTypes.length] = 1; // crude2_gasType
                vals[i + 2 * gasTypes.length] = 1; // crude3_gasType
                vals[9 + i] = -10; // advertising
                lp.setRowName(rownum, "demand" + i);
                lp.setRowBounds(rownum, VariableType.FIXED_VARIABLE, demands[i], 0);
                System.out.println(lp.getRowBounds(rownum));
                lp.setMatrixRow(rownum, new MatrixRow(vals));
                Arrays.fill(vals, 0);
                rownum++;
            }

            // purchase limitations
            for (int i = 0; i < oilTypes.length; i++) {
                for (int j = 0; j < gasTypes.length; j++) {
                    vals[gasTypes.length * i + j] = 1;
                }
                lp.setRowName(rownum, "plim" + i);
                lp.setRowBounds(rownum, VariableType.DOUBLE_BOUNDED, 0, capacities[i]);
                System.out.println(lp.getRowBounds(rownum));
                lp.setMatrixRow(rownum, new MatrixRow(vals));
                Arrays.fill(vals, 0);
                rownum++;
            }

            // capacity limitation
            lp.setRowName(rownum, "clim");
            lp.setRowBounds(rownum, VariableType.DOUBLE_BOUNDED, 0, maxProduction);
            System.out.println(lp.getRowBounds(rownum));
            for (int i = 0; i < 9; i++) {
                vals[i] = 1;
            }
            lp.setMatrixRow(rownum, new MatrixRow(vals));

            Arrays.fill(vals, 0);
            rownum++;

            // quality constraints based on octane rating
            for (int i = 0; i < gasTypes.length; i++) {
                for (int j = 0; j < oilTypes.length; j++) {
                    vals[i + j * gasTypes.length] = oil_octane[j] - gas_octane[i];
                }
                lp.setRowName(rownum, "quality" + i);
                lp.setRowBounds(rownum, VariableType.BOUNDED_BELOW, 0, 0);
                lp.setMatrixRow(rownum, new MatrixRow(vals));
                Arrays.fill(vals, 0);
                rownum++;
            }

            // quality constraints based on lead content
            for (int i = 0; i < gasTypes.length; i++) {
                for (int j = 0; j < oilTypes.length; j++) {
                    vals[i + j * gasTypes.length] = oil_lead[j] - gas_lead[i];
                }
                lp.setRowName(rownum, "quality" + (3 + i));
                lp.setRowBounds(rownum, VariableType.BOUNDED_ABOVE, 0, 0);
                lp.setMatrixRow(rownum, new MatrixRow(vals));
                Arrays.fill(vals, 0);
                rownum++;
            }
            // objective function
            for (int i = 0; i < oilTypes.length; i++) {
                for (int j = 0; j < gasTypes.length; j++) {
                    lp.setColumnCoeff(i * gasTypes.length + j, gas_prices[j] - oil_prices[i]);
                }
            }
            // we have to subtract advertising cost from the objective function
            lp.setColumnCoeff(9, -1);
            lp.setColumnCoeff(10, -1);
            lp.setColumnCoeff(11, -1);

            // We are aimed at gaining maximal income
            long start_time = System.currentTimeMillis();
            int status = lp.solveLP(SearchDirection.MAXIMIZATION);
            // int status = lp.solveLP();
            long finish_time = System.currentTimeMillis();
            System.out.println("Execution time: " + (finish_time - start_time) + "ms");
            // System.out.println(lp.errorAsString(lp.getProblemStatus()) +
            // lp.getProblemStatus());
            if (true/* SimplexErrorCodes.isSuccessful(status) */) {
                // lp.printSolution("out");
                System.out.println("Object value:" + lp.getObjValue());
                System.out.println("Solution status is \"" + lp.errorAsString(lp.getProblemStatus()) + "\"");
                System.out.println("Vars:");
                for (int i = 0; i < lp.getNumColumns(); i++) {
                    System.out.println(lp.getColumnName(i) + " : " + lp.getColumnValue(i));
                }
            } else {
                // System.out.println(Status.translate(status));
            }
            lp.printSolutionToFile("Blending.sol");
            lp.deleteCurrentLP();

        } catch (NoSolutionException ex) {
            System.out.println();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

        }
    }
}
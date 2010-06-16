package org.openl.ie.test.simplex;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Arrays;

import org.openl.ie.simplex.ArrayOut;
import org.openl.ie.simplex.Direction;
import org.openl.ie.simplex.LPX;
import org.openl.ie.simplex.Status;
import org.openl.ie.simplex.VarType;
import org.openl.ie.simplex.WrongLPX;


public class Blending {

    static public void main(String[] argv) {
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

            LPX lp = new LPX();
            // LPX.loadLibrary(new File("").getAbsolutePath()+"\\",
            // "glpDll.dll");
            lp.createLPX();
            lp.setName("Blending_Problem");
            lp.addRows(13);
            // 13 - is an amount of constraints in the problem:
            // 3 demand constraints
            // 3 constraints for purchase limitation to be included
            // 1 is a capacity limitation
            // 6 is for quality criteria

            lp.addColumns(9);
            // 9 variables
            for (int i = 0; i < 9; i++) {
                lp.setColName(i, oilTypes[i / 3] + "_" + gasTypes[i % 3]);
                lp.setColBnds(i, VarType.LPX_LO, 0, 0); // all variables are
                                                        // bounded below
            }
            // advertising values
            lp.addColumns(3);
            for (int i = 9; i <= 11; i++) {
                lp.setColName(i, "adv" + i);
                lp.setColBnds(i, VarType.LPX_LO, 0, 0); // all variables are
                                                        // bounded below
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
                lp.setRowBnds(rownum, VarType.LPX_FX, demands[i], 0);
                lp.setMatRow(rownum, vals);
                if (output_task) {
                    System.out.println(lp.getRowName(rownum) + ":" + lp.getRowBnds(rownum) + ", "
                            + new ArrayOut(lp.getMatRow(rownum)));
                }
                Arrays.fill(vals, 0);
                rownum++;
            }

            // purchase limitations
            for (int i = 0; i < oilTypes.length; i++) {
                for (int j = 0; j < gasTypes.length; j++) {
                    vals[gasTypes.length * i + j] = 1;
                }
                lp.setRowName(rownum, "plim" + i);
                lp.setRowBnds(rownum, VarType.LPX_DB, 0, capacities[i]);
                lp.setMatRow(rownum, vals);
                if (output_task) {
                    System.out.println(lp.getRowName(rownum) + ":" + lp.getRowBnds(rownum) + ", "
                            + new ArrayOut(lp.getMatRow(rownum)));
                }
                Arrays.fill(vals, 0);
                rownum++;
            }

            // capacity limitation
            lp.setRowName(rownum, "clim");
            lp.setRowBnds(rownum, VarType.LPX_DB, 0, maxProduction);
            for (int i = 0; i < 9; i++) {
                vals[i] = 1;
            }
            lp.setMatRow(rownum, vals);
            if (output_task) {
                System.out.println(lp.getRowName(rownum) + ":" + lp.getRowBnds(rownum) + ", "
                        + new ArrayOut(lp.getMatRow(rownum)));
            }
            Arrays.fill(vals, 0);
            rownum++;

            // quality constraints based on octane rating
            for (int i = 0; i < gasTypes.length; i++) {
                for (int j = 0; j < oilTypes.length; j++) {
                    vals[i + j * gasTypes.length] = oil_octane[j] - gas_octane[i];
                }
                lp.setRowName(rownum, "quality" + i);
                lp.setRowBnds(rownum, VarType.LPX_LO, 0, 0);
                lp.setMatRow(rownum, vals);
                if (output_task) {
                    System.out.println(lp.getRowName(rownum) + ":" + lp.getRowBnds(rownum) + ", "
                            + new ArrayOut(lp.getMatRow(rownum)));
                }
                Arrays.fill(vals, 0);
                rownum++;
            }

            // quality constraints based on lead content
            for (int i = 0; i < gasTypes.length; i++) {
                for (int j = 0; j < oilTypes.length; j++) {
                    vals[i + j * gasTypes.length] = oil_lead[j] - gas_lead[i];
                }
                lp.setRowName(rownum, "quality" + (3 + i));
                lp.setRowBnds(rownum, VarType.LPX_UP, 0, 0);
                lp.setMatRow(rownum, vals);
                if (output_task) {
                    System.out.println(lp.getRowName(rownum) + ":" + lp.getRowBnds(rownum) + ", "
                            + new ArrayOut(lp.getMatRow(rownum)));
                }
                Arrays.fill(vals, 0);
                rownum++;
            }
            // objective function
            for (int i = 0; i < oilTypes.length; i++) {
                for (int j = 0; j < gasTypes.length; j++) {
                    lp.setColCoef(i * gasTypes.length + j, gas_prices[j] - oil_prices[i]);
                }
            }
            // we have to subtract advertising cost from the objective function
            lp.setColCoef(9, -1);
            lp.setColCoef(10, -1);
            lp.setColCoef(11, -1);

            // We are aimed at gaining maximal income
            lp.setObjDir(Direction.MAX);

            int status = lp.simplexSolve();
            long start_time = System.currentTimeMillis();
            lp.simplexSolve();
            long finish_time = System.currentTimeMillis();
            System.out.println("Execution time: " + (finish_time - start_time) + "ms");

            if (true/* SimplexErrorCodes.isSuccessful(status) */) {
                // lp.printSolution("out");
                System.out.println("Object value:" + lp.getObjVal());
                System.out.println("Vars:");
                for (int i = 0; i < lp.getNumCols(); i++) {
                    System.out.println(lp.getColName(i) + " : " + lp.getBasicInfo(i));
                }
            } else {
                System.out.println(Status.translate(status));
            }

            lp.deleteLPX();

        } catch (WrongLPX e) {
            System.out.println("Use createLPX() method to create new LP");
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
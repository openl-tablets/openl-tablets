package org.openl.ie.test.simplex;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import org.openl.ie.simplex.Direction;
import org.openl.ie.simplex.LPX;
import org.openl.ie.simplex.VarType;

public class MIPExample extends Object {

    static public void main(String[] argv) {
        try {
            int nbResources = 7;
            int nbItems = 12;
            int[] capacity = { 18209, 7692, 1333, 924, 26638, 61188, 13360 };
            int[] value = { 96, 76, 56, 11, 86, 10, 66, 86, 83, 12, 9, 81 };
            int[][] use = { { 19, 1, 10, 1, 1, 14, 152, 11, 1, 1, 1, 1 }, { 0, 4, 53, 0, 0, 80, 0, 4, 5, 0, 0, 0 },
                    { 4, 660, 3, 0, 30, 0, 3, 0, 4, 90, 0, 0 }, { 7, 0, 18, 6, 770, 330, 7, 0, 0, 6, 0, 0 },
                    { 0, 20, 0, 4, 52, 3, 0, 0, 0, 5, 4, 0 }, { 0, 0, 40, 70, 4, 63, 0, 0, 60, 0, 4, 0 },
                    { 0, 32, 0, 0, 0, 5, 0, 3, 0, 660, 0, 9 } };

            LPX lp = new LPX();
            lp.createLPX();
            lp.setName("MIPExample");
            lp.addColumns(value.length);
            lp.addRows(nbResources);
            lp.setMIPStatus();
            lp.setObjDir(Direction.MAX);

            // cost function
            for (int i = 0; i < value.length; i++) {
                lp.setColCoef(i, value[i]);
                lp.setColBnds(i, VarType.LPX_LO, 0, 0);
                lp.setColName(i, "take[" + i + "]");
                lp.makeVarInt(i);
            }

            double[] vals = new double[value.length];
            for (int r = 0; r < nbResources; r++) {
                for (int i = 0; i < value.length; i++) {
                    vals[i] = use[r][i];
                }
                lp.setMatRow(r, vals);
                lp.setRowBnds(r, VarType.LPX_UP, 0, capacity[r]);
            }
            lp.simplexSolve();
            for (int i = 0; i < lp.getNumCols(); i++) {
                System.out.println(lp.getColName(i) + lp.getBasicInfo(i).getPrim());
            }
            System.out.println("---------------------------------------------------------------");
            System.out.println("MIP Solution:");
            int status = lp.solveMIP();
            // lp.processMIPSolution();
            for (int i = 0; i < lp.getNumCols(); i++) {
                System.out.println(lp.getColName(i) + "=" + lp.getMIPBasic(i));
                // System.out.println(lp.getColName(i) + "=" +
                // lp.getBasicInfo(i).getPrim());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MIPExample() {
    }
}
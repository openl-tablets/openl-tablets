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
import org.openl.ie.simplex.Status;
import org.openl.ie.simplex.VarType;

public class AnotherLP {
    public static void main(String[] args) {
        try {
            LPX lp = new LPX();
            lp.createLPX();
            // lp.setParm(Param.LPX_K_MSGLEV, 0);
            System.out.println("Direction " + lp.getObjDir());

            lp.addRows(2);
            lp.addColumns(2);
            lp.setColCoef(0, 40);
            lp.setColBnds(0, VarType.LPX_LO, 0, 0);
            lp.setColName(0, "gas");
            lp.setColCoef(1, 50);
            lp.setColBnds(1, VarType.LPX_DB, 0, 40);
            lp.setColName(1, "chloride");

            double[] vals = { 1, 1 };
            lp.setMatRow(0, vals);
            lp.setRowBnds(0, VarType.LPX_UP, 0, 50);

            vals[0] = 3;
            vals[1] = 4;
            lp.setMatRow(1, vals);
            lp.setRowBnds(1, VarType.LPX_UP, 0, 180);

            lp.setObjDir(Direction.MAX);

            long start_time = System.currentTimeMillis();
            lp.simplexSolve();
            long finish_time = System.currentTimeMillis();
            System.out.println("Execution time: " + (finish_time - start_time) + "ms");

            int status = lp.getStatus();
            System.out.println("The current solution status is " + Status.translate(status));
            if (true/* LPErrorCodes.isOptimal(status) */) {
                System.out.println("Object value:" + lp.getObjVal());
                System.out.println("Vars:");
                for (int i = 0; i < lp.getNumCols(); i++) {
                    System.out.println(lp.getColName(i) + " : " + lp.getBasicInfo(i));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
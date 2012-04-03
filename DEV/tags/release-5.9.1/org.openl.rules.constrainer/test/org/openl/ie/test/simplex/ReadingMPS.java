package org.openl.ie.test.simplex;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.simplex.LPX;
import org.openl.ie.simplex.Param;

public class ReadingMPS {
    private static final String path = "netlib\\";
    private static final String fname = "cycle.mps";

    public static void main(String[] args) {
        try {
            ReadingMPS readingMPS1 = new ReadingMPS();
            LPX lp = new LPX();
            lp.createLPX();
            lp.addColumns(5);
            long start = System.currentTimeMillis();
            lp.readMPS(path + fname);
            lp.setIntParm(Param.LPX_K_MSGLEV, 0);
            long readingTime = System.currentTimeMillis() - start;
            start = System.currentTimeMillis();
            lp.simplexSolve();
            long solvingTime = System.currentTimeMillis() - start;
            String outname = fname + ".txt";
            System.out.println("reading mps file: " + readingTime + " ms.");
            System.out.println("number of variables: " + lp.getNumCols());
            System.out.println("number of constraints: " + lp.getNumRows());
            System.out.println("solving time: " + solvingTime + " ms.");
            lp.printSolution(outname);
            lp.deleteLPX();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ReadingMPS() {
    }
}
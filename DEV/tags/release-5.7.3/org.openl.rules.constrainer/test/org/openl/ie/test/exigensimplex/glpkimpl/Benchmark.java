package org.openl.ie.test.exigensimplex.glpkimpl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.openl.ie.exigensimplex.LPProblem;
import org.openl.ie.exigensimplex.NoSolutionException;
import org.openl.ie.exigensimplex.glpkimpl.Algorithm;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;
import org.openl.ie.exigensimplex.glpkimpl.Parameters;


public class Benchmark {
    static LPProblem lp = new GLPKLPProblem();
    static private String reportFile = "benchmark.rep";

    static private FilenameFilter filter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if (name.endsWith(".mps")) {
                return true;
            }
            return false;
        }
    };

    public static void main(String[] args) {
        ObjectOutputStream out = null;
        try {
            String global_path = "..\\..\\ProblemsArchieve\\";
            String[] local_path = { "cplex_tests\\", "netlib_tests\\" };
            // String[] local_path = {"ip_tests\\", "cplex_tests\\",
            // "netlib_tests\\"};
            out = new ObjectOutputStream(new FileOutputStream(reportFile));
            out.writeBytes("Problem\tRows\tCols\tNonzeros\tOptimum\tIters\tTime,s\tMem,MB\n");
            // out.writeBytes("-------\t-----\t----\t--------\t--------\t----\t----\t----\n");
            out.close();
            out = new ObjectOutputStream(new FileOutputStream(reportFile, true));
            for (int i = 0; i < local_path.length; i++) {
                File fullpath = new File(global_path + local_path[i]);
                String[] list = fullpath.list(filter);

                String[] inlist = new String[list.length];
                for (int j = 0; j < list.length; j++) {
                    inlist[j] = global_path + local_path[i] + list[j];
                }

                String[] outlist = new String[list.length];
                for (int j = 0; j < list.length; j++) {
                    outlist[j] = list[j].substring(0, list[j].lastIndexOf(".mps")) + ".sol";
                }

                for (int j = 0; j < list.length; j++) {
                    lp.readMPS(inlist[j]);
                    lp.setAlgorithm(Algorithm.TWO_PHASED_REVISED_SIMPLEX);
                    lp.setIntParam(Parameters.MSG_LEV, 0);
                    lp.setIntParam(Parameters.SCALE_OPT, 3);
                    // lp.setIntParam(784, 1);
                    long start = System.currentTimeMillis();
                    int status = lp.solveLP();
                    long stop = System.currentTimeMillis();
                    if (status != 0) {
                        lp.setAlgorithm(Algorithm.INTERIOR_POINT);
                        start = System.currentTimeMillis();
                        lp.solveLP();
                        stop = System.currentTimeMillis();
                    }
                    lp.printSolutionToFile(outlist[j]);
                    int iter_cnt = lp.getIntParam(Parameters.ITER_COUNT);
                    printReport(out, lp, (stop - start) / 1000);
                    lp.deleteCurrentLP();
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            lp.deleteCurrentLP();
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    static private void printReport(ObjectOutputStream out, LPProblem lp, long time) throws FileNotFoundException,
            IOException {
        // Problem Rows Cols Nonzeros Optimum Iters Time,s Mem,MB
        out.writeBytes(lp.getProblemName() + "\t");
        out.writeBytes(lp.getNumRows() + "\t");
        out.writeBytes(lp.getNumColumns() + "\t");
        out.writeBytes(lp.getNumNonZero() + "\t");
        try {
            out.writeBytes(lp.getObjValue() + "\t");
        } catch (NoSolutionException nse) {
            out.writeBytes(nse.getErrorMessage() + "\t");
        }
        out.writeBytes(lp.getIntParam(Parameters.ITER_COUNT) + "\t");
        out.writeBytes(time + "\t");
        out.writeBytes((lp.getIntParam(Parameters.MEMCNT) / (1024 * 1024)) + "\n");
    }

    public Benchmark() {
    }

}
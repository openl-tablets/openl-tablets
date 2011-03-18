package org.openl.ie.exigensimplex.glpkimpl;

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

public class Parameters {
    /**
     * level of messages output by the GLPK's solver:
     * <ul>
     * <li> 0 -- no output
     * <li> 1 -- error messages only
     * <li> 2 -- normal output
     * <li> 3 -- full output (includes informational messages)
     * </ul>
     */
    static public final int MSG_LEV = Param.LPX_K_MSGLEV;

    /**
     * Scaling options
     * <ul>
     * <li> 0 -- no scaling
     * <li> 1 -- equilibration scaling
     * <li> 2 -- geometric mean scaling
     * </ul>
     */
    static public final int SCALE_OPT = Param.LPX_K_SCALE;

    /**
     * Dual simplex option
     * <ul>
     * <li> 0 -- don't use the dual simplex
     * <li> 1 -- if initial basic solution is dual feasible, use the dual
     * simplex
     * </ul>
     */
    static public final int DUAL_OPT = Param.LPX_K_DUAL;

    /**
     * pricing option (for both primal and dual simplex):
     * <ul>
     * <li> 0 -- textbook pricing
     * <li> 1 -- steepest edge pricing
     * </ul>
     */
    static public final int PRICE_OPT = Param.LPX_K_PRICE;

    /**
     * relative tolerance used to check if the current basic solution is primal
     * feasible
     */
    static public final int TOLBND = Param.LPX_K_TOLBND;

    /**
     * absolute tolerance used to check if the current basic solution is dual
     * feasible
     */
    static public final int TOLDJ = Param.LPX_K_TOLDJ;

    /**
     * relative tolerance used to choose eligible pivotal elements of the
     * simplex table in the ratio test
     */
    static public final int TOLPIV = Param.LPX_K_TOLPIV;

    /**
     * solution rounding option:
     * <ul>
     * <li> 0 -- report all computed values and reduced costs "as is"
     * <li> 1 -- if possible (allowed by the tolerances), replace computed
     * values and reduced costs which are close to zero by exact zeros
     * </ul>
     */
    static public final int SOLUTION_ROUNDING = Param.LPX_K_ROUND;

    /**
     * <ul>
     * <li> lower limit of the objective function; if on the phase II the
     * <li> objective function reaches this limit and continues decreasing,
     * <li> the solver stops the search
     * </ul>
     */
    static public final int LOWER_LIM = Param.LPX_K_OBJLL;

    /**
     * upper limit of the objective function; if on the phase II the objective
     * function reaches this limit and continues increasing, the solver stops
     * the search.
     */
    static public final int UPPER_LIM = Param.LPX_K_OBJUL;

    /**
     * simplex iterations limit; if this value is positive, it is decreased by
     * one each time when one simplex iteration has been performed, and reaching
     * zero value signals the solver to stop the search; negative value means no
     * iterations limit
     */
    static public final int ITER_LIM = Param.LPX_K_ITLIM;

    /**
     * simplex iterations count; this count is increased by one each time when
     * one simplex iteration has been performed
     */
    static public final int ITER_COUNT = Param.LPX_K_ITCNT;

    /**
     * searching time limit, in seconds; if this value is positive, it is
     * decreased each time when one simplex iteration has been performed by the
     * amount of time spent for the iteration, and reaching zero value signals
     * the solver to stop the search; negative value means no time limit
     */
    static public final int TIME_LIM = Param.LPX_K_TMLIM;

    /**
     * output frequency, in iterations; this parameter specifies how frequently
     * the solver sends information about the solution to the standard output
     */
    static public final int OUTPUT_FREQ = Param.LPX_K_OUTFRQ;

    /**
     * output delay, in seconds; this parameter specifies how long the solver
     * should delay sending information about the solution to the standard
     * output; zero value means no delay
     */
    static public final int OUTPUT_DELAY = Param.LPX_K_OUTDLY;

    /**
     * branching heuristic:
     * <ul>
     * <li> 0 -- branch on the first variable
     * <li> 1 -- branch on the last variable
     * <li> 2 -- branch using a heuristic by Driebeck and Tomlin
     * </ul>
     */
    static public final int BRANCH_HEURISTIC = Param.LPX_K_BRANCH;

    /**
     * for MIP backtracking heuristic:
     * <ul>
     * <li> 0 -- depth first search
     * <li> 1 -- breadth first search
     * <li> 2 -- backtrack using the best projection heuristic
     * </ul>
     */
    static public final int BTRACK_HEURISTIC = Param.LPX_K_BTRACK;

    /**
     * absolute tolerance used to check if the current basic solution is integer
     * feasible
     */
    static public final int MIP_TOLINT = Param.LPX_K_TOLINT;

    /**
     * relative tolerance used to check if the value of the objective function
     * is not better than in the best known integer feasible solution
     */
    static public final int MIP_TOLOBJ = Param.LPX_K_TOLOBJ;

    /**
     * This parameter tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * how to output the objective function row:
     * <ul>
     * <li> 1 - never output objective function row
     * <li> 2 - always output objective function row
     * <li> 3 - output objective function row if the problem doesn't have any
     * free rows
     * <ul>
     * <b>Default value is 2</b>
     */
    static public final int MPSOBJ = Param.LPX_K_MPSOBJ;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to add some special comments
     */
    static public final int MPSINFO = Param.LPX_K_MPSINFO;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to use original symbolic names of rows and columns, otherwise the routine
     * generates plain names using ordinal numbers of rows and columns.
     * <b>Default value is 0</b>
     */
    static public final int MPSORIG = Param.LPX_K_MPSORIG;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to use all data field otherwise it keeps the fields 5 and 6 empty
     * <b>Default value is 1</b>
     */
    static public final int MPSWIDE = Param.LPX_K_MPSWIDE;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to omit column and vectors names every time when possible, otherwise the
     * routine never omits them. <b>Default value is 0</b>
     */
    static public final int MPSFREE = Param.LPX_K_MPSFREE;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to skip empty columns <b>Default value is 0</b>
     */
    static public final int MPSSKIP = Param.LPX_K_MPSSKIP;

    static public final int RELAX = Param.LPX_K_RELAX;

    /**
     * total amount of currently allocated memory in bytes
     */
    static public final int MEMCNT = Param.LPX_K_MEMCNT;

    private Parameters() {
    }

}
package org.openl.ie.simplex;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.openl.util.Log;

/**
 * <p>
 * Title: JGSimplex
 * </p>
 * <p>
 * Description: JGSimplex is actually a wrapper class for the set of JNI calls.
 * It solves LP problems using GLPK (Gnu Linear Programming Kit)
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */
public class LPX {

    private int id = -1;
    private int nbRows = 0;
    private int nbCols = 0;
    private int nbMarkedRows = 0;
    private int nbMarkedColumns = 0;

    static {
        String dllName = "glpDll";
        File dllFile = new File(System.mapLibraryName(dllName));

        if (dllFile.exists()) {
            System.load(dllFile.getAbsolutePath());
        } else {
            System.loadLibrary(dllName); // java.library.path search
        }
    }

    static private boolean isAllDiff(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] == arr[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Constructor
     */
    public LPX() {
    }

    /**
     * Adds columns (variables) to the problem object. Rows are always appended
     * to the end of the column's list so the numbers of the existing columns
     * hasn't changed.
     *
     * @param num The number of columns to be added
     * @throws WrongLPX if the problem object doesn't exist
     */
    public void addColumns(int num) throws WrongLPX {
        ensureLPXCreated();
        native_add_cols(id, num);
        nbCols += num;
    }

    /**
     * Adds rows (constraints) to the problem object. Rows are always appended
     * to the end of the row list so the numbers of the existing rows hasn't
     * changed.
     *
     * @param num The number of rows to be added
     * @throws WrongLPX if the problem object doesn't exist
     */
    public void addRows(int num) throws WrongLPX {
        ensureLPXCreated();
        native_add_rows(id, num);
        nbRows += num;
    }

    /**
     * Nullifies previously marked rows and columns
     *
     * @throws WrongLPX if the problem object wasn't created
     */
    public void clearMat() throws WrongLPX {
        ensureLPXCreated();
        if ((nbMarkedColumns != 0) || (nbMarkedRows != 0)) {
            native_clear_mat(id);
        }
    }

    // for working with
    // LPX------------------------------------------------------------------
    /**
     * Creates a new EMPTY problem object
     */
    public void createLPX() {
        if (id == -1) {
            id = native_create_lpx();
        }
    }

    /**
     * Removes a problem object and releases the memory
     */
    public void deleteLPX() {
        if (noLPX()) {
            return;
        }
        native_delete_lpx(id);
        id = -1;
    }

    /**
     * Removes all marked rows and columns from the problem object
     *
     * @throws WrongLPX if the problem object wasn't created
     */
    public void delItems() throws WrongLPX {
        ensureLPXCreated();
        if ((nbMarkedColumns != 0) || (nbMarkedRows != 0)) {
            native_del_items(id);
            nbRows -= nbMarkedRows;
            nbCols -= nbMarkedColumns;
            nbMarkedRows = 0;
            nbMarkedColumns = 0;
        }
    }

    private void ensureColumnExist(int colnum) throws WrongLPX {
        ensureLPXCreated();
        if (nbCols <= colnum) {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }

    private void ensureLPXCreated() throws WrongLPX {
        if (id == -1) {
            throw new WrongLPX();
        }
    }

    private void ensureRowExist(int rownum) throws WrongLPX {
        ensureLPXCreated();
        if (nbRows <= rownum) {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }

    /**
     * Returns an object of type {@link VarInfo} containing the information
     * about the variable.
     *
     * @param rownum The number of auxiliary variable
     * @return An object of type {@link VarInfo}
     * @throws WrongLPX if the problem object wasn't created
     */
    public VarInfo getAuxInfo(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        return new VarInfo(native_get_row_info(id, rownum + 1));
    }

    /**
     * Returns an object of type {@link VarInfo} containing the information
     * about the variable.
     *
     * @param colnum The number of basic variable.
     * @return An object of type {@link VarInfo}
     * @throws WrongLPX if the problem object wasn't created
     */
    public VarInfo getBasicInfo(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        return new VarInfo(native_get_col_info(id, colnum + 1));
    }

    /**
     * Return the value of the given real-valued parameter
     *
     * @param parm The parameter
     * @return The value of the parameter given by the first argument
     * @throws WrongLPX if the problem object hasn't been created.
     */
    public double getBoolParm(int parm) throws WrongLPX {
        ensureLPXCreated();
        if (!Param.isValidBoolParam(parm)) {
            throw new IllegalArgumentException("GetBoolParm: " + "unknown parameter: " + parm);
        }
        return native_get_int_parm(id, Param.getParam(parm));
    }

    /**
     * Returns an object of the type {@link VarType} containing information
     * about the basic variable specified by it's number
     *
     * @param colnum The number of the variable in the LP problem object
     * @return An object of the type {@link VarType}
     * @throws WrongLPX
     */
    public VarType getColBnds(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        double[] temp = native_get_col_bnds(id, colnum + 1);
        return new VarType((int) temp[0], temp[1], temp[2]);
    }

    /**
     * Returns the appropriate coefficient of the objective function
     *
     * @param colnum the number of the variable which factor is to be returned
     * @return the value of the of the of the "colnum"'s coefficient of the
     *         objective function.
     * @throws WrongLPX if the problem object hasn't been created.
     */
    public double getColCoef(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        return native_get_col_coef(id, colnum + 1);
    }

    /**
     * Returns the symbolic name of the basic variable specified by it's index
     *
     * @param colnum An index of a basic variable in the given LP problem object
     * @return The string type variable associated with the variable specified
     * @throws WrongLPX if the problem object hasn't been created
     */
    public String getColName(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        return native_get_col_name(id, colnum + 1);
    }

    /**
     * Reports the dual status of the basic solution obtained by the solver for
     * an LP problem object.
     *
     * @return an integer code of the status. It might possesses one of the
     *         following values:
     *         <ol>
     *         <li> {@link DualErrorCodes#LPX_D_UNDEF} - the dual status is
     *         undefined;
     *         <li> {@link DualErrorCodes#LPX_D_FEAS} - the solution is dual
     *         feasible;
     *         <li> {@link DualErrorCodes#LPX_D_INFEAS} - the solution is dual
     *         infeasible;
     *         <li> {@link DualErrorCodes#LPX_D_NOFEAS} - no dual feasible
     *         solution exists
     *         </ol>
     *         Note: one might call {@link Status#translate(int)} in order to
     *         obtain the human-readable interpretation of the status returned.
     * @throws WrongLPX
     */
    public int getDualStatus() throws WrongLPX {
        ensureLPXCreated();
        return native_get_dual_status(id);
    }

    /**
     * Return the value of the given integer-valued parameter
     *
     * @param parm The parameter
     * @return The value of the parameter given by the first argument
     * @throws WrongLPX if the problem object hasn't been created.
     */
    public int getIntParm(int parm) throws WrongLPX {
        ensureLPXCreated();
        if (parm == Param.LPX_K_MEMCNT) {
            return native_get_total_memory_used(id);
        }
        if (!Param.isValidIntParam(parm)) {
            throw new IllegalArgumentException("GetIntParm: " + "unknown parameter: " + parm);
        }
        return native_get_int_parm(id, Param.getParam(parm));
    }

    /**
     * Returns an object of type {@link VarInfo} containing the information
     * about a structural variable.
     *
     * @param colnum The number of structural variable
     * @return An object of type {@link VarInfo}
     * @throws WrongLPX if the problem object wasn't created
     */
    public VarInfo getIPSColInfo(int colnum) throws WrongLPX {
        ensureRowExist(colnum);
        return new VarInfo(native_get_ips_col(id, colnum + 1));
    }

    /**
     * Returns the value of the objective function if the problem has been
     * successfully solved; otherwise it throws an exception
     * {@link NoSolutionException}
     *
     * @return the value of the objective function.
     * @throws WrongLPX if the proble object hasn't been created
     * @throws NoSolutionException
     */
    public double getIPSObjValue() throws WrongLPX, NoSolutionException {
        ensureLPXCreated();
        int status = getIPSStatus();
        if (!IPSErrorCodes.isOptimal(status)) {
            throw new NoSolutionException(Status.translate(status));
        }
        return native_get_ips_obj(id);
    }

    /**
     * Returns an object of type {@link VarInfo} containing the information
     * about an auxiliary variable.
     *
     * @param rownum The number of auxiliary variable
     * @return An object of type {@link VarInfo}
     * @throws WrongLPX if the problem object wasn't created
     */
    public VarInfo getIPSRowInfo(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        return new VarInfo(native_get_ips_row(id, rownum + 1));
    }

    /**
     * This routine reports the status of an interior point solution found by
     * the solver
     *
     * @returns one of the following codes:
     *          <ol>
     *          <li> <b>{@link IPSErrorCodes#LPX_T_UNDEF}</b> The interior
     *          point solution is undefined
     *          <li> <b>{@link IPSErrorCodes#LPX_T_OPT}</b> The interior point
     *          solution is optimal
     *          </ol>
     */
    public int getIPSStatus() {
        if (noLPX()) {
            return LPErrorCodes.NO_LP;
        }
        return native_get_ips_stat(id);
    }

    /**
     * Returns the appropriate column of the constraint matrix specified by it's
     * number.
     *
     * @param colnum The number of the column to get information about
     * @throws WrongLPX if the problem hasn't been created
     * @return The specified column of a constraint matrix represented as array
     *         of double[]
     */
    public double[] getMatCols(int colnum) throws WrongLPX {
        ensureRowExist(colnum);
        double[] temp = native_get_mat_row(id, colnum + 1);
        double[] col = new double[nbCols];
        int halfsize = temp.length / 2;
        for (int i = 0; i < halfsize; i++) {
            col[(int) temp[halfsize + i] - 1] = temp[i];
        }
        return col;
    }

    /**
     * Returns the row of the constraint matrix specified by it's number
     *
     * @param rownum The number of the row
     * @throws WrongLPX if the problem hasn't been created
     * @returns the appropriate row of constrained matrix represented by array
     *          of doubles (coefficients)
     *
     */
    public double[] getMatRow(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        double[] temp = native_get_mat_row(id, rownum + 1);
        double[] row = new double[nbCols];
        int halfsize = temp.length / 2;
        for (int i = 0; i < halfsize; i++) {
            row[(int) temp[halfsize + i] - 1] = temp[i];
        }
        return row;
    }

    /**
     * Returns a value of the auxiliary variable specified by it's number for
     * the MIP solution
     *
     * @param rownum the number of the variable
     * @return 0 if the problem hasn't been solved (use #processMIPSolution to
     *         check wether the solution has bee nfound); the value of the
     *         auxiliary variable for the optimal solution of MIP
     * @throws WrongLPX if the problem object hasn't been created
     * @throws NotAMIPProblem if the problem doesn't have a MIP status
     */

    public double getMIPAux(int rownum) throws WrongLPX, NotAMIPProblem {
        ensureRowExist(rownum);
        if (!isMIP()) {
            throw new NotAMIPProblem();
        }
        return native_get_mip_row(id, rownum + 1);
    }

    /**
     * Returns a value of the basic variable specified by it's number for the
     * MIP solution
     *
     * @param colnum the number of the variable
     * @return 0 if the problem hasn't been solved (use #processMIPSolution to
     *         check wether the solution has bee nfound); the value of the basic
     *         variable for the optimal solution of MIP
     * @throws WrongLPX if the problem object hasn't been created
     * @throws NotAMIPProblem if the problem doesn't have a MIP status
     */
    public double getMIPBasic(int colnum) throws WrongLPX, NotAMIPProblem {
        ensureColumnExist(colnum);
        if (!isMIP()) {
            throw new NotAMIPProblem();
        }
        return native_get_mip_col(id, colnum + 1);
    }

    /**
     * @return The optimal objective function value if any
     * @throws WrongLPX if the problem object hasn't been created
     * @throws NoSolutionException if no optimal solution has been found.
     */
    public double getMIPObjVal() throws WrongLPX, NoSolutionException {
        ensureLPXCreated();
        processMIPSolution();
        return native_get_mip_obj(id);
    }

    /**
     * Reports the status of the current basic solution obtained for MIP problem
     * object. One might invoke {@link Status#translate(int)} in order to obtain
     * the human-readable interpretation of the status returned.
     *
     * @return An integer code of the solution status
     * @see Status
     */
    public int getMIPStatus() {
        if (noLPX()) {
            return LPErrorCodes.NO_LP;
        }
        if (!isMIP()) {
            return MIPErrorCodes.NOT_A_MIP;
        }
        return native_get_mip_stat(id);
    }

    /**
     * Returns a name of the problem object
     *
     * @return problem object's name
     */
    public String getName() {
        if (noLPX()) {
            return "";
        }
        return native_get_name(id);
    }

    /**
     * @return Amount of integer-valued variables with 0 as their lower bound
     *         and 1 as their upper bound in the lp problem object.
     */
    public int getNumBoolVars() {
        if (noLPX() || (!isMIP())) {
            return 0;
        }
        return native_get_num_bin(id);
    }

    /**
     * Returns the number of variables belonging to the lp problem
     *
     * @return The number of columns
     */
    public int getNumCols() {
        if (noLPX()) {
            return 0;
        }
        return native_get_num_cols(id);
    }

    /**
     * @return Amount of integer-valued variables in the lp problem object
     */
    public int getNumIntVars() {
        if (noLPX() || (!isMIP())) {
            return 0;
        }
        return native_get_num_int(id);
    }

    /**
     * @return the number of nonzero elements in the constraint matrix
     */
    public int getNumNz() {
        if (noLPX()) {
            return 0;
        }
        return native_get_num_nz(id);
    }

    /**
     * Returns the number of constraints defining the lp problem
     *
     * @return The number of rows
     */
    public int getNumRows() {
        if (noLPX()) {
            return 0;
        }
        return native_get_num_rows(id);
    }

    // -----------------------------------------------------------------------------------

    /**
     * Returns the free term of the objective function If the lp problem object
     * doesn't exist it will return 0
     *
     * @returns The free term of the objective function
     * @throws WrongLPX if the problem object doesn't exist
     */
    public double getObjConst() throws WrongLPX {
        if (noLPX()) {
            return 0;
        }
        return native_get_obj_c0(id);
    }

    /**
     * Returns the integer code of the optimization direction. The return code
     * -1 indicates the absence of problem object
     *
     * @return {@link Direction#MAX} if the objective function is to be
     *         maximized or {@link Direction#MIN} if the objective function is
     *         to be minimized
     */
    public int getObjDir() {
        if (noLPX()) {
            return -1;
        }
        return native_get_obj_dir(id);
    }

    /**
     * Returns the value of the objective function if the problem has been
     * successfully solved; otherwise it throws an exception
     * {@link NoSolutionException}
     *
     * @return the value of the objective function.
     * @throws WrongLPX if the proble object hasn't been created
     * @throws NoSolutionException
     */
    public double getObjVal() throws WrongLPX, NoSolutionException {
        solutionProcess();
        return native_get_obj_val(id);
    }

    /**
     * Reports the primal status of the basic solution obtained by the solver
     * for an LP problem object.
     *
     * @return an integer code of the status. It might possesses one of the
     *         following values:
     *         <ol>
     *         <li> {@link PrimalErrorCodes#LPX_P_UNDEF} - the primal status is
     *         undefined;
     *         <li> {@link PrimalErrorCodes#LPX_P_FEAS} - the solution is primal
     *         feasible;
     *         <li> {@link PrimalErrorCodes#LPX_P_INFEAS} - the solution is
     *         primal infeasible;
     *         <li> {@link PrimalErrorCodes#LPX_P_NOFEAS} - no primal feasible
     *         solution exists
     *         </ol>
     *         Note: one might call {@link Status#translate(int)} in order to
     *         obtain the human-readable interpretation of the status returned.
     * @throws WrongLPX if the problem object wasn't created.
     */
    public int getPrimStatus() throws WrongLPX {
        ensureLPXCreated();
        return native_get_prim_status(id);
    }

    /**
     * Return the value of the given real-valued parameter
     *
     * @param parm The parameter
     * @return The value of the parameter given by the first argument
     * @throws WrongLPX if the problem object hasn't been created.
     */
    public double getRealParm(int parm) throws WrongLPX {
        ensureLPXCreated();
        if (!Param.isValidIntParam(parm)) {
            throw new IllegalArgumentException("GetRealParm: " + "unknown parameter: " + parm);
        }
        return native_get_real_parm(id, Param.getParam(parm));
    }

    /**
     * Returns an object of the type {@link VarType} containing information
     * about the row specified by it's number
     *
     * @param rownum The number of the row
     * @return An object of the type {@link VarType}
     * @throws WrongLPX
     */
    public VarType getRowBnds(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        double[] temp = native_get_row_bnds(id, rownum + 1);
        return new VarType((int) temp[0], temp[1], temp[2]);
    }

    /**
     * Returns the appropriate coefficient of the objective function
     *
     * @param rownum the number of an auxiliary variable which factor is to be
     *            returned
     * @return the value of the appropriate objective coefficient.
     * @throws WrongLPX if the problem object wasn't created properly
     */
    public double getRowCoef(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        return native_get_col_coef(id, rownum + 1);
    }

    /**
     * Returns the name of the row given by it's number
     *
     * @param rownum The number of the row
     * @return The name of the raw
     * @throws WrongLPX if the problem object hasn't been created
     */
    public String getRowName(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        return native_get_row_name(id, rownum + 1);
    }

    // Basic solution query routines
    /**
     * Reports the status of the current basic solution obtained for an LP
     * problem object. One might invoke {@link Status#translate(int)} in order
     * to obtain the human-readable interpretation of the status returned.
     *
     * @return the integer code of the solution status
     * @see Status
     */
    public int getStatus() {
        if (noLPX()) {
            return LPErrorCodes.NO_LP;
        }
        return native_get_status(id);
    }

    // -------------------------------------------------------------------------------------------

    /**
     * Solves the lprpoblem using primal-dual interior point procedure. Interior
     * point algorithm is more efficient than simplex method for large scale
     * problems and especially for very sparse problems. As the solving of a
     * large lp problems may take long the routine displays brief information
     * about each interior point iteration in the following format: nnn: F =
     * fff; rpi = ppp; rdi = ddd; gap = ggg where nnn is an iteration number,
     * fff is the current value of the objective function, ppp is the current
     * relative primal infeasibility, ddd is the current relative dual
     * infeasibility, and ggg is the current primal-dual gap.
     *
     * @return one of the following codes:
     *         <ol>
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_OK}</b> the LP problem
     *         has been succesefully solved.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_FAULT}</b> unable to
     *         start the search because either the problem has no rows/columns,
     *         or the initial basis is invalid, or the initial basis matrix is
     *         singular or ill-conditioned.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_ITLIM}</b> the search
     *         was terminated because the simplex iterations limit has been
     *         exceeded.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_TMLIM}</b> the search
     *         was terminated because the time limit has been exceeded.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_NOFEAS}</b> the problem
     *         has no feasible solution
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_NOCONV}</b> The search
     *         was terminated due to very slow convergence
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_INSTAB}</b> The search
     *         was prematurely terminated due to numerical instability on
     *         solving Newtonial system
     *         <li> <b>{@link LPErrorCodes#NO_LP}</b> if the lp problem hasn't
     *         been created
     *         </ul>
     */
    public int interiorPointSolve() {
        if (noLPX()) {
            return LPErrorCodes.NO_LP;
        }
        return native_interior(id);
    }

    /**
     * Checks wether the appropriate variable is treated as integer-valued one
     * or not.
     *
     * @param colnum The number of the variable to be checked in the lp proble
     *            object
     * @return <code>true</code> or <code></code> depending on wether the
     *         variable is integer-valued or not.
     * @throws WrongLPX if the problem object hasn't been created
     */
    public boolean isIntVar(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        int flag = native_get_col_kind(id, colnum + 1);
        if (flag == VarKind.INT_VAR) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * If the problem object has been created and acquired a MIP status the
     * function will return <code>true</code>; otherwise it will return
     * <code>false</code>
     *
     * @return <code>true</code> if the problem has a MIP status, otherwise
     *         <code>false</code>
     */
    public boolean isMIP() {
        if (noLPX()) {
            return false;
        }
        int flag = native_get_type(id);
        if (flag == LPClass.LPX_MIP) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Notify the solver to treat the variable specified by it's number as
     * integer-valued one
     *
     * @param colnum the number of the variable
     * @throws WrongLPX if the problem object hasn't been created
     * @throws NotAMIPProblem if the problem doesn't have MIP status
     */
    public void makeVarInt(int colnum) throws WrongLPX, NotAMIPProblem {
        ensureColumnExist(colnum);
        if (!isMIP()) {
            throw new NotAMIPProblem();
        }
        native_set_col_kind(id, colnum + 1, VarKind.INT_VAR);
    }

    /**
     * Notify the solver to treat the variable specified by it's number as real
     * one
     *
     * @param colnum the number of the variable
     * @throws WrongLPX if the problem object hasn't been created
     */
    public void makeVarReal(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        native_set_col_kind(id, colnum + 1, VarKind.REAL_VAR);
    }

    /**
     * Assotiated an integer value (mark) with the certain column
     *
     * @param colnum the number of column to be marked
     * @throws WrongLPX If the problem object wasn't created
     */
    public void markColumn(int colnum) throws WrongLPX {
        ensureColumnExist(colnum);
        native_mark_col(id, colnum + 1, 1);
        nbMarkedColumns++;
    }

    /**
     * Assotiated an integer mark with the row
     *
     * @param rownum The number of row to be marked
     * @throws WrongLPX If the problem object wasn't created
     */
    public void markRow(int rownum) throws WrongLPX {
        ensureRowExist(rownum);
        native_mark_row(id, rownum + 1, 1);
        nbMarkedRows++;
    }

    // for working with columns
    // *
    private native void native_add_cols(int lpxid, int num);

    // for working with rows
    // *
    private native void native_add_rows(int lpxid, int num);

    private native void native_clear_mat(int lpxid);

    // for working with current lpx
    private native int native_create_lpx();

    private native void native_del_items(int lpxid);

    private native void native_delete_lpx(int lpxid);

    private native double[] native_get_col_bnds(int lpxid, int colnum);

    private native double native_get_col_coef(int lpxid, int colnum);

    private native double[] native_get_col_info(int lpxid);

    private native double[] native_get_col_info(int lpxid, int rownum);

    private native int native_get_col_kind(int lpxid, int colnum);

    private native String native_get_col_name(int lpxid, int colnum);

    private native int native_get_dual_status(int lpxid);

    private native int native_get_int_parm(int lpxid, int parm);

    private native double[] native_get_ips_col(int lpxid, int colnum);

    private native double native_get_ips_obj(int lpxid);

    private native double[] native_get_ips_row(int lpxid, int rownum);

    private native int native_get_ips_stat(int lpxid);

    private native double[] native_get_mat_col(int lpxid, int colnum);

    private native double[] native_get_mat_row(int lpxid, int rownum);

    private native double native_get_mip_col(int lpxid, int colnum);

    private native double native_get_mip_obj(int lpxid);

    private native double native_get_mip_row(int lpxid, int rownum);

    private native int native_get_mip_stat(int lpxid);

    private native String native_get_name(int lpxid);

    private native int native_get_num_bin(int lpxid);

    private native int native_get_num_cols(int lpxid);

    private native int native_get_num_int(int lpxid);

    private native int native_get_num_nz(int lpxid);

    private native int native_get_num_rows(int lpxid);

    private native double native_get_obj_c0(int lpxid);

    private native int native_get_obj_dir(int lpxid);

    private native double native_get_obj_val(int lpxid);

    private native int native_get_prim_status(int lpxid);

    private native double native_get_real_parm(int lpxid, int parm);

    private native double[] native_get_row_bnds(int lpxid, int rownum);

    private native double native_get_row_coef(int lpxid, int rownum);

    private native double[] native_get_row_info(int lpxid, int rownum);

    private native String native_get_row_name(int lpxid, int rownum);

    // basic solution query routines
    private native int native_get_status(int lpxid);

    // auxilliary statistucal routines
    private native int native_get_total_memory_used(int lpxid);

    private native int native_get_type(int lpxid);

    private native int native_integer(int lpxid);

    // ---------------interior point block--------------------------
    private native int native_interior(int lpxid);

    private native void native_mark_col(int lpxid, int colnum, int mark);

    private native void native_mark_row(int lpxid, int rownum, int mark);

    private native void native_print_ips(int lpxid, String fname);

    private native void native_print_mip(int lpxid, String name);

    private native void native_print_solution(int lpxid, String fname);

    private native void native_read_lp(int lpxid, String name);

    private native void native_read_mps(int lpxid, String name);

    // *

    // control parameters and statistic routines
    private native void native_reset_parm(int lpxid);

    private native void native_set_col_bnds(int lpxid, int colnum, int type, double ubnd, double lbnd);

    private native void native_set_col_coef(int lpxid, int colnum, double coeff);

    private native void native_set_col_kind(int lpxid, int colnum, int type);

    private native void native_set_col_name(int lpxid, int colnum, String name);

    private native void native_set_int_parm(int lpxid, int parm, int val);

    private native void native_set_mat_col(int lpxid, int rownum, int[] locations, double[] values);

    private native void native_set_mat_row(int lpxid, int rownum, int[] locations, double[] values);

    private native void native_set_name(int lpxid, String name);

    private native void native_set_obj_c0(int lpxid, double c0);

    private native void native_set_obj_dir(int lpxid, int type);

    private native void native_set_real_parm(int lpxid, int parm, double val);

    private native void native_set_row_bnds(int lpxid, int rownum, int type, double ubnd, double lbnd);

    private native void native_set_row_coef(int lpxid, int rownum, double coef);

    private native void native_set_row_name(int lpxid, int rownum, String name);

    private native void native_set_type(int lpxid, int type);

    private native int native_simplex_solve(int lpxid);

    private native void native_unmark_all(int lpxid);

    private native void native_write_mps(int lpxid, String name);

    // -----------------------------------------------------------------------------------------------
    private boolean noLPX() {
        return ((id == -1) ? true : false);
    }

    /**
     * Print interior point solution in a human-readable form to the file
     * specified by it's name.
     *
     * @param fname The name of the file the solution is to be printed to.
     */
    public void printIPSolution(String fname) {
        int status = getIPSStatus();
        if (!IPSErrorCodes.isOptimal(status)) {
            try {
                DataOutputStream odstr = new DataOutputStream(new FileOutputStream(fname));
                odstr.writeBytes(Status.translate(status));
            } catch (FileNotFoundException ex) {
                Log.error("attempt to open the file denoted by a specified pathname \"" + fname + "\" has failed");
            } catch (Exception ex) {
                Log.error("printSolution(String) has failed for some reason");
            }
        } else {
            native_print_ips(id, fname);
        }
    }

    /**
     * Outputs the solution of current MIP problem (if any) to the file given by
     * its name
     *
     * @param fname - The name of the file to print the solution to.
     */
    public void printMIPSolution(String fname) {
        int status = getMIPStatus();
        if (!MIPErrorCodes.isFeasible(status)) {
            try {
                DataOutputStream odstr = new DataOutputStream(new FileOutputStream(fname));
                odstr.writeBytes(Status.translate(status));
            } catch (FileNotFoundException ex) {
                Log.error("attempt to open the file denoted by a specified pathname \"" + fname + "\" has failed");
            } catch (Exception ex) {
                Log.error("printSolution(String) has failed for some reason");
            }
        } else {
            native_print_mip(id, fname);
        }
    }

    /**
     * Print solution in a human-readable form to the file specified by it's
     * name.
     *
     * @param fname The name of the file the solution is to be printed to.
     */
    public void printSolution(String fname) {
        int status = getStatus();
        if (!LPErrorCodes.isFeasible(status)) {
            try {
                DataOutputStream odstr = new DataOutputStream(new FileOutputStream(fname));
                odstr.writeBytes(Status.translate(status));
            } catch (FileNotFoundException ex) {
                Log.error("Attempt to open the file denoted by a specified pathname \"" + fname + "\" has failed");
            } catch (Exception ex) {
                Log.error("printSolution(String) has failed for some reason");
            }
        } else {
            native_print_solution(id, fname);
        }
    }

    /**
     * Inquire about the MIP solution status and throws
     * <code>NoSolutionException</code> if the solution was not found due to
     * some error or the function {@link #solveMIP()} hasn't been invoked yet.
     *
     * @throws NoSolutionException if for some reason the optimal solution of
     *             the MIP problem hasn't been found yet.
     * @throws WrongLPX if the problem object hasn't been created.
     */
    public void processMIPSolution() throws WrongLPX, NoSolutionException {
        int status = native_get_mip_stat(id);
        if (!MIPErrorCodes.isFeasible(status)) {
            throw new NoSolutionException(Status.translate(status));
        }
        return;
    }

    /**
     * Rewrites current lp problem object by that reading from CPLEX lp file
     * given by its name
     *
     * @param fname The name of the file to read lp problem from
     */
    public void readLP(String fname) {
        if (id == -1) {
            id = native_create_lpx();
        }
        native_read_lp(id, fname);
        nbRows = native_get_num_rows(id);
        nbCols = native_get_num_cols(id);
        nbMarkedColumns = 0;
        nbMarkedRows = 0;
    }

    /**
     * Rewrites current lp problem object by that reading from mps file given by
     * its name
     *
     * @param fname The name of the file to read lp problem from
     */
    public void readMPS(String fname) {
        if (id == -1) {
            id = native_create_lpx();
        }
        native_read_mps(id, fname);
        nbRows = native_get_num_rows(id);
        nbCols = native_get_num_cols(id);
        nbMarkedColumns = 0;
        nbMarkedRows = 0;
    }

    /**
     * Removes MIP status
     */
    public void removeMIPStatus() {
        if (noLPX()) {
            return;
        }
        native_set_type(id, LPClass.LPX_PURE);
    }

    // control parameters and statistic routines
    /**
     * Reset all the control parameters to their default values
     *
     * @see Param
     */
    public void resetParms() {
        if (!noLPX()) {
            native_reset_parm(id);
        }
    }

    /**
     *
     * @param fname
     */
    public void saveLPtoMPSFormat(String fname) {
        if (noLPX()) {
            try {
                DataOutputStream odstr = new DataOutputStream(new FileOutputStream(fname));
                odstr.writeBytes("LPX hasn't been created");
            } catch (FileNotFoundException ex) {
                Log.error("attempt to open the file denoted by a specified pathname \"" + fname + "\" has failed");
            } catch (Exception ex) {
                Log.error("saveLPtoMPSFormat(String) has failed due to some IO error");
            }
        } else {
            native_write_mps(id, fname);
        }
    }

    /**
     * Sets up (changes) value of the parameter given by the first argument
     *
     * @param parm The parameter to be changed
     * @param val The new value of the given parameter
     * @see Param
     */
    public void setBoolParm(int parm, boolean val) {
        if (noLPX()) {
            return;
        }
        if (!Param.isValidBoolParam(parm)) {
            throw new IllegalArgumentException("SetBoolParm: " + "unsupported arguments: " + "param = " + parm
                    + ", value= " + val);
        }
        native_set_int_parm(id, Param.getParam(parm), (val ? 1 : 0));
    }

    /**
     * Imposes new constraints on a basic variable specified by it's number
     *
     * @param colnum An index of a basic variable
     * @param type The type of the basic variable
     * @param lb The lower bound
     * @param ub The upper bound
     * @throws WrongLPX if the problem object hasn't been created
     * @see VarType
     */
    public void setColBnds(int colnum, int type, double lb, double ub) throws WrongLPX {
        ensureColumnExist(colnum);
        VarType vt = new VarType(type, lb, ub);
        native_set_col_bnds(id, colnum + 1, vt.getType(), vt.getLb(), vt.getUb());
    }

    /**
     * Sets (or changes) the appropriate coefficient of the objective function.
     *
     * @param colnum The number of the variable which factor is to be set
     * @param coef The new value of the coefficient
     * @throws WrongLPX if the problem object hasn't been created
     */
    public void setColCoef(int colnum, double coef) throws WrongLPX {
        ensureColumnExist(colnum);
        native_set_col_coef(id, colnum + 1, coef);
    }

    // for working with columns
    /**
     * Sets the name specified by the second argument for the basic variable
     * given by it's number specified by the first argument.
     *
     * @param colnum The index of an auxilliary variable in the given LP problem
     *            object.
     * @param name The name to be associated with the particular basic variable
     */
    public void setColName(int colnum, String name) {
        if (noLPX()) {
            return;
        }
        if (colnum >= nbCols) {
            throw new java.lang.IndexOutOfBoundsException();
        }
        native_set_col_name(id, colnum + 1, name);
    }

    /**
     * Sets up (changes) value of the parameter given by the first argument
     *
     * @param parm The parameter to be changed
     * @param val The new value of the given parameter
     * @see Param
     */
    public void setIntParm(int parm, int val) {
        if (noLPX()) {
            return;
        }
        if (!Param.isValidIntParam(parm, val)) {
            throw new IllegalArgumentException("SetIntParm: " + "unsupported arguments: " + "param = " + parm
                    + ", value= " + val);
        }
        native_set_int_parm(id, Param.getParam(parm), val);
    }

    /**
     * Sets up or changes the column of the constraint's matrix. The new column
     * is represented by an array "values". The location of a particular value
     * in the array corresponds to the number of the particular constraint in
     * the lp problem object. Note: An array "values" may contain zero elements.
     *
     * @param colnum The number of the column in the constraint matrix.
     * @param values Array of coeffitients
     * @throws WrongLPX if the problem hasn't been created
     */
    public void setMatCol(int colnum, double values[]) throws WrongLPX {
        ensureRowExist(colnum);
        if (values.length != nbCols) {
            throw new IllegalArgumentException("Array's length must be equal to the dimensionality of LP");
        }
        int nz = 0; // nonzero element's counter
        int[] locations = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0) {
                locations[nz] = i;
                nz++;
            }
        }
        if (nz == 0) {
            throw new IllegalArgumentException("Nill array");
        }

        int locs[] = new int[nz + 1];
        double vals[] = new double[nz + 1];
        for (int i = 1; i <= nz; i++) {
            locs[i] = locations[i - 1] + 1;
            vals[i] = values[locations[i - 1]];
        }
        native_set_mat_row(id, colnum + 1, locs, vals);
    }

    /**
     * Sets up or replaces the column of the constraint matrix specified by it's
     * number.
     *
     * @param colnum The number of the column to be changed
     * @param locations An array of type <code>int</code> defining the
     *            locations of nonzero coefficients
     * @param values An array of type double. values[i] is the coefficient
     *            standing at the locations[i] position.
     * @throws WrongLPX if the problem hasn't been created
     */
    public void setMatCol(int colnum, int[] locations, double[] values) throws WrongLPX {
        ensureRowExist(colnum);
        String errStr = "setMatCol(int, int[], double[]): ";
        if (locations.length != values.length) {
            throw new IllegalArgumentException(errStr
                    + "The second and the third arguments must be arrays of the same length");
        }
        if (locations.length > nbCols + 1) {
            throw new IllegalArgumentException(errStr + "Array's length must not be greater then dimensionality of LP");
        }
        for (int i = 0; i < locations.length; i++) {
            if ((locations[i] < 0) || (locations[i] >= nbCols)) {
                throw new IllegalArgumentException(errStr + "locations[" + i + "]=" + locations[i]
                        + " : must be within the range [0.." + (nbRows - 1) + "]");
            }
            locations[i]++;
            if (values[i] == 0) {
                throw new IllegalArgumentException(errStr + "values[" + i + "]=0" + ": can't contain nill elements");
            }
        }
        if (!isAllDiff(locations)) {
            throw new IllegalArgumentException(errStr + " \"locations\" array must not contain identical elements");
        }

        int[] locs = new int[values.length + 1];
        double[] vals = new double[values.length + 1];
        System.arraycopy(locations, 0, locs, 1, values.length);
        System.arraycopy(values, 0, vals, 1, values.length);
        native_set_mat_row(id, colnum + 1, locs, vals);
    }

    /**
     * Sets up or changes the row of the constraint's matrix. The new row is
     * represented by an array "values". The location of a particular value in
     * the array corresponds to the number of the basic variable in the lp
     * problem object. So the raw is just a scalar product of "x" (vector of
     * variables) and "values". An array "values" may contain zero elements.
     *
     * @param rownum The number of the row
     * @param values Array of coeffitients
     * @throws WrongLPX if the problem hasn't been created
     */
    public void setMatRow(int rownum, double values[]) throws WrongLPX {
        ensureRowExist(rownum);
        if (values.length != nbCols) {
            throw new IllegalArgumentException("Array's length must be equal to the dimensionality of LP");
        }
        int nz = 0;
        int[] locations = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0) {
                locations[nz] = i;
                nz++;
            }
        }
        if (nz == 0) {
            throw new IllegalArgumentException("Nill array");
        }

        int locs[] = new int[nz + 1];
        double vals[] = new double[nz + 1];
        for (int i = 1; i <= nz; i++) {
            locs[i] = locations[i - 1] + 1;
            vals[i] = values[locations[i - 1]];
        }
        native_set_mat_row(id, rownum + 1, locs, vals);
    }

    /**
     * Sets up or replaces the row specified by it's number.
     *
     * @param rownum The number of the row to be changed
     * @param locations An array of type <code>int</code> defining the
     *            locations of nonzero coefficients
     * @param values An array of type double. values[i] is the coefficient at
     *            the locations[i] position
     * @throws WrongLPX if the problem hasn't been created
     */
    public void setMatRow(int rownum, int[] locations, double[] values) throws WrongLPX

    {

        ensureRowExist(rownum);
        String errStr = "setMatRow(int,int[],double[]) :";
        if (locations.length != values.length) {
            throw new IllegalArgumentException(errStr
                    + "The second and the third arguments must be arrays of the same length");
        }
        if (locations.length > nbCols + 1) {
            throw new IllegalArgumentException(errStr + "Array's length must not be greater then dimensionality of LP");
        }
        for (int i = 0; i < locations.length; i++) {
            if ((locations[i] < 0) || (locations[i] >= nbCols)) {
                throw new IllegalArgumentException(errStr + "locations[" + i + "]=" + locations[i]
                        + " : must be within the range [0.." + (nbCols - 1) + "]");
            }
            if (values[i] == 0) {
                throw new IllegalArgumentException(errStr + "values[" + i + "]=0" + ": can't contain nill elements");
            }
            locations[i]++;
        }
        if (!isAllDiff(locations)) {
            throw new IllegalArgumentException(errStr + "\"locations\" array must not contain identical elements");
        }

        int[] locs = new int[values.length + 1];
        double[] vals = new double[values.length + 1];
        System.arraycopy(locations, 0, locs, 1, values.length);
        System.arraycopy(values, 0, vals, 1, values.length);
        native_set_mat_row(id, rownum + 1, locs, vals);
    }

    // MIP part
    /**
     * Ascribes to the problem MIP status
     *
     * @throws WrongLPX if the problem object hasn't been created
     */
    public void setMIPStatus() throws WrongLPX {
        ensureLPXCreated();
        native_set_type(id, LPClass.LPX_MIP);
    }

    /**
     * Assignes a symbolic name to the problem object
     *
     * @param name The name to be assigned
     * @throws WrongLPX if the problem object doesn't exist
     */
    public void setName(String name) throws WrongLPX {
        ensureLPXCreated();
        native_set_name(id, name);
    }

    /**
     * Sets up (changes) the free term of the objective function
     *
     * @param val A new value of the free term
     * @throws WrongLPX if the problem object doesn't exist
     */
    public void setObjConst(double val) throws WrongLPX {
        ensureLPXCreated();
        native_set_obj_c0(id, val);
    }

    /**
     * Sets up (chnges) the optimization direction
     *
     * @param dirType If <code>dirType == </code> {@link Direction#MAX} the
     *            solver will try to maximize the objective function, otherwise
     *            it will try to minimize it
     * @throws WrongLPX If the lp problem object doesn't exist.
     */
    public void setObjDir(int dirType) throws WrongLPX {
        ensureLPXCreated();
        if (dirType == Direction.MAX) {
            native_set_obj_dir(id, dirType);
        } else {
            native_set_obj_dir(id, Direction.MIN);
        }
    }

    /**
     * Sets up (changes) value of the double-valued parameter given by the first
     * argument
     *
     * @param parm The parameter to be changed
     * @param val The new value of the given parameter
     * @see Param
     */
    public void setRealParm(int parm, double val) {
        if (noLPX()) {
            return;
        }
        if (!Param.isValidRealParam(parm, val)) {
            throw new IllegalArgumentException("SetRealParm: " + "unsupported arguments: " + "param = " + parm
                    + ", value= " + val);
        }
        native_set_real_parm(id, Param.getParam(parm), val);
    }

    /**
     * Sets up or changes type and bounds of the row specified by its number
     *
     * @param rownum the number of the row to be changed
     * @param type The type of the raw
     * @param lb The lower bound
     * @param ub The upper bound
     * @throws WrongLPX if the problem object hasn't been created
     * @see VarType
     */
    public void setRowBnds(int rownum, int type, double lb, double ub) throws WrongLPX {
        ensureRowExist(rownum);
        VarType vartype = new VarType(type, lb, ub);
        native_set_row_bnds(id, rownum + 1, vartype.getType(), vartype.getLb(), vartype.getUb());
    }

    /**
     * Sets (or changes) the appropriate coefficient of the objective function.
     *
     * @param rownum The number of an auxiliary variable which factor is to be
     *            set
     * @param coef The new value of the coefficient
     * @throws WrongLPX if the problem object hasn't been created
     */
    public void setRowCoef(int rownum, double coef) throws WrongLPX {
        ensureRowExist(rownum);
        native_set_col_coef(id, rownum + 1, coef);
    }

    // for working with rows
    /**
     * Sets the name given by the second argument for the raw given by the first
     * argument
     *
     * @param rownum The namber of row
     * @param name the name to be associated with the particular row
     */
    public void setRowName(int rownum, String name) {
        if (noLPX()) {
            return;
        }
        if (rownum >= nbRows) {
            throw new java.lang.IndexOutOfBoundsException();
        }
        native_set_row_name(id, rownum + 1, name);

    }

    /**
     * Solves the lp problem. The routine obtains the data from the problem
     * object and stores all the results back in the problem object. The solver
     * based on the two phase revised simplex method. Generally, the simplex
     * method does the following:
     * <ul>
     * <li> searching for (primal) feasible solution (phase 1)
     * <li> searching for optimal basic solution (phase 2)
     * </ol>
     *
     * @return one of the following codes:
     *         <ol>
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_OK}</b> the LP problem
     *         has been succesefully solved.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_FAULT}</b> unable to
     *         start the search because either the problem has no rows/columns,
     *         or the initial basis is invalid, or the initial basis matrix is
     *         singular or ill-conditioned.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_OBJLL}</b> the search
     *         was terminated because the objective function being maximized has
     *         reached its lower limit and continious decreasing.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_OBJUL}</b> the search
     *         was terminated because the objective function being minimized has
     *         reached its upper limin and continious increasing.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_ITLIM}</b> the search
     *         was terminated because the simplex iterations limit has been
     *         exceeded.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_TMLIM}</b> the search
     *         was terminated because the time limit has been exceeded.
     *         <li> <b>{@link SolutionErrorCodes#LPX_E_SING}</b> the search
     *         was due to the solver failure
     *         </ul>
     */
    public int simplexSolve() {
        if (noLPX()) {
            return LPErrorCodes.NO_LP;
        }
        return native_simplex_solve(id);
    }

    /**
     * Inquire about the solution status and throws
     * <code>NoSolutionException</code> if the solution was not found due to
     * some error or the function {@link #simplexSolve()} hasn't been invoked
     * yet.
     *
     * @throws NoSolutionException If there is no feasible solution
     */
    public void solutionProcess() throws NoSolutionException {
        int status = getStatus();
        if (LPErrorCodes.isFeasible(status)) {
            return;
        }
        throw new NoSolutionException(Status.translate(status));
    }

    /**
     * Tries to find an optimal solution of the MIP problem using GLPK solver
     * which is based on branch and bound method. As the solution process for
     * the MIP problem might take a lot of time the GLPK solver reports some
     * information about the best known solution, which is sent to the standart
     * output. This information has the following format:
     *
     * <code>+nnn: mip = xxx; lp = yyy (mmm, nnn)</code>
     *
     * where 'nnn' is the simplex iteration number, 'xxx' is a value of the
     * objective function for for the best known solution (if no integer
     * feasible solution has been found yet, 'xxx' is the text: "not found
     * yet"), 'yyy' is an optimal value of the objective function for LP
     * relaxation (this value is not changed during all the search), 'mmm' and
     * 'nnn' are some specific GLPK's information.
     *
     * @return one of the follows exit codes:
     *         <ul>
     *         <li> {@link SolutionErrorCodes#LPX_E_OK} - the problem was
     *         successfully solved
     *         <li> {@link SolutionErrorCodes#LPX_E_FAULT} - unable to start the
     *         search because either: the problem of not MIP class or the
     *         problem object doesn't contain optimal solution for LP or some
     *         integer variables has noninteger lower or upper boound
     *         <li> {@link SolutionErrorCodes#LPX_E_ITLIM} - the search was
     *         terminated because the simplex iterations name has been exceeded.
     *         <li> {@link SolutionErrorCodes#LPX_E_TMLIM} - the search was
     *         terminated because the time limit has been exceeded
     *         <li> {@link SolutionErrorCodes#LPX_E_SING} - the search was
     *         terminated due to the solver failure
     *         </ul>
     * @see Status#translate(int)
     */
    public int solveMIP() {
        if (noLPX()) {
            return LPErrorCodes.NO_LP;
        }
        if (!isMIP()) {
            return MIPErrorCodes.NOT_A_MIP;
        }
        int status = native_get_status(id);
        if (status == LPErrorCodes.LPX_UNDEF) {
            status = simplexSolve();
            if (!SolutionErrorCodes.isSuccessful(status)) {
                return SolutionErrorCodes.LPX_E_FAULT;
            }
            return native_integer(id);
        }
        if (!LPErrorCodes.isOptimal(status)) {
            return SolutionErrorCodes.LPX_E_FAULT;
        }
        return native_integer(id);
    }

    /**
     * Clears all marks
     *
     * @throws WrongLPX if the problem object wasn't created
     */
    public void unmarkAll() throws WrongLPX {
        ensureLPXCreated();
        if ((nbMarkedColumns != 0) || (nbMarkedRows != 0)) {
            native_unmark_all(id);
            nbMarkedRows = 0;
            nbMarkedColumns = 0;
        }
    }
}

package org.openl.ie.exigensimplex.glpkimpl;

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
import org.openl.ie.exigensimplex.SearchDirection;
import org.openl.ie.exigensimplex.VarBounds;
import org.openl.ie.exigensimplex.VariableType;
import org.openl.ie.simplex.Direction;
import org.openl.ie.simplex.IPSErrorCodes;
import org.openl.ie.simplex.LPErrorCodes;
import org.openl.ie.simplex.LPX;
import org.openl.ie.simplex.MIPErrorCodes;
import org.openl.ie.simplex.NotAMIPProblem;
import org.openl.ie.simplex.SolutionErrorCodes;
import org.openl.ie.simplex.Status;
import org.openl.ie.simplex.VarType;
import org.openl.ie.simplex.WrongLPX;

public class GLPKLPProblem implements LPProblem {
    private int _lpAlgorithm = Algorithm.TWO_PHASED_REVISED_SIMPLEX;
    private int _mipAlgorithm = MIPAlgorithm.BRANCH_AND_BOUNDS;
    private int _successfullyUsedLPAlgorithm = -1;
    private int _successfullyUsedMIPAlgorithm = -1;
    private int _lastErrorWhileSolvingLP = 0;
    private int _lastErrorWhileSolvingMIP = 0;

    // private boolean _isMIP = false;

    // private int _nbMIPVars;
    // private int _nbBoolVars;

    private org.openl.ie.simplex.LPX _lp = new LPX();

    static public String errorToString(int errorCode) {
        return Status.translate(errorCode);
    }

    public GLPKLPProblem() {
        _lp.createLPX();
    }

    public void addColumns(int num) {
        try {
            _lp.addColumns(num);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void addColumns(int num, String[] names) {
        if (names.length != num) {
            throw new IllegalArgumentException("addRows(int, String[]) : wrong size of String[] array");
        }
        try {
            int oldnum = _lp.getNumCols();
            _lp.addColumns(num);
            for (int i = 0; i < names.length; i++) {
                _lp.setColName(i + oldnum - 1, names[i]);
            }
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void addColumns(int num, String[] names, int[] types, double[] ubounds, double[] lbounds) {
        try {
            if (!(num == names.length && num == ubounds.length && num == lbounds.length)) {
                throw new IllegalArgumentException("addRows(int, String[], double[], double[])");
            }
            int oldnum = _lp.getNumCols();
            _lp.addColumns(num);
            for (int i = 0; i < names.length; i++) {
                _lp.setColName(i + oldnum - 1, names[i]);
                VarType type = new VarType(parseBoundsType(types[i]), lbounds[i], ubounds[i]);
                _lp.setColBnds(i + oldnum - 1, type.getType(), type.getLb(), type.getUb());
            }
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void addRows(int num) {
        try {
            _lp.addRows(num);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void addRows(int num, String[] names) {
        try {
            if (names.length != num) {
                throw new IllegalArgumentException("addRows(int, String[]) : wrong size of String[] array");
            }
            int oldnum = _lp.getNumRows();
            _lp.addRows(num);
            for (int i = 0; i < names.length; i++) {
                _lp.setRowName(i + oldnum - 1, names[i]);
            }
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void addRows(int num, String[] names, int[] types, double[] lbounds, double[] ubounds) {
        try {
            if (!(num == names.length && num == ubounds.length && num == lbounds.length)) {
                throw new IllegalArgumentException("addRows(int, String[], double[], double[])");
            }
            int oldnum = _lp.getNumRows();
            _lp.addRows(num);
            for (int i = 0; i < names.length; i++) {
                _lp.setRowName(i + oldnum - 1, names[i]);
                VarType type = new VarType(parseBoundsType(types[i]), lbounds[i], ubounds[i]);
                _lp.setRowBnds(i + oldnum - 1, type.getType(), type.getLb(), type.getUb());
            }
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // MIP routines
    public void ascribeMIPStatus() {
        try {
            _lp.setMIPStatus();
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    private boolean checkSymbolicNames(String name) {
        return true;
    }

    public void deleteCurrentLP() {
        _lp.deleteLPX();
        _lp.createLPX();
    }

    private void ensureFeasibleSolutionExists() throws org.openl.ie.exigensimplex.NoSolutionException {
        int status = _lp.getStatus();
        if (!LPErrorCodes.isFeasible(status)) {
            throw new org.openl.ie.exigensimplex.NoSolutionException(status);
        }
    }

    private void ensureMIPFeasibleSolutionExists() throws org.openl.ie.exigensimplex.NoSolutionException {
        int status = _lp.getMIPStatus();
        if (!MIPErrorCodes.isFeasible(status)) {
            throw new org.openl.ie.exigensimplex.NoSolutionException(status);
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // utility routines
    public String errorAsString(int errorCode) {
        return Status.translate(errorCode);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            _lp.deleteLPX();
        } catch (Exception ignored) {
        } finally {
            super.finalize();
        }
    }

    public int getAlgorithm() {
        return _lpAlgorithm;
    }

    public boolean getBoolParam(int paramNum) {
        try {
            return (_lp.getBoolParm(paramNum) != 0);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public VarBounds getColumnBounds(int num) {
        try {
            VarType type = _lp.getColBnds(num);
            return new VarBounds(inverseParseBoundsType(type.getType()), type.getLb(), type.getUb());
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public double getColumnCoeff(int num) {
        try {
            return _lp.getColCoef(num);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public String getColumnName(int num) {
        try {
            return _lp.getColName(num);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error has occured");
        }
    }

    public double getColumnValue(int colnum) throws org.openl.ie.exigensimplex.NoSolutionException {
        try {
            ensureFeasibleSolutionExists();
            return _lp.getBasicInfo(colnum).getPrim();
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public int getIntParam(int paramNum) {
        try {
            return _lp.getIntParm(paramNum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public int getLastLPError() {
        return _lastErrorWhileSolvingLP;
    }

    public int getlastMIPError() {
        return _lastErrorWhileSolvingMIP;
    }

    public double getMatrixCoeff(int i, int j) {
        throw new java.lang.RuntimeException("getMatrixCoeff is not implemented");
    }

    public MatrixRow getMatrixColumn(int num) {
        try {
            return new MatrixRow(_lp.getMatCols(num));
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public MatrixRow getMatrixRow(int num) {
        try {
            return new MatrixRow(_lp.getMatRow(num));
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public int getMIPAlgorithm() {
        return _mipAlgorithm;
    }

    public double getMIPColumnValue(int colnum) throws org.openl.ie.exigensimplex.NoSolutionException {
        try {
            int status = _lp.getMIPStatus();
            if (!MIPErrorCodes.isFeasible(status)) {
                throw new org.openl.ie.exigensimplex.NoSolutionException(status);
            }
            return _lp.getMIPBasic(colnum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        } catch (NotAMIPProblem ex) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public double getMIPObjVal() throws org.openl.ie.exigensimplex.NoSolutionException {
        try {
            return _lp.getMIPObjVal();
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        } catch (org.openl.ie.simplex.NoSolutionException ex) {
            throw new org.openl.ie.exigensimplex.NoSolutionException(_lp.getMIPStatus());
        }
    }

    public double getMIPRowValue(int rownum) throws org.openl.ie.exigensimplex.NoSolutionException {
        try {
            int status = _lp.getMIPStatus();
            if (!MIPErrorCodes.isFeasible(status)) {
                throw new org.openl.ie.exigensimplex.NoSolutionException(status);
            }
            return _lp.getMIPAux(rownum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        } catch (NotAMIPProblem ex) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public int getMIPStatus() {
        return _lp.getMIPStatus();
    }

    public int getNumBooleanColumns() {
        return _lp.getNumBoolVars();
    }

    public int getNumColumns() {
        return _lp.getNumCols();
    }

    public int getNumIntegerColumns() {
        return _lp.getNumIntVars();
    }

    public int getNumNonZero() {
        return _lp.getNumNz();
    }

    public int getNumRows() {
        return _lp.getNumRows();
    }

    public double getObjConst() {
        try {
            return _lp.getObjConst();
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public double getObjValue() throws org.openl.ie.exigensimplex.NoSolutionException {
        try {
            return _lp.getObjVal();
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        } catch (org.openl.ie.simplex.NoSolutionException ex) {
            int status = _lp.getStatus();
            throw new org.openl.ie.exigensimplex.NoSolutionException(status, Status.translate(status));
        }
    }

    public String getProblemName() {
        return _lp.getName();
    }

    public int getProblemStatus() {
        if (_lp.getIPSStatus() == IPSErrorCodes.LPX_T_UNDEF) {
            return _lp.getStatus();
        }
        return _lp.getIPSStatus();
    }

    public double getRealParam(int paramNum) {
        try {
            return _lp.getRealParm(paramNum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public VarBounds getRowBounds(int num) {
        try {
            VarType type = _lp.getRowBnds(num);
            return new VarBounds(inverseParseBoundsType(type.getType()), type.getLb(), type.getUb());
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public double getRowCoeff(int num) {
        try {
            return _lp.getRowCoef(num);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public String getRowName(int num) {
        try {
            return _lp.getRowName(num);
        } catch (WrongLPX ex) {
            throw new RuntimeException("internal error has occured");
        }
    }

    public double getRowValue(int rownum) throws org.openl.ie.exigensimplex.NoSolutionException {
        try {
            ensureFeasibleSolutionExists();
            return _lp.getAuxInfo(rownum).getPrim();
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    // solution query routines
    // -----------------------------------------------------------------------------------------------

    public String getStringParam(int paramNum) {
        return "";
    }

    private boolean hasFeasibleMIPSolution() {
        return (MIPErrorCodes.isFeasible(_lp.getMIPStatus()));
    }

    private boolean hasFeasibleSolution() {
        return (LPErrorCodes.isFeasible(_lp.getStatus()));
    }

    private int inverseParseBoundsType(int type) {
        if (type == VarType.LPX_UP) {
            return VariableType.BOUNDED_ABOVE;
        }
        if (type == VarType.LPX_LO) {
            return VariableType.BOUNDED_BELOW;
        }
        if (type == VarType.LPX_DB) {
            return VariableType.DOUBLE_BOUNDED;
        }
        if (type == VarType.LPX_FX) {
            return VariableType.FIXED_VARIABLE;
        }
        if (type == VarType.LPX_FR) {
            return VariableType.FREE_VARIABLE;
        }
        return VariableType.DOUBLE_BOUNDED;
    }

    public boolean isColumnBoolean(int colnum) {
        try {
            VarType vt = _lp.getColBnds(colnum);
            return (vt.getLb() == 0) && (vt.getUb() == 1) && (_lp.isIntVar(colnum));
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public boolean isColumnInteger(int colnum) {
        try {
            return _lp.isIntVar(colnum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public boolean isFeasibleLPSolutionFound() {
        return LPErrorCodes.isFeasible(_lp.getStatus()) || IPSErrorCodes.isOptimal(_lp.getIPSStatus());
    }

    public boolean isFeasibleMIPSolutionFound() {
        return MIPErrorCodes.isFeasible(_lp.getMIPStatus());
    }

    public boolean isMIP() {
        return _lp.isMIP();
    }

    public boolean isOptimalLPSolutionFound() {
        return LPErrorCodes.isOptimal(_lp.getStatus()) || IPSErrorCodes.isOptimal(_lp.getIPSStatus());
    }

    public boolean isOptimalMIPSolutionFound() {
        return MIPErrorCodes.isOptimal(_lp.getMIPStatus());
    }

    public boolean isRowBoolean(int rownum) {
        return false;
    }

    public boolean isRowInteger(int rownum) {
        return false;
    }

    public void markColumnAsBoolVar(int colnum) {
        try {
            if (!_lp.isMIP()) {
                ascribeMIPStatus();
            }
            _lp.makeVarInt(colnum);
            _lp.setColBnds(colnum, parseBoundsType(VariableType.DOUBLE_BOUNDED), 0, 1);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        } catch (NotAMIPProblem ex) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void markColumnAsFloatVar(int colnum) {
        try {
            _lp.makeVarReal(colnum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void markColumnAsIntVar(int colnum) {
        try {
            if (!_lp.isMIP()) {
                ascribeMIPStatus();
            }
            _lp.makeVarInt(colnum);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        } catch (NotAMIPProblem ex) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    private int parseBoundsType(int type) {
        switch (type) {
            case VariableType.BOUNDED_ABOVE:
                return VarType.LPX_UP;
            case VariableType.BOUNDED_BELOW:
                return VarType.LPX_LO;
            case VariableType.DOUBLE_BOUNDED:
                return VarType.LPX_DB;
            case VariableType.FIXED_VARIABLE:
                return VarType.LPX_FX;
            case VariableType.FREE_VARIABLE:
                return VarType.LPX_FR;
            default:
                return VarType.LPX_DB;
        }
    }

    public void printMIPSolutionToFile(String filename) {
        _lp.printMIPSolution(filename);
    }

    public void printSolutionToFile(String filename) {
        if (_successfullyUsedLPAlgorithm == Algorithm.INTERIOR_POINT) {
            _lp.printIPSolution(filename);
            return;
        }
        if (_successfullyUsedLPAlgorithm == Algorithm.TWO_PHASED_REVISED_SIMPLEX) {
            _lp.printSolution(filename);
            return;
        }
        if (_successfullyUsedLPAlgorithm == -1) {
            _lp.printSolution(filename);
            return;
        }
    }

    public void readFromFile(String filename) {

    }

    public void readLP(String filename) {
        _lp.readLP(filename);
    }

    public void readMPS(String filename) {
        _lp.readMPS(filename);
    }

    public void saveLPToMPS(String filename) {
        _lp.saveLPtoMPSFormat(filename);
    }

    public void setAlgorithm(int algorithm) {
        if (!Algorithm.isAvailableAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        _lpAlgorithm = algorithm;
    }

    public void setBoolParam(int paramNum, boolean value) {
        _lp.setBoolParm(paramNum, value);
    }

    public void setColumnBounds(int num, int type, double lbound, double ubound) {
        try {
            _lp.setColBnds(num, parseBoundsType(type), lbound, ubound);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setColumnCoeff(int num, double value) {
        try {
            _lp.setColCoef(num, value);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setColumnName(int num, String name) {
        if (!checkSymbolicNames(name)) {
            throw new IllegalArgumentException("GLPKLP.setRowName: incorrect symbolic name");
        }
        _lp.setColName(num, name);
    }

    public void setIntParam(int paramNum, int value) {
        _lp.setIntParm(paramNum, value);
    }

    public void setMatrixCoeff(int i, int j, double val) {
        throw new java.lang.RuntimeException("setMatrixCoeff is not implemented");
    }

    public void setMatrixColumn(int num, int[] locations, double[] values) {
        try {
            _lp.setMatCol(num, locations, values);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setMatrixColumn(int num, MatrixRow matcol) {
        try {
            _lp.setMatCol(num, matcol.getLocations(), matcol.getValues());
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setMatrixRow(int num, int[] locations, double[] values) {
        try {
            _lp.setMatRow(num, locations, values);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setMatrixRow(int num, MatrixRow matrow) {
        try {
            _lp.setMatRow(num, matrow.getLocations(), matrow.getValues());
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setMIPAlgorithm(int algorithm) {
        if (!MIPAlgorithm.isAvailableAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        _mipAlgorithm = algorithm;
    }

    public void setObjConst(double value) {
        try {
            _lp.setObjConst(value);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    private void setOptimizationDirection(int direction) {
        try {
            if (direction == SearchDirection.MAXIMIZATION) {
                if (!(_lp.getObjDir() == Direction.MAX)) {
                    _lp.setObjDir(Direction.MAX);
                }
            } else {
                if (!(_lp.getObjDir() == Direction.MIN)) {
                    _lp.setObjDir(Direction.MIN);
                }
            }
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setProblemName(String name) {
        if (!checkSymbolicNames(name)) {
            throw new IllegalArgumentException("GLPKLP.setRowName: incorrect symbolic name");
        }
        try {
            _lp.setName(name);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setRealParam(int paramNum, double value) {
        _lp.setRealParm(paramNum, value);
    }

    public void setRowBounds(int num, int type, double lbound, double ubound) {
        try {
            _lp.setRowBnds(num, parseBoundsType(type), lbound, ubound);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setRowCoeff(int num, double val) {
        try {
            _lp.setRowCoef(num, val);
        } catch (WrongLPX wrglpx) {
            throw new java.lang.RuntimeException("Internal error");
        }
    }

    public void setRowName(int num, String name) {
        if (!checkSymbolicNames(name)) {
            throw new IllegalArgumentException("GLPKLP.setRowName: incorrect symbolic name");
        }
        _lp.setRowName(num, name);
    }

    public void setStringParam(int paramNum, String value) {
    }

    public int solveLP() {
        int solution, status, success;
        switch (_lpAlgorithm) {
            case Algorithm.TWO_PHASED_REVISED_SIMPLEX: {
                solution = _lp.simplexSolve();
                status = _lp.getStatus();
                success = LPErrorCodes.isFeasible(status) ? 0 : status;
                break;
            }
            case Algorithm.INTERIOR_POINT: {
                solution = _lp.interiorPointSolve();
                status = _lp.getIPSStatus();
                success = IPSErrorCodes.isOptimal(status) ? 0 : status;
                break;
            }
            default: {
                solution = 0;
                status = 0;
                success = 0;
                break;
            }
        }

        if (!SolutionErrorCodes.isSuccessful(solution)) {
            _lastErrorWhileSolvingLP = solution;
        } else {
            _lastErrorWhileSolvingLP = 0;
        }

        if (success == 0) {
            _successfullyUsedLPAlgorithm = _lpAlgorithm;
        } else {
            _successfullyUsedLPAlgorithm = -1;
        }

        return success;
    }

    public int solveLP(int direction) {
        setOptimizationDirection(direction);
        return solveLP();
    }

    public int solveMIP() {

        if (!isMIP()) {
            return MIPErrorCodes.NOT_A_MIP;
        }
        int solution, status, success;
        switch (_mipAlgorithm) {
            case MIPAlgorithm.BRANCH_AND_BOUNDS: {
                solution = _lp.solveMIP();
                status = _lp.getMIPStatus();
                success = MIPErrorCodes.isFeasible(status) ? 0 : 1;
                break;
            }
            default: {
                status = 0;
                solution = 0;
                success = 0;
            }
        }

        if (success == 0) {
            _successfullyUsedMIPAlgorithm = _mipAlgorithm;
        } else {
            _successfullyUsedLPAlgorithm = -1;
        }

        if (!SolutionErrorCodes.isSuccessful(solution)) {
            _lastErrorWhileSolvingMIP = solution;
        } else {
            _lastErrorWhileSolvingMIP = 0;
        }

        return success;
    }

    public int solveMIP(int direction) {
        setOptimizationDirection(direction);
        return solveMIP();
    }

}
package org.openl.ie.exigensimplex;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
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

public interface LPProblem {

    /**
     * Adds specified amount of auxilliary variables to the problem
     *
     * @param num The number of variables to be added
     */
    public void addColumns(int num);

    /**
     * Adds specified amount of auxilliary variables to the problem and assignes
     * to each of them a particular name.
     *
     * @param num The number of variables to be added
     * @param names Array of names for the future variable
     */
    public void addColumns(int num, String[] names);

    /**
     * Adds specified amount of auxilliary variables to the problem, assignes
     * names and also defines bounds for them
     *
     * @param num The number of variables to be added
     * @param names Array of names for the future variables
     * @param types Defines type of bounds
     * @param lbounds Array of lower bounds
     * @param ubounds Array of upper bounds
     * @see VariableType
     */
    public void addColumns(int num, String[] names, int[] types, double[] lbounds, double[] ubounds);

    /**
     * Adds specified amount of auxilliary variables to the problem
     *
     * @param num The number of variables to be added
     */
    public void addRows(int num);

    /**
     * Adds specified amount of auxilliary variables to the problem and assignes
     * to each of them a particular name.
     *
     * @param num The number of variables to be added
     * @param names Array of names for the future variable
     */
    public void addRows(int num, String[] names);

    /**
     * Adds specified amount of auxilliary variables to the problem, assignes
     * names and also defines bounds for them
     *
     * @param num The number of variables to be added
     * @param names Array of names for the future variables
     * @param types Defines type of bounds
     * @param lbounds Array of lower bounds
     * @param ubounds Array of upper bounds
     * @see VariableType
     */
    public void addRows(int num, String[] names, int[] types, double[] lbounds, double[] ubounds);

    // ----------------------------------------------------------------------------------------------------
    // MIP routines
    /**
     * Tells the solver to treat the problem as MIP one
     */
    public void ascribeMIPStatus();

    /**
     * Removes current lp, frees memory and creates a new empty one
     */
    public void deleteCurrentLP();

    // ----------------------------------------------------------------------------------------------------
    // utility routines
    /**
     * Gives a brief description of an error specified by it's integer code
     *
     * @param errorCode An integer code of an error
     */
    public String errorAsString(int errorCode);

    /**
     * @return integer code of a currently chosen algorithm for solving lp
     *         problems
     */
    public int getAlgorithm();

    /**
     * Obtains current value for a specified boolean-valued control parameter
     *
     * @param paramNum An integer code of a control parameter
     * @return current value of a parameter specified
     */
    public boolean getBoolParam(int paramNum);

    /**
     * Gets the constraints imposed on a particular structural variable
     *
     * @param num The ordinal number of the column
     * @return An object of type {@link VarBounds} containing information about
     *         type of bounds for the given variable, it's lower and upper
     *         bound.
     */
    public VarBounds getColumnBounds(int num);

    /**
     * Gets an objective coefficient of a specified row
     *
     * @param num The ordinal number of a row
     * @return The value of the objective coefficient
     */
    public double getColumnCoeff(int num);

    /**
     * Returns a symbolic name of the given column if any, otherwise returns
     * <code>null</code>
     *
     * @param num The ordinal number of the column
     * @return Symbolic name of a column
     */
    public String getColumnName(int num);

    /**
     * Gets a value of a structural variable specified by it's index
     *
     * @param colnum Index of a row in a given lp problem object
     * @return A value of a row specified
     * @throws NoSolutionException if no feasible solution wasn't found and
     *             particularly if the problem hasn't been solved yet.
     */
    public double getColumnValue(int colnum) throws NoSolutionException;

    /**
     * Obtains current value for a specified integer-valued control parameter
     *
     * @param paramNum An integer code of a control parameter
     * @return current value of a parameter specified
     */
    public int getIntParam(int paramNum);

    /**
     * @return an integer code of an error has occured during solving of an lp
     *         problem
     */
    public int getLastLPError();

    /**
     *
     * @return an integer code of an error has occured during solving of an mip
     *         problem
     */
    public int getlastMIPError();

    /**
     * Obtains current value of an entry of a constraint matrix specified by
     * it's row and column indices
     *
     * @param i The row index of an entry
     * @param j The column index of an entry
     * @return the value of an entry
     */
    public double getMatrixCoeff(int i, int j);

    /**
     * Gets sparse-array-like column's representation (representation based on
     * two arrays: array of indices and array of values).
     *
     * @param num The ordinal number of a column in the constraint matrix
     * @return An object of type <code>MatrixRow</code> containing information
     *         about the column specified.
     * @see MatrixRow
     */
    public MatrixRow getMatrixColumn(int num);

    /**
     * Gets sparse-array-like row's representation (representation based on two
     * arrays: array of indices and array of values).
     *
     * @param num The ordinal number of a row in the constraint matrix
     * @return An object of type <code>MatrixRow</code> containing information
     *         about the row specified.
     * @see MatrixRow
     */
    public MatrixRow getMatrixRow(int num);

    /**
     * @return currently chosen algorithm
     */
    public int getMIPAlgorithm();

    /**
     * Gets an activity of a specified column of a given MIP problem object
     * found by the solver
     *
     * @param colnum An index of a column
     * @return current value of a column that was assigned to it by a solver
     * @throws NoSolutionException if no feasible solution was found and
     *             particularly if the problem hasn't been solved yet.
     */
    public double getMIPColumnValue(int colnum) throws NoSolutionException;

    /**
     * @return an objective function value for MIP problem
     * @throws NoSolutionException if the MIP problem doesn't have any solution
     *             or hasn't been solved yet
     */
    public double getMIPObjVal() throws NoSolutionException;

    /**
     * Gets an activity of a specified row of a given MIP problem object found
     * by the solver
     *
     * @param rownum An index of a row
     * @return current value of a row that was assigned to it by a solver
     * @throws NoSolutionException if no feasible solution was found and
     *             particularly if the problem hasn't been solved yet.
     */
    public double getMIPRowValue(int rownum) throws NoSolutionException;

    /**
     * Reports the status of a MIP problem object found by the solver.
     * <em>NOTE: the list of possible states is provided by a specific implementation.</em>
     *
     * @return Integer code of a current status of a given MIP problem object
     */
    public int getMIPStatus();

    /**
     * @return number of boolean-valued variables in a given lp problem object
     */
    public int getNumBooleanColumns();

    /**
     * @return The number of structural variables (columns) having been added to
     *         the problem up to the moment
     */
    public int getNumColumns();

    /**
     * @return number of integer-valued variables in a given lp problem object
     */
    public int getNumIntegerColumns();

    /**
     * @return current number of non-zero elements in the constraint matrix
     */
    public int getNumNonZero();

    /**
     * @return The number of auxilliary variables (rows) having been added to
     *         the problem up to the moment
     */
    public int getNumRows();

    /**
     * @return value of a constant term of an objective function
     */
    public double getObjConst();

    // solution query routines
    // -----------------------------------------------------------------------------------------------
    /**
     * @return an objective function value
     * @throws NoSolutionException if the problem doesn't have any solution or
     *             hasn't been solved yet
     */
    public double getObjValue() throws NoSolutionException;

    /**
     * @return Returns the symbolic name of the problem
     */
    public String getProblemName();

    /**
     *
     * @return An integer code of the current problem status
     *         <em>NOTE: the list of possible states is provided by a specific implementation</em>
     */
    public int getProblemStatus();

    /**
     * Obtains current value for a specified real-valued control parameter
     *
     * @param paramNum An integer code of a control parameter
     * @return current value of a parameter specified
     */
    public double getRealParam(int paramNum);

    /**
     *
     * @param num The ordinal number of the row
     * @return An object of type {@link VarBounds} containing information about
     *         type of bounds for the given variable, it's lower and upper
     *         bound.
     */
    public VarBounds getRowBounds(int num);

    /**
     * Gets an objective coefficient of a specified row
     *
     * @param num The ordinal number of a row
     * @return The value of the objective coefficient
     */
    public double getRowCoeff(int num);

    /**
     * Gets a symbolic name of the given row if any, otherwise returns
     * <code>null</code>
     *
     * @param num The ordinal number of the row
     * @return Symbollic name of a row
     */
    public String getRowName(int num);

    /**
     * Gets a value of an auxilliary variable specified by it's index
     *
     * @param rownum Index of a row in a current lp problem object
     * @return A value of a row specified
     * @throws NoSolutionException if no feasible solution wasn't found and
     *             particularly if the problem hasn't been solved yet.
     */
    public double getRowValue(int rownum) throws NoSolutionException;

    /**
     * Obtains current value for a specified string control parameter
     *
     * @param paramNum An integer code of a control parameter
     * @return current value of a parameter specified
     */
    public String getStringParam(int paramNum);

    /**
     * Checks whether the specified column is a boolean-valued.
     *
     * @param colnum An index of a column in a given lp problem object
     * @return <code>true<code> if it is, <code>false</code> otherwise
     */
    public boolean isColumnBoolean(int colnum);

    /**
     * Checks whether the specified column was marked as integer-valued one
     *
     * @param colnum An index of a column in a given lp problem object
     * @return <code>true<code> if it is, <code>false</code> otherwise
     */
    public boolean isColumnInteger(int colnum);

    /**
     * @return <code>true</code> if a feasible solution of the given lp
     *         problem has been already found by the solver otherwise returns
     *         <code>false</code>
     */
    public boolean isFeasibleLPSolutionFound();

    /**
     * @return <code>true</code> if a feasible solution of the given mip
     *         problem has been already found by the solver otherwise returns
     *         <code>false</code>
     */
    public boolean isFeasibleMIPSolutionFound();

    // public boolean isRowBoolean(int rownum);
    /**
     * @return <code>true</code> if there is at least one integer or boolean
     *         variable in the problem, <code>false</code> otherwise
     */
    public boolean isMIP();

    /**
     * @return <code>true</code> if an optimal solution of the given lp
     *         problem has been already found by the solver otherwise returns
     *         <code>false</code>
     */
    public boolean isOptimalLPSolutionFound();

    /**
     * @return <code>true</code> if a feasible solution of the given mip
     *         problem has been already found by the solver otherwise returns
     *         <code>false</code>
     */
    public boolean isOptimalMIPSolutionFound();

    /**
     * Tells the solver to treat the column specified as an boolean-valued one.
     * The variable automatically becomes an integer-valued double bounded with
     * it's lower bound equals zero and it's upper bound equals 1.
     *
     * @param colnum An index of a column in a given lp problem object.
     */
    public void markColumnAsBoolVar(int colnum);

    /**
     * Tells the solver to treat the column specified as an real-valued one.
     *
     * @param colnum An index of a column in a given lp problem object.
     */
    public void markColumnAsFloatVar(int colnum);

    /**
     * Tells the solver to treat the column specified as an integer-valued one.
     *
     * @param colnum An index of a column in a given lp problem object.
     */
    public void markColumnAsIntVar(int colnum);

    /**
     * Saves a solution of a MIP problem to a file specified by it's name in a
     * human readable format
     *
     * @param filename The symbolic name of a destination file.
     */
    public void printMIPSolutionToFile(String filename);

    /**
     * Saves a solution of an lp problem to a file specified by it's name in a
     * human readable format
     *
     * @param filename The symbolic name of a destination file.
     */
    public void printSolutionToFile(String filename);

    /**
     * Attempts to read the problem data prepared in a specific lp
     * (implementation dependable) format from a file specified by it's name
     *
     * @param filename A name of a file containing the problem's description
     */
    public void readLP(String filename);

    /**
     * Attempts to read the problem data prepared in MPS format from a file
     * specified by it's name
     *
     * @param filename A name of a file containing the problem's description
     */
    public void readMPS(String filename);

    // public boolean isRowInteger(int rownum);

    // public void readFromFile(String filename);
    /**
     * Saves a given lp problem to a file specified by it's name in MPS format
     *
     * @param filename The symbolic name of a destination file.
     */
    public void saveLPToMPS(String filename);

    /**
     * Allows to choose an algorithm for solving current lp problem ("simplex
     * method", for instance)
     * <em>NOTE: A List of available algorithms is provided by a specific implementation.<em>
     * @param algorithm
     */
    public void setAlgorithm(int algorithm);

    /**
     * Sets a new value for a boolean-valued control parameter
     * <em>NOTE: the list of available control parameters is provided by a specific implementation</em>
     *
     * @param paramNum An integer code of a control parameter
     * @param value A new value for a parameter to be set
     */
    public void setBoolParam(int paramNum, boolean value);

    /**
     * Defines (set/change) the type and value of the variable's bounds
     *
     * @param num The ordinal number of the column
     * @param type The parameter to specify one of 5 possible types of bounds
     * @param lbound The value of the lower bound to be assigned to the given
     *            variable
     * @param ubound The value of the upper bound to be assigned to the given
     *            variable
     * @see VariableType
     */
    public void setColumnBounds(int num, int type, double lbound, double ubound);

    /**
     * Sets or changes an objective coefficient of a specified column
     * (structural variable)
     *
     * @param num The ordinal number of a column
     * @param value The new value of the objective coefficient
     */
    public void setColumnCoeff(int num, double value);

    /**
     * Assigns the specified symbolic name to the particular column
     *
     * @param num The ordinal number of a row to be associated with the name
     *            specified by the second argument
     * @param name The name to be assigned.
     */
    public void setColumnName(int num, String name);

    /**
     * Sets a new value for an integer-valued control parameter.
     * <em>NOTE: the list of available control parameters is provided by a specific implementation</em>
     *
     * @param paramNum An integer code of a control parameter
     * @param value A new value to be set
     */
    public void setIntParam(int paramNum, int value);

    /**
     * Sets a new value for an entry of a constraint matrix specified by it's
     * row and column indices.
     *
     * @param i The row index of an entry
     * @param j The column index of an entry
     * @param val A new value to be set
     */
    public void setMatrixCoeff(int i, int j, double val);

    /**
     * Sets or changes the specified row of the constraint matrix
     *
     * @param num The ordinal number of row.
     * @param locations Indices of non zero entries. Indices array must contain
     *            only unique elements.
     * @param values Array of values. It must be of the same size as the indices
     *            array and must not contain zero elements.
     */
    public void setMatrixColumn(int num, int[] locations, double[] values);

    // public void setMatrixCoeff(int row, int column, double value);
    // public double getMatrixCoeff(int row, int column);
    /**
     * Sets or changes the specified column of the constraint matrix. The column
     * is represented in terms of two arrays: one of them containing elements of
     * type <code>int</code> which are actually the indicies of non zero
     * entries and another of type <code>double</code> is a vector of non zero
     * entries itself.
     *
     * @param num The number of column
     * @param matcol An object of type <code>MatrixRow</code> describing the
     *            column to be substituted for a current one
     * @see MatrixRow
     */
    public void setMatrixColumn(int num, MatrixRow matcol);

    /**
     * Sets or changes the specified row of the constraint matrix
     *
     * @param num The ordinal number of row.
     * @param locations Indices of non zero entries. Indices array must contain
     *            only unique elements.
     * @param values Array of values. It must be of the same size as the indices
     *            array and must not contain zero elements.
     */
    public void setMatrixRow(int num, int[] locations, double[] values);

    /**
     * Sets or changes the specified row of the constraint matrix. The row is
     * represented in terms of two arrays: one of them containing elements of
     * type <code>int</code> which are actually the indicies of non zero
     * entries and another of type <code>double</code> is a vector of non zero
     * entries itself.
     *
     * @param num The number of row
     * @param matrow An object of type <code>MatrixRow</code> describing the
     *            row to be substituted for a current one
     * @see MatrixRow
     */
    public void setMatrixRow(int num, MatrixRow matrow);

    /**
     * Allows to choose an algorithm for solving MIP problems ("branch&bounds",
     * for instance)
     * <em>NOTE: the list of available algorithms is provided by a specific implementation</em>
     *
     * @param algorithm
     */
    public void setMIPAlgorithm(int algorithm);

    /**
     * Sets new value of a constant term of an objective function
     *
     * @param value The value to be set
     */
    public void setObjConst(double value);

    /**
     * Assigns the specified symbolic name to the particular problem.
     *
     * @param name The name to be assigned
     */
    public void setProblemName(String name);

    /**
     * Sets a new value for a real-valued control parameter
     * <em>NOTE: the list of available control parameters is provided by a specific implementation</em>
     *
     * @param paramNum An integer code of a control parameter
     * @param value A new value for a parameter to be set
     */
    public void setRealParam(int paramNum, double value);

    /**
     * Defines (set/change) the type and value of the variable's bounds
     *
     * @param num The ordinal number of the row
     * @param type The parameter to specify one of 5 possible types of bounds
     * @param lbound The value of the lower bound to be assigned to the given
     *            variable
     * @param ubound The value of the upper bound to be assigned to the given
     *            variable
     * @see VariableType
     */
    public void setRowBounds(int num, int type, double lbound, double ubound);

    /**
     * Sets or changes an objective coefficient of a specified row (auxilliary
     * variable)
     *
     * @param num The ordinal number of a row
     * @param val The new value of the objective coefficient
     */
    public void setRowCoeff(int num, double val);

    /**
     * Assigns the specified symbolic name to the particular row
     *
     * @param num The ordinal number of a row to be associated with the name
     *            specified by the second argument
     * @param name The name to be assigned.
     */
    public void setRowName(int num, String name);

    /**
     * Sets a new value for a string control parameter List of available control
     * parameters is provided by a specific implementation
     *
     * @param paramNum An integer code of a control parameter
     * @param value A new value for a parameter to be set
     */
    public void setStringParam(int paramNum, String value);

    /**
     * Tries to solve an lp problem using currently chosen algorithm and
     * optimization direction
     * <em>NOTE: One usually uses this function for solving problems having been read from a file.
     * If a problem wasn't read from a file the solver attempts to minimize an objective function </em>
     *
     * @return An integer code of an error occured if any, otherwise it returns
     *         zero
     *         <em>NOTE: The list of possible return values is provided by specific implementation.</em>
     */
    public int solveLP();

    /**
     * Tries to solve an lp problem using currently chosen algorithm and
     * specified optimization direction
     *
     * @param direction Specified an optimization direction
     * @return An integer code of an error occured if any, otherwise it returns
     *         zero
     *         <em>NOTE: The list of possible return values is provided by specific implementation.</em>
     */
    public int solveLP(int direction);

    /**
     * Attempts to solve a given MIP problem using currently chosen algorithm
     * and optimization direction
     *
     * @return Zero if successeful (It's not nessesary for the problem to have
     *         at least integer-feasible solution. It only means that the solver
     *         has managed to finish solution procedure) otherwise it returns an
     *         integer error code.
     *         <em>NOTE: the list of possible error codes is provided by a specific implementation</em>
     */
    public int solveMIP();

    /**
     * Attempts to solve a given MIP problem using currently chosen algorithm
     * and specified optimization direction
     *
     * @return Zero if successeful (It's not nessesary for the problem to have
     *         at least integer-feasible solution. It only means that the solver
     *         has managed to finish solution procedure.) otherwise it returns
     *         an integer code of an error occured.
     *         <em>NOTE: the list of possible error codes is provided by a specific implementation</em>
     */
    public int solveMIP(int direction);
}
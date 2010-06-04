package org.openl.ie.constrainer.lpsolver;

/**
 * <p>Title: LPProblem</p>
 * <p>Description: An interface allowing to create and adjust a linear programming problem
 * for integer variables.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigengroup </p>
 * @author Tseitlin
 * @version 1.0
 */

import java.util.Collection;

import org.openl.ie.constrainer.ConstrainerObject;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.NonLinearExpression;


public interface ConstrainerMIP extends ConstrainerObject {
    /**
     * Adds a given constraint to the problem
     *
     * @param exp An Expression treated as constraint
     * @param isEquality The flag that reveals whether the constraint is
     *            equality (<code>true<\code>)
     * or inequality (<code>false<\code>)
     * @throws {@link org.openl.ie.constrainer.NonLinearExpression} if the expression is not linear
     */
    public void addConstraint(IntBoolExp exp, boolean isEquality) throws NonLinearExpression;

    /**
     * Adds a number of constraints to the problem
     *
     * @param exps A
     *            <code>Collection<code> of {@link org.openl.ie.constrainer.IntBoolExp} to be added
     * to the problem
     * @param isEquality The flag that reveals whether all the constraint are equalities (<code>true<\code>)
     * or inequalities (<code>false<\code>)
     * @throws {@link org.openl.ie.constrainer.NonLinearExpression} if at least one of
     * the expression is not linear
     */
    public void addConstraints(Collection exps, boolean isEquality) throws NonLinearExpression;

    /**
     * Adds a integer variable to the LP problem
     *
     * @param var A variable to be added
     */
    public void addVar(IntVar var);

    /**
     * Returns all the constraints
     *
     * @return All the constraints
     */
    public IntBoolExp[] constraints();

    /**
     * Returns a respective <code>IntBoolExp<\code>
     * @param idx The number of a constraint
     * @return A respective constraint
     */
    public IntBoolExp getConstraint(int idx);

    /**
     * Returns a respective cost function's coefficient
     *
     * @param idx The number of a coefficient.
     * @return The coefficient's value
     */
    public double getCostCoeff(int idx);

    /**
     * Returns a free term of a cost funtion
     *
     * @return Free term of a cost function
     */
    public double getFreeTerm();

    /**
     * Returns an lp representation of a respective constraint
     *
     * @param idx the number of a constraint
     * @return jgsimplex.LPConstraint
     * @throws UnexpectedVariable If the
     *             <code>IntBoolExp<\code> contains an unbounded variable
     * that doesn't present in LPproblem description.
     */
    public LPConstraint getLPConstraint(int idx) throws UnexpectedVariable;

    /**
     * Returns an appropriate variable
     *
     * @param idx The number of the variable
     * @return An appropriate variable
     */
    public IntVar getVar(int idx);

    /**
     * Checks whether the constraint is equality or not
     *
     * @param idx - the number of constraint
     * @return <code>true<\code> if the constraint is equality, <code>false<\code> if it is inequality
     */
    public boolean isEquality(int idx);

    /**
     * Returns an array with the i'th element being an lp representation of a
     * corresponding <code>IntBoolExp<\code>
     * @return An array of jgsimplex.LPConstraints
     * @throws UnexpectedVariable if at least one of the constraints contains a variable being absent
     * in LP problem description.
     */
    public LPConstraint[] lpConstraints() throws UnexpectedVariable;

    /**
     * Returns the number of constraints in the lp problem
     *
     * @return The number of constraints
     */
    public int nbConstraints();

    /**
     * Returns the number of variables in the lp problem
     *
     * @return The number of variables
     */
    public int nbVars();

    /**
     * Removes a respective constraint
     *
     * @param idx The number of a constraint to remove
     */
    public void removeConstraint(int idx);

    /**
     * Removes respective variable from LP problem description
     *
     * @param var A variable to be removed
     */
    public void removeVar(IntVar var);

    /**
     * Sets up a respective coefficient of a cost function
     *
     * @param idx the number of a coefficient to be changed or set up
     * @param coeff the value
     */
    public void setCostCoeff(int idx, double coeff);

    /**
     * Sets up or changes a free term of a cost function
     *
     * @param frTrm A new value of a free term
     */
    public void setFreeTerm(double frTrm);

    /**
     * Checks whether the cost function to be maximized or minimized
     *
     * @return <code>true<\code> if it is to be maximized.
     */
    public boolean toBeMaximized();
}
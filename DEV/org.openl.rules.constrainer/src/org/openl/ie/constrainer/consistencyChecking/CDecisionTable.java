package org.openl.ie.constrainer.consistencyChecking;

/**
 * <p>Title: </p>
 * <p>Description: Interface for gaining access to the data of the Decision Table.</p>
 * <p>
 * Decision Table is represented as a matrix consisted of m columns - variables and n rows - rules.
 * Thus the entry with coordinates [i,j] is a constraint imposed by i'th rule on the j'th variable.
 * The conjunction of all the constraint situated in the same row called "Rule". Formally speaking,
 * rule is an area in the space of states which bounds are due to the constraints being part of it.
 * </p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

public interface CDecisionTable {

    /**
     * @param The number of rule to be returned (actually it is the number of
     *            the appropriate row from the data matrix).
     *
     * @return the conjunction of all the constraints from the i'th row. Note:
     *         it should return not <b><code>null</code></b> for all
     *         decision table rows.
     */
    public IntBoolExp getRule(int i);

    /**
     * @return an array of rules. Rule is the conjuction of all the constraints
     *         located in the i'th row of the constraint matrix. Note: All the
     *         values in the returned array should be not <b><code>null</code></b>.
     */
    public IntBoolExp[] getRules();

    /**
     * @param The number of variable to be returned.
     *
     * @return the i'th variable.
     */
    public IntVar getVar(int i);

    /**
     * @return all variables.
     */
    public IntExpArray getVars();
    
    
    /**
     * 
     * @return true if the direction of the override logic is going in ascending direction.
     * Rule A overrides Rule B if for any input that (A == true) => (B == true). The direction is ascending if
     * the index of A < the index of B. This is the case when the DT has RET column and will return on the first match. 
     * In case if there is no RET column in DT, the direction of the override will change to the opposite
     *   
     */
    
    public boolean isOverrideAscending();
    
}
package sampleproblem.completenessChecking;

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
import org.openl.ie.constrainer.*;

public interface DecisionTable
{
  /**
   * @return an array of Rules. Each rule is a conjuction of all the constraints situated in
   * the i'th row of the data matrix.
   */
  public IntBoolExp[] getRules();
  /**
   * @param i The number of rule to be returned (actually it is the number of the appropriate
   * row from the data matrix).
   * @return the conjunction of all the constraints from the i'th row.
   */
  public IntBoolExp getRule(int i);
  /**
   * @return all variables.
   */
  public IntExpArray getVars();
  /**
   * @param i The number of variable to be returned.
   * @return the i'th variable.
   */
  public IntVar getVar(int i);
  /**
   * @param i the number of rule the constraint belongs to
   * @param j the number of variable the constraint imposed on
   * @return the constraint imposed by the i'th rule on the j'th variable
   */
  public IntBoolExp getEntry(int i,int j);
}
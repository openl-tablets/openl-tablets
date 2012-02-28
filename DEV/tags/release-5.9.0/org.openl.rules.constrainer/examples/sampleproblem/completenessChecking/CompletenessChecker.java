package sampleproblem.completenessChecking;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Vector;
public interface CompletenessChecker
{
 /**
  *  returns Vector of uncovered regions in the space of states
  */

  public Vector check();
}
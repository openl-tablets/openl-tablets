package sampleproblem.completenessChecking;

/**
 * <p>Title: </p>
 * <p>Description: Representation of a point in the space of states not covered by any rule</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import java.util.HashMap;
import org.openl.ie.constrainer.*;

public class Uncovered
{
  HashMap _solution = null;
  Uncovered(IntExpArray array)
  {
    _solution = Utils.IntExpArray2HashMap(array);
  }
  /**
  *@param name The name of the variable.
  *@return value of the variable given by it's name
  */
  public Object getValue(String name){
    return _solution.get(name);
  }
  /**
   *@return an array of names of all variables
   */
  public String[] vars(){
    String[] names = new String[_solution.size()];
    return (String[])_solution.keySet().toArray(names);
  }

  public String toString(){
    return _solution.toString();
  }
}
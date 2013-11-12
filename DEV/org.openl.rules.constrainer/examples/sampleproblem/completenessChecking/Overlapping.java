package sampleproblem.completenessChecking;

/**
 * <p>Title: </p>
 * <p>Description: Auxiliary class using to store information about overlapping rules</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.constrainer.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;

public class Overlapping {
  private Vector _overlapped = null;
  private HashMap _solution = null;

  Overlapping(IntExpArray solution){
    _solution = Utils.IntExpArray2HashMap(solution);
  }
  /**
   *@return an array of numbers of rules overlapping in the decision table
   */
  public int[] getOverlapped(){
    int[] arr = new int[_overlapped.size()];
    Iterator iter = _overlapped.iterator();
    int i = 0;
    while(iter.hasNext())
      arr[i++] = ((Integer)iter.next()).intValue();
    return arr;
  }
  /**
   *@return an amount of rules being satisfied with the solution returned by <code>getSolution()</code>
   *@see #getSolution()
   */
  public int amount(){return _overlapped.size();}

  void addRule(int num){
    if (_overlapped == null)
      _overlapped = new Vector();
    _overlapped.add(new Integer(num));
  }

  /**
   *
   * @return the solution satisfying two or more rules in the form of a
   * HashMap of names of the variables associated with the according values
   */
  public HashMap getSolution(){return _solution;}
  public String toString(){
    String rep = "{ solution: " + _solution + " obeys the following rules: " + _overlapped +"}";
    return rep;
  }
}
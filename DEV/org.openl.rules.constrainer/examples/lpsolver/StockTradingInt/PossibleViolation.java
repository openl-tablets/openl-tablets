package lpsolver.StockTradingInt;
import org.openl.ie.constrainer.*;
import org.openl.ie.constrainer.impl.*;
import java.util.*;
/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class PossibleViolation implements Violation{
  static private HashMap _violationTypes = new HashMap();
  int _deviation;
  int _type;

  PossibleViolation(int deviation, int type) {
    _deviation = deviation;
    _type = type;
    if (_violationTypes.get(new Integer(_type)) == null)
      _violationTypes.put(new Integer(_type), new Vector());
  }

  public int getMaxValue(){return _deviation;}

  public IntVar generateIntVar(Constrainer C, String name, int max){
    return registerViolation(new IntVarImpl(C, 0, max, name));
  }

  private IntVar registerViolation(IntVar var){
    ((Vector)(_violationTypes.get(new Integer(_type)))).add(var);
    return var;
  }

  public static Vector getAllViolationsOfType(int ruleNumber){
    return (Vector)(_violationTypes.get(new Integer(ruleNumber)));
  }

  public static Vector getAllViolations(){
    Vector out = new Vector();
    Iterator iter = _violationTypes.keySet().iterator();
    while(iter.hasNext()){
      out.addAll((Vector)(_violationTypes.get((Integer)iter.next())));
    }
    return out;
  }
}
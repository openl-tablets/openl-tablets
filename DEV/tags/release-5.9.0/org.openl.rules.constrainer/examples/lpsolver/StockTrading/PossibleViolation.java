package lpsolver.StockTrading;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.impl.FloatVarImpl;

public class PossibleViolation implements Violation{
  static private HashMap _violationTypes = new HashMap();
  double _deviation;
  int _type;

  PossibleViolation(double deviation, int type) {
    _deviation = deviation;
    _type = type;
    if (_violationTypes.get(new Integer(_type)) == null)
      _violationTypes.put(new Integer(_type), new Vector());
  }

  public double getMaxValue(){return _deviation;}

  public FloatVar generateFloatVar(Constrainer C, String name){
    return registerViolation(new FloatVarImpl(C, 0, _deviation, name));
  }

  private FloatVar registerViolation(FloatVar var){
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
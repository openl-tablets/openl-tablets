package lpsolver.StockTrading;

import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Constrainer;
/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public interface Violation {
  public double getMaxValue();
  public FloatVar generateFloatVar(Constrainer C, String name);
}
package lpsolver.StockTradingInt;


import org.openl.ie.constrainer.IntVar;
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
  public int getMaxValue();
  public IntVar generateIntVar(Constrainer C, String name, int max);
}
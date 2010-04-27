package lpsolver.StockTradingInt;


import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.Constrainer;
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
package lpsolver.StockTradingInt;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public interface Rule {
  public Violation getViolation();
  public int getWeight();
  public String getName();
}
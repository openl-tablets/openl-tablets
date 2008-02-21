package lpsolver.StockTradingInt;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class TradingRule implements Rule{
  private static int counter = 0;
  private int _weight;
  private Violation _viol;
  private String _name;
  public TradingRule(int weight, int deviation, String name) {
    _name = name;
    _weight = weight;
    _viol = new PossibleViolation(deviation, counter++);
  }

  public int getWeight(){return _weight;}
  public Violation getViolation(){return _viol;}
  public String getName() {return _name;}
}
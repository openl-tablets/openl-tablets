package lpsolver.StockTrading;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public interface Stock {
  double getPrice();
  int getID();
  String getName();
  int getType();
}
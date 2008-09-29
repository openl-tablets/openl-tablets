package lpsolver.StockTradingInt;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public interface StockTypes {
  int TOBACCO    = 1;
  int FINANCIAL  = 2;
  int UTILITIES  = 3;
  int TECHNOLOGY = 4;
  int INSURANCE  = 5;
  int HEALTHCARE = 6;
  int TRANSPORTATION = 7;
  int RETAIL     = 8;
  int ENERGY     = 9;
  int OTHER      = 10;
  String[] _TYPES = {"", "TOBACCO", "FINANCIAL", "UTILITIES",
                     "TECHNOLOGY", "INSURANCE", "HEALTHCARE",
                     "TRANSPORTATION", "RETAIL", "ENERGY", "OTHER"};
}
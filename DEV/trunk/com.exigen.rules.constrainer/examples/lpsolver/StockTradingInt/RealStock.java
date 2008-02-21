package lpsolver.StockTradingInt;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.*;

public class RealStock implements Stock{
  private static Vector  _allStocks = new Vector();
  private static HashMap _StockIDs = new HashMap();
  private String _name;
  private int _type;
  private int _price;
  private int _id;

  private RealStock(String name, int type, int price) {
    _name = name;
    _type = type;
    _price = price;
    _id = _allStocks.size();
    registerStock();
  }

  private void registerStock(){
    _allStocks.add(this);
    _StockIDs.put(_name, new Integer(_id));
  }

  public int getID(){return _id;}
  public int getPrice(){return _price;}
  public int getType(){return _type;}
  public String getName(){return _name;}
  static public Vector allStocks(){return _allStocks;}
  static public void createStock(String name, int type, int price){
    new RealStock(name, type, price);
  }

  static public int getStockID(String name){
    return ((Integer)_StockIDs.get(name)).intValue();
  }

}
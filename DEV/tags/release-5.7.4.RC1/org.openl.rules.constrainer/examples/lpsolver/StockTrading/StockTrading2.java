package lpsolver.StockTrading;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.impl.FloatVarImpl;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;
import org.openl.ie.constrainer.lpsolver.GoalSimplexSolve;

public class StockTrading2 {
  static private double[] inventory;
  static private double _initCash = 12730;
  static private double _investRequired = 0;
  static private Rule[] _tradingRules = new Rule[8];
  static String[] _names = null;
  static{
    RealStock.createStock("MSFT", StockTypes.FINANCIAL,   64);
    RealStock.createStock("INTC", StockTypes.TECHNOLOGY,  27);
    RealStock.createStock("CMB",  StockTypes.FINANCIAL,   71);
    RealStock.createStock("MO",   StockTypes.TOBACCO,     47);
    RealStock.createStock("ITWO", StockTypes.TECHNOLOGY,   6);
    RealStock.createStock("MANU", StockTypes.TECHNOLOGY,  10);
    RealStock.createStock("JDEC", StockTypes.TECHNOLOGY,  10);
    RealStock.createStock("PSFT", StockTypes.TECHNOLOGY,  30);
    RealStock.createStock("SEBL", StockTypes.TECHNOLOGY,  23);
    RealStock.createStock("IBM",  StockTypes.TECHNOLOGY, 113);
    RealStock.createStock("KSE",  StockTypes.UTILITIES,   34);
    RealStock.createStock("CRA",  StockTypes.OTHER,       25);
    RealStock.createStock("JPM",  StockTypes.FINANCIAL,   39);
    RealStock.createStock("AOL",  StockTypes.TECHNOLOGY,  37);
    RealStock.createStock("YHOO", StockTypes.TECHNOLOGY,  13);
    RealStock.createStock("AMD",  StockTypes.TECHNOLOGY,  12);

    RealStock.createStock("HWP",  StockTypes.TECHNOLOGY,  19);
    RealStock.createStock("GTW",  StockTypes.TECHNOLOGY,   7);
    RealStock.createStock("ORCL", StockTypes.TECHNOLOGY,  15);
    RealStock.createStock("DELL", StockTypes.TECHNOLOGY,  25);
    RealStock.createStock("CSCO", StockTypes.TECHNOLOGY,  19);
    RealStock.createStock("BEAS", StockTypes.TECHNOLOGY,  15);
    RealStock.createStock("ALA",  StockTypes.TECHNOLOGY,  17);
    RealStock.createStock("BROA", StockTypes.TECHNOLOGY,   1);
    RealStock.createStock("MER",  StockTypes.FINANCIAL,   49);
    RealStock.createStock("ALL",  StockTypes.INSURANCE,   32);
    RealStock.createStock("MET",  StockTypes.INSURANCE,   28);
    RealStock.createStock("JNJ",  StockTypes.HEALTHCARE,  60);
    RealStock.createStock("XOM",  StockTypes.ENERGY,      40);
    RealStock.createStock("CVX",  StockTypes.ENERGY,      95);
    RealStock.createStock("WMT",  StockTypes.RETAIL,      55);
    RealStock.createStock("MK",   StockTypes.RETAIL,       6);

    inventory = new double[RealStock.allStocks().size()];
    Arrays.fill(inventory, (double)0);
    inventory[RealStock.getStockID("CMB")]  = 200;
    inventory[RealStock.getStockID("MO")]   = 300;
    inventory[RealStock.getStockID("KSE")]  = 200;
    inventory[RealStock.getStockID("MANU")] = 300;
    inventory[RealStock.getStockID("MSFT")] = 100;
    inventory[RealStock.getStockID("CRA")]  = 400;

    _tradingRules[0] = new TradingRule(10,
                                       1,
                                     "Can't sell more than have");

    _tradingRules[1] = new TradingRule(8,
                                      1,
                                     "Must have enough cash to buy");

    _tradingRules[2] = new TradingRule( 6,
                                      10,
                                      "Can't buy tobacco stocks");

    _tradingRules[3] = new TradingRule(8,
                                      2,
                                      "Allocation of financial");

    _tradingRules[4] = new TradingRule(10,
                                       2,
                                     "Allocation of utilities");

    _tradingRules[5] = new TradingRule(5,
                                      2,
                                     "Allocation of technology");

    _tradingRules[6] = new TradingRule(   1,
                                      2000,
                                      "Exact Invest/Raise");

    _tradingRules[7] = new TradingRule(10,
                                       1,
                                      "Minimal lot size");

    _names = new String[RealStock.allStocks().size()];
    for (int i=0;i<_names.length; i++){
      _names[i] = ((Stock)(RealStock.allStocks().get(i))).getName();
    }


  }
  public double calculateAll(){
    return accumulateIf(inventory, new Predicate(){
                                    public boolean check(int i){
                                      return true;}
                                    });
  }

  private interface Predicate{
    public boolean check(int i);
  }

  private static class PredicateImp implements Predicate{
    private int _type;
    public PredicateImp(int type){
      _type = type;
    }
    public boolean check(int StockID){
      if (((Stock)(RealStock.allStocks().get(StockID))).getType() == _type)
        return true;
      return false;
    }
  }

  static private class PredicateTrue implements Predicate{
    public boolean check(int i){
      return true;
    }
  }

  static private double accumulateIf(double[] array, Predicate pred){
    double sum = 0;
    for (int i=0;i< array.length;i++){
      if (pred.check(i))
        sum += array[i];//*((Stock)(RealStock.allStocks().get(i))).getPrice();
    }
    return sum;
  }


  static private FloatExp accumulateIf (FloatExpArray array, Predicate pred){
    Vector tempVec = new Vector();
    for (int i=0; i<array.size();i++){
      if (pred.check(i))
        tempVec.add(array.get(i));
    }
    FloatExpArray tempArray = new FloatExpArray(array.constrainer(), tempVec);
    return tempArray.sum();
  }

  public StockTrading2() {
  }

  public static void main(String[] args) {
    try{
//    StockTrading2 stockTrading21 = new StockTrading2();
    Constrainer C = new Constrainer("StockTrading");
    Vector constraints = new Vector();
    Vector equalities = new Vector();
    // our aim is to minimize violations

    FloatExpArray buyingOrders  = new FloatExpArray(C, RealStock.allStocks().size());
    FloatExpArray sellingOrders = new FloatExpArray(C, RealStock.allStocks().size());
    FloatExpArray changes       = new FloatExpArray(C, RealStock.allStocks().size());
    for (int i=0;i<buyingOrders.size();i++){
      buyingOrders.set(new  FloatVarImpl(C,  0,  1000,
                  ((Stock)(RealStock.allStocks().get(i))).getName()+ "_bought"), i);
      sellingOrders.set(new FloatVarImpl(C, -1000, 0,
                  ((Stock)(RealStock.allStocks().get(i))).getName()+ "_sold"  ), i);
      changes.set(buyingOrders.get(i).add(sellingOrders.get(i)), i);
      changes.get(i).name(_names[i]);
    }


    // rule#0 can't sell more than have:
    for (int i=0;i<buyingOrders.size();i++)
      constraints.add(     buyingOrders.get(i)
                      .add(sellingOrders.get(i))
                      .add(inventory[i])
                      .add(_tradingRules[0].getViolation().generateFloatVar(C, _names[i] + "_shortage"))
                      .ge(0));

    // rule#1 must have enough cash to buy stocks
    double[] prices = new double[changes.size()];
    for (int i=0; i<prices.length; i++){
      prices[i] = ((Stock)(RealStock.allStocks().get(i))).getPrice();
    }
    constraints.add(C.scalarProduct(changes, prices)
                    .add(_tradingRules[1].getViolation().generateFloatVar(C, "debt"))
                    .eq(0));

    // rule#2 can't buy tobacco stocks
    equalities.add(accumulateIf(changes, new PredicateImp(StockTypes.TOBACCO))
                         .sub(_tradingRules[2].getViolation().generateFloatVar(C, "BoughtTobaccoes"))
                         .eq(0));

    // rule#3 to maintain the allocation of financial in the given range
    FloatExp totalStocks = accumulateIf(changes, new PredicateTrue())
                          .add(accumulateIf(inventory, new PredicateTrue()));
    FloatVar viol = _tradingRules[3].getViolation().generateFloatVar(C, "FinancialsOutOfRange");

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.FINANCIAL))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.FINANCIAL)))
                    .add(viol)
                    .gt(totalStocks.mul(0.1)));

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.FINANCIAL))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.FINANCIAL)))
                    .sub(viol)
                    .lt(totalStocks.mul(0.2)));

    // rule#4 to maintain the allocation of utilities in the given range (0% - 10%)
    viol = _tradingRules[4].getViolation().generateFloatVar(C, "UtilitiesOutOfRange");
    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.UTILITIES))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.UTILITIES)))
                    .add(viol)
                    .gt(0));

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.UTILITIES))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.UTILITIES)))
                    .sub(viol)
                    .lt(totalStocks.mul(0.1)));

    // rule#5 to maintain the allocation of technology in the given range (20% - 30%)
    viol = _tradingRules[5].getViolation().generateFloatVar(C, "TechnologicalOutOfRange");
    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.TECHNOLOGY))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.TECHNOLOGY)))
                    .add(viol)
                    .gt(totalStocks.mul(0.2)));

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.TECHNOLOGY))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.TECHNOLOGY)))
                    .sub(viol)
                    .lt(totalStocks.mul(0.3)));

    // rule#6 invest as close as possible to the given value
    viol = _tradingRules[5].getViolation().generateFloatVar(C, "InvestmentFault");
    constraints.add(C.scalarProduct(changes, prices)
                    .sub(viol)
                    .lt(_investRequired));

    constraints.add(C.scalarProduct(changes, prices)
                    .add(viol)
                    .gt(_investRequired));

    FloatExpArray allViolations = new FloatExpArray(C, PossibleViolation.getAllViolations());

    ConstrainerLP problem = new org.openl.ie.constrainer.lpsolver.impl.LPProblemImpl(allViolations.sum(), false);

    problem.addConstraints(constraints, false);
    problem.addConstraints(equalities, true);
    for (int i=0;i<changes.size();i++){
      problem.addVar((FloatVar)buyingOrders.get(i));
      problem.addVar((FloatVar)sellingOrders.get(i));
    }

    DataOutputStream fstr = new DataOutputStream(new FileOutputStream("d:\\output\\StockTrading\\out.txt"));
    fstr.writeBytes("Variables: \n");
    for (int i=0;i<problem.nbVars();i++){
      fstr.writeBytes("" + i + ":" + problem.getVar(i).name()+"\n");
    }
    fstr.writeBytes("Constraints: \n");
    for (int i=0; i<problem.nbConstraints(); i++){
      fstr.writeBytes(problem.getLPConstraint(i)+"\n");
    }
     Goal solve = new GoalSimplexSolve(C, problem);
   /*  System.out.println("Test!!!");
     System.out.println(accumulateIf(inventory, new PredicateTrue()));
     System.out.println(accumulateIf(inventory, new PredicateImp(StockTypes.TECHNOLOGY)));*/
     boolean flag = C.execute(solve);
     if (flag){
      System.out.println("Vse putem!!!!");
      //System.out.println(buyingOrders);
      //System.out.println(sellingOrders);
      System.out.println(allViolations);
      System.out.println(changes);
      System.out.println("Cash: " + (_initCash + C.scalarProduct(changes, prices).value()));
     }
     else
      System.out.println("Vse oblazhalos'!!!!");
    }



    /*catch(UnexpectedVariable var){
      System.out.println("UnexpectedVariable: " + var.getExp());
    }*/
    /*catch(NonLinearExpression exp){
      System.out.println("Nonlinear expression!!!");
      exp.printStackTrace();
    }*/
    catch(Exception exp){
      exp.printStackTrace();
    }
  } // end of main

} //end of class StockTrading2 description
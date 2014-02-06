package lpsolver.StockTradingInt;

import java.util.*;
import org.openl.ie.constrainer.impl.*;
import org.openl.ie.constrainer.*;
import org.openl.ie.constrainer.lpsolver.*;
import org.openl.ie.constrainer.lpsolver.impl.*;
import java.io.*;
/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class StockTrading2 {
  static private int[] inventory;
  static private int _initCash = 12730;
  static private int _investRequired = 5000;
  static private Rule[] _tradingRules = new Rule[8];

  static private int _technologyINF = 20;
  static private int _technologySUP = 20;
  static private int _utilitiesINF  =  4;
  static private int _utilitiesSUP  =  4;
  static private int _financialINF  = 10;
  static private int _financialSUP  = 10;

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

    inventory = new int[RealStock.allStocks().size()];
    Arrays.fill(inventory, (int)0);
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
                                      20,
                                      "Allocation of financial");

    _tradingRules[4] = new TradingRule(10,
                                       20,
                                     "Allocation of utilities");

    _tradingRules[5] = new TradingRule(5,
                                      20,
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
//  private int calculateAll(){
//    return accumulateIf(inventory, new Predicate(){
//                                    public boolean check(int i){
//                                      return true;}
//                                    });
//  }

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

  static private int accumulateIf(int[] array, Predicate pred){
    int sum = 0;
    for (int i=0;i< array.length;i++){
      if (pred.check(i))
        sum += array[i];//*((Stock)(RealStock.allStocks().get(i))).getPrice();
    }
    return sum;
  }


  static private IntExp accumulateIf (IntExpArray array, Predicate pred){
    Vector tempVec = new Vector();
    for (int i=0; i<array.size();i++){
      if (pred.check(i))
        tempVec.add(array.get(i));
    }
    IntExpArray tempArray = new IntExpArray(array.constrainer(), tempVec);
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

    IntExpArray buyingOrders  = new IntExpArray(C, RealStock.allStocks().size());
    IntExpArray sellingOrders = new IntExpArray(C, RealStock.allStocks().size());
    IntExpArray changes       = new IntExpArray(C, RealStock.allStocks().size());
    for (int i=0;i<buyingOrders.size();i++){
      buyingOrders.set(new  IntVarImpl(C,  -10,  10,
                  ((Stock)(RealStock.allStocks().get(i))).getName()+ "_bought"), i);
      sellingOrders.set(new IntVarImpl(C, -500, 0,
                  ((Stock)(RealStock.allStocks().get(i))).getName()+ "_sold"  ), i);
      changes.set(buyingOrders.get(i).mul(100), i);
      changes.get(i).name(_names[i]);
    }


    // rule#0 can't sell more than have:
    for (int i=0;i<buyingOrders.size();i++)
      constraints.add(changes.get(i)
                      .add(inventory[i])
                      .add(_tradingRules[0].getViolation()
                           .generateIntVar(C, _names[i] + "_shortage", inventory[i] + changes.get(i).max()))
                      .ge(0));

    // rule#1 must have enough cash to buy stocks
    double[] prices = new double[changes.size()];
    for (int i=0; i<prices.length; i++){
      prices[i] = ((Stock)(RealStock.allStocks().get(i))).getPrice();
    }
    constraints.add(C.scalarProduct(changes, prices).neg()
                    .add(_initCash)
                    .add(_tradingRules[1].getViolation().generateIntVar(C, "debt", _initCash))
                    .gt(0));

    // rule#2 can't buy tobacco stocks
    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.TOBACCO))
                         .sub(_tradingRules[2].getViolation().generateIntVar(C, "BoughtTobaccoes",
                                                                             1000))
                        .lt(0));


    // rule#3 to maintain the allocation of financial in the given range
    IntExp totalStocks = accumulateIf(changes, new PredicateTrue())
                          .add(accumulateIf(inventory, new PredicateTrue()));
    IntVar viol = _tradingRules[3].getViolation().generateIntVar(C, "FinancialsOutOfRange",
                                                                 1000);

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.FINANCIAL))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.FINANCIAL)))
                    .add(viol)
                    .mul(100)
                    .gt(totalStocks.mul(_financialINF)));

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.FINANCIAL))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.FINANCIAL)))
                    .sub(viol)
                    .mul(100)
                    .lt(totalStocks.mul(_financialSUP)));

    // rule#4 to maintain the allocation of utilities in the given range (5% - 6%)
    viol = _tradingRules[4].getViolation().generateIntVar(C, "UtilitiesOutOfRange",
                                                          1000);
    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.UTILITIES))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.UTILITIES)))
                    .add(viol)
                    .mul(100)
                    .gt(totalStocks.mul(_utilitiesINF)));

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.UTILITIES))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.UTILITIES)))
                    .sub(viol)
                    .mul(100)
                    .lt(totalStocks.mul(_utilitiesSUP)));

    // rule#5 to maintain the allocation of technology in the given range (20% - 21%)
    viol = _tradingRules[5].getViolation().generateIntVar(C, "TechnologicalOutOfRange",
                                                        1000);
    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.TECHNOLOGY))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.TECHNOLOGY)))
                    .add(viol)
                    .mul(100)
                    .gt(totalStocks.mul(_technologyINF)));

    constraints.add(accumulateIf(changes, new PredicateImp(StockTypes.TECHNOLOGY))
                    .add(accumulateIf(inventory, new PredicateImp(StockTypes.TECHNOLOGY)))
                    .sub(viol)
                    .mul(100)
                    .lt(totalStocks.mul(_technologySUP)));

   // rule#6 invest as close as possible to the given value
    viol = _tradingRules[5].getViolation().generateIntVar(C, "InvestmentFault", _initCash);
    constraints.add(C.scalarProduct(changes, prices).neg()
                    .sub(viol)
                    .lt(-_investRequired));

    /*constraints.add(C.scalarProduct(changes, prices)
                    .sub(viol)
                    .gt(_investRequired));*/

    IntExpArray allViolations = new IntExpArray(C, PossibleViolation.getAllViolations());

    ConstrainerMIP problem = new LPIntegerProblemImpl(allViolations.sum(), true);
    for (int i = 0 ; i<allViolations.size();i++)
      System.out.println(allViolations.get(i));
    problem.addConstraints(constraints, false);
    problem.addConstraints(equalities, true);
    for (int i=0;i<changes.size();i++){
      problem.addVar((IntVar)buyingOrders.get(i));
      problem.addVar((IntVar)sellingOrders.get(i));
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
     Goal solve = new GoalIntSimplexSolve(C, problem);
     boolean flag = C.execute(solve);

     if (flag){
      System.out.println("Vse putem!!!!");
      System.out.println(allViolations);
      for (int i=0;i<changes.size();i++){
        int val = changes.get(i).value();
        if (val != 0)
          System.out.println(changes.get(i).name() + " : " + changes.get(i).value());
      }
      System.out.println("*******************************************************************\n");
      System.out.println("Position\t" + "Value\t" + " Sector\t" );
      for (int i=0;i<changes.size();i++){
        int val = changes.get(i).value() + inventory[i];
        if (val != 0)
          System.out.println(changes.get(i).name() + "\t" +( inventory[i] + changes.get(i).value()) + "\t"
           + StockTypes._TYPES[((Stock)(RealStock.allStocks().get(i))).getType()]);
      }
      System.out.println("Total\t" + totalStocks.value());
      System.out.println("*******************************************************************");
            System.out.println("Sector\t" + "Value\t" + "Percentage");
      int valueF = accumulateIf(changes, new PredicateImp(StockTypes.UTILITIES))
                .add(accumulateIf(inventory, new PredicateImp(StockTypes.UTILITIES))).value();
      System.out.println("Utilities\t" +
                         valueF + "\t" +
                         ((double)valueF/totalStocks.value())*100
                        );
      valueF = accumulateIf(changes, new PredicateImp(StockTypes.FINANCIAL))
                .add(accumulateIf(inventory, new PredicateImp(StockTypes.FINANCIAL))).value();
      System.out.println("Financial\t" +
                         valueF + "\t" +
                         ((double)valueF/totalStocks.value())*100
                         );

      valueF = accumulateIf(changes, new PredicateImp(StockTypes.TECHNOLOGY))
                .add(accumulateIf(inventory, new PredicateImp(StockTypes.TECHNOLOGY))).value();
      System.out.println("Technology\t" +
                         valueF + "\t" +
                         ((double)valueF/totalStocks.value())*100
                         );
      System.out.println("*******************************************************************\n");

      System.out.println("Cash: " + (C.scalarProduct(changes, prices).neg()
                    .add(_initCash).value() - ((IntVar)(PossibleViolation.getAllViolationsOfType(1).get(0))).value()));

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
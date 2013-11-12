package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: ChangingMoney<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * let S be a given sum that we want to achieve
 * with a minimal amount of coins in denominations of x1, x2, ..., xn.  (In our example,
 * S is 123 cents, n is 4, x1 is 1 cent, x2 is 10 cents, x3 is
 * 25 cents, and xn is 100 cents.)<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class ChangingMoney
{
  static Constrainer C = new Constrainer("ChangingMoney");
  static final int denominations[] = {1,10,25,100};
  static int SUM = 123;
  public ChangingMoney()
  {

  }

  public static void main (String[] argv){
    try{
      IntExpArray coinsCounter = new IntExpArray(C,denominations.length);
      for (int i=0;i<denominations.length;i++){
        coinsCounter.set(C.addIntVar(0,(int)(SUM/denominations[i]) ),i);
      }
      C.postConstraint(C.scalarProduct(coinsCounter,denominations).eq(SUM));
      C.postConstraint(coinsCounter.elementAt(0).le(5)); // we can use 5 pennies at most

      IntExp nbCoins = coinsCounter.sum();

      Goal gen = new GoalGenerate(coinsCounter,
                                  new IntVarSelectorMaxSize(coinsCounter),
                                  null);
      Goal total = new GoalFastMinimize(gen, nbCoins);

      C.printInformation();
      C.execute(total);
      System.out.println(coinsCounter);
    }
    catch(Failure f){
      System.out.println("There is no solutions");
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}
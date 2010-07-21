package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: Warehouse example<br>
 * Description: <br>
 * In this problem, a company is considering a number of
 * locations for building warehouses to supply its existing
 * stores. Each possible warehouse has a fixed maintenance
 * cost and a maximum capacity specifying how many stores
 * it can support. In addition, each store can be supplied
 * by only one warehouse and the supply cost to the store
 * varies according to the warehouse selected.<br>
 * Find which warehouses have to be built while minimizing
 * the total cost consisting of warehouse maintenance cost
 * and supply cost.<br>
 * <br>
 * Copyright: Copyright (ñ) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 4.0
 */

 public class Warehouse4 {

  public static void main(String[] args) throws Failure {
    // warehouse maintenance cost
    int fixed = 300;
    // number of stores
    int nbStores = 21;
    // number of warehouses
    int nbWarehouses = 20;

    Constrainer c = new Constrainer ("Warehouse");

    // warehouse to store supply cost matrix
    IntArray    supplyCost [] = new IntArray [nbStores];

    // the matrix initialization
    supplyCost [0] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [1] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [2] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});
    supplyCost [3] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [4] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [5] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});
    supplyCost [6] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [7] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [8] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});
    supplyCost [9] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [10] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [11] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});
    supplyCost [12] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [13] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [14] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});
    supplyCost [15] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [16] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [17] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});
    supplyCost [18] = new IntArray (c, new int []{20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4});
    supplyCost [19] = new IntArray (c, new int []{29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48});
    supplyCost [20] = new IntArray (c, new int []{110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44});

    // the array of conditions showing whether a warehouse is opened
    IntExpArray openWarehouses = new IntExpArray(c, nbWarehouses);
    for (int i = 0; i < nbWarehouses; i++) {
      openWarehouses.set(c.addIntBoolVar(), i);
    }
    // the array of store assignment to warehouses
    IntExpArray storeAssign =
      new IntExpArray(c, nbStores, 0, nbWarehouses - 1, "SA");

    // if a store is assigned to a warehouse then the warehouse is opened
    for (int i = 0; i < nbStores; i++) {
      for (int j = 0; j < nbWarehouses; j++) {
        c.postConstraint(
          storeAssign.get(i).eq(j).implies(
            (IntBoolExp)openWarehouses.get(j)
           )
        );
      }
    }

    // warehouse->store transition cost array
    IntExpArray transCost = new IntExpArray (c, nbStores);
    for (int i = 0; i < nbStores; i++) {
      transCost.set(supplyCost [i].elementAt (storeAssign.elementAt (i)), i);
    }

    // the first summand of the cost function is the transition cost sum
    // the second summand of the cost function is the maintenance cost of warehouses
    IntExp cost = transCost.sum ().add (openWarehouses.sum ().mul (fixed));

    c.printInformation ();
    c.execute (
      new GoalFastMinimize (
        new GoalAnd (
          new GoalGenerate (openWarehouses),
          new GoalGenerate (storeAssign)
        ),
        cost
      )
    );
    System.out.println("Optimal cost    :" + cost.toString ());
    System.out.print("Open warehouses :" );
    for (int i = 0; i < nbWarehouses; i++) {
      System.out.print("  " + openWarehouses.get(i).value());
    }
    System.out.println();
    System.out.print("Store assignment:");
    for (int i = 0; i < nbStores; i++) {
      System.out.print(" " + storeAssign.get(i).value());
    }
  }
}
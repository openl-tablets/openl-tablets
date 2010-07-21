package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: Warehouse example <br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * In this problem, a company is considering a number of
 * locations for building warehouses to supply its existing
 * stores. Each possible warehouse has a fixed maintenance
 * cost and a maximum capacity specifying how many stores
 * it can support. In addition, each store can be supplied
 * by only one warehouse and the supply cost to the store
 * varies according to the warehouse selected.<br>
 * Find which warehouses have to be built while minimizing
 * the total cost consisting of warehouse maintenance cost
 * and supply cost. <br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Sergey Vanskov
 * @version 2.0
 */

public class Warehouse2 {
  public static void printDelimeter (int nbStores) {
    System.out.print ("+-----+");
    for (int i = 0; i < nbStores; i++) {
      System.out.print ("---+");
    }
    System.out.println ();
  }

  public static void main(String[] args) {
    try {
      // warehouse maintenance cost
      int fixed = 300;
      // number of stores
      int nbStores = 14;
      // number of warehouses
      int nbWarehouses = 20;

      Constrainer c = new Constrainer ("Warehouse");
      // warehouse capacities
      int capacity [] = new int [nbWarehouses];
      for (int i = 0; i < nbWarehouses; i++) {
        capacity [i] = nbStores;
      }
      // warehouse to store supply cost matrix
      int supplycost [] [] =
        {
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44},
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44},
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44},
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44},
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44},
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44},
          {20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4},
          {29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44,20,52,37,48},
          {110,24,83,4,29,62,43,44,20,52,37,48,110,24,83,4,29,62,43,44}
        };
      IntExpArray open = new IntExpArray (c, nbWarehouses, 0, 1, "open");
      // array of bool variables: "Does a warehouse supply to a store?"
      IntExpArray supply = new IntExpArray (c, nbStores * nbWarehouses, 0, 1, "supply");

      // array of int variables: "The number of stores being supplied from the warehouse"
      IntExpArray whsum = new IntExpArray (c, nbWarehouses);
      // array of int variables: "The number of warehouses that supply to the store"
      IntExpArray ssum = new IntExpArray (c, nbStores);

      // populate ssum array
      for (int i = 0; i < nbStores; i++) {
        IntExp  l_SSum = supply.elementAt (i * nbWarehouses);
        for (int j = 1; j < nbWarehouses; j++) {
          l_SSum = l_SSum.add (supply.elementAt (i * nbWarehouses + j));
        }
        ssum.set (l_SSum, i);
      }

      // populate whsum array
      for (int i = 0; i < nbWarehouses; i++) {
        IntExp  l_WHSum = supply.elementAt (i);
        for (int j = 1; j < nbStores; j++) {
          l_WHSum = l_WHSum.add (supply.elementAt (j * nbWarehouses + i));
        }
        whsum.set (l_WHSum, i);
      }

      // put constraints
      for (int i = 0; i < nbStores; i++) {
        // each store can be supplied by only one warehouse
        c.addConstraint (ssum.elementAt (i).eq (1));
      }
      for (int i = 0; i < nbWarehouses; i++) {
        // each warehouse can support stores up to the maximum capacity
        c.addConstraint (whsum.elementAt (i).le (open.elementAt (i).mul (capacity [i])));
      }

      // calculating cost function
      IntExp cost = new IntExpConst (c, 0);
      for (int i = 0; i < nbWarehouses; i++) {
        for (int j = 0; j < nbStores; j++) {
          // add supply cost
          cost = cost.add (supply.elementAt (j * nbWarehouses + i).mul (supplycost [j] [i]));
        }
        // add warehouse maintenance cost
        cost = cost.add (open.elementAt (i).mul (fixed));
      }

      // execute the goal that minimize cost function
      long start = System.currentTimeMillis();
      try {
        c.postConstraints ();
      } catch (Failure e_Failure) {
        System.out.println ("The system is overconstrained. Solution can not be found");
      }
      c.printInformation ();
      c.execute (
        new GoalMinimize (
          new GoalAnd (
            new GoalGenerate (open),
            new GoalGenerate (supply)
          ),
          cost
        )
      );
      long finish = System.currentTimeMillis();

      // output the results
      System.out.println ();
      System.out.println ("Calculation takes " + (finish - start) + " milliseconds");
      System.out.println ();
      System.out.println ("Cost function minimum: " + cost.value ());
      System.out.println ();

      printDelimeter (nbStores);
      System.out.print ("| W/S |");
      for (int i = 1; i <= nbStores; i++) {
        System.out.print (" ");
        if (i < 10) {
          System.out.print (i + " |");
        } else {
          System.out.print (i + "|");
        }
      }
      System.out.println ();
      printDelimeter (nbStores);
      for (int i = 0; i < nbWarehouses; i++) {
        if (whsum.elementAt(i).value () < 10) {
          System.out.print ("| " + whsum.elementAt(i).value () + "   |");
        } else {
          System.out.print ("| " + whsum.elementAt(i).value () + "  |");
        }
        for (int j = 0; j < nbStores; j++) {
          if (supply.elementAt (j * nbWarehouses + i).value () != 0) {
            System.out.print (" X |");
          } else {
            System.out.print ("   |");
          }
        }
        System.out.println ();
        printDelimeter (nbStores);
      }

    } catch (Failure e_Failure) {
      // report failure
      System.out.println (e_Failure);
    }
  }
}
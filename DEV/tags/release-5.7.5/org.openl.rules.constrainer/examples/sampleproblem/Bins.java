package sampleproblem;

import java.util.Vector;
import org.openl.ie.constrainer.*;

/**
 * Title: Bins<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * Given a supply of different components and bins of given types,
 * determine all assignments of components to bins satisfying
 * specified assignment constraints subject to an optimization criterion.<br>
 * <br>
 * In the following example there are five types of components:
 * <UL compact>
 *   <LI compact>glass</LI>
 *   <LI compact>plastic</LI>
 *   <LI compact>steel</LI>
 *   <LI compact>wood</LI>
 *   <LI compact> copper</LI>
 * </UL>
 * There are three types of bins:
 * <UL>
 *   <LI>red</LI>
 *   <LI>blue</LI>
 *   <LI>green</LI>
 * </UL>
 * whose capacity constraints are:
 * <UL>
 *   <LI>red   has capacity 3</LI>
 *   <LI>blue  has capacity 1</LI>
 *   <LI>green has capacity 4</LI>
 * </UL>
 * whose containment constraints are:<br>
 * <UL>
 *   <LI>red   can contain glass, wood, copper</LI>
 *   <LI>blue  can contain glass, steel, copper</LI>
 *   <LI>green can contain plastic, wood, copper</LI>
 * </UL>
 * and requirement constraints are (for all bin types):<br>
 * <UL>
 *   <LI>wood requires plastic</LI>
 * </UL>
 * Certain component types cannot coexist:<br>
 * <UL>
 *   <LI>glass  exclusive copper</LI>
 *   <LI>copper exclusive plastic</LI>
 * </UL>
 * and certain bin types have capacity constraint for certain components:<br>
 * <UL>
 *   <LI>red   contains at most 1 of wood</LI>
 *   <LI>green contains at most 2 of wood</LI>
 * </UL>
 * Here is the components demand:</LI>
 * <UL>
 *   <LI>1 of glass</LI>
 *   <LI>2 of plastic</LI>
 *   <LI>1 of steel</LI>
 *   <LI>3 of wood</LI>
 *   <LI>2 of copper</LI>
 * </UL>
 * <br>
 * The problem statement:
 * What is the minimum total number of bins required to contain the components?<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class Bins
{
  // Components
  static final int glass   = 0;
  static final int plastic = 1;
  static final int steel   = 2;
  static final int wood    = 3;
  static final int copper  = 4;

  static final String[] components = { "glass", "plastic", "steel", "wood", "copper" };
  // Demand
  static final int[] _demand =       { 1,        2,         1,       3,      2};
  static int[] demand; // demand * size

  // Bins
  static final int red = 0;
  static final int blue = 1;
  static final int green = 2;
  static final String[] binTypes =   { "red", "blue", "green" };
  static final int[] binCapacities = { 3,     1,      4 };

  // Constrainer
  static Constrainer C = null;
  static IntArray capacities = null;

  class Bin
  {
    public int		id;
    public IntVar       type;
    public IntExp       capacity;
    public IntExpArray	counts; // per component

    IntExp count(int component)
    {
      return counts.elementAt(component);
    }

    Bin(int binId) throws Failure
    {
      id = binId;
      System.out.println("Create Bin-"+id);
      type = C.addIntVar(0, binTypes.length-1,"bin"+id+"Type");
      int demand_max = demand[0];
      for(int i=1; i<demand.length; ++i)
      {
        if (demand_max < demand[i])
          demand_max = demand[i];
      }
      counts = new IntExpArray(C, components.length, 0, demand_max, "bin"+id+"Components");

      // Define capacity-type constraint
      capacity = capacities.elementAt(type);

      // Post capacity constraint
      IntExp counts_sum = counts.sum();
      C.postConstraint(counts_sum.gt(0));
      C.postConstraint(counts_sum.le(capacity));

      // Post containment constraints

      // red   contains at most 1 of wood
      C.postConstraint(
        type.eq(red).implies(counts.elementAt(wood).le(1))
      );

      // green contains at most 2 of wood
      C.postConstraint(
        type.eq(green).implies(counts.elementAt(wood).le(2))
      );

      // red   can contain glass, wood, copper
      C.postConstraint(type.eq(red).implies(
        counts.elementAt(plastic).eq(0).and(counts.elementAt(steel).eq(0)))
      );

      // blue  can contain glass, steel, copper
      C.postConstraint(type.eq(blue).implies(
        counts.elementAt(plastic).eq(0).and(counts.elementAt(wood).eq(0)))
      );

      // green can contain plastic, wood, copper
      C.postConstraint(type.eq(green).implies(
        counts.elementAt(glass).eq(0).and(counts.elementAt(steel).eq(0)))
      );

      // wood requires plastic
      C.postConstraint(
        counts.elementAt(wood).gt(0).implies(counts.elementAt(plastic).gt(0))
      );

      // glass  exclusive copper
      C.postConstraint(
        counts.elementAt(glass).eq(0).or(counts.elementAt(copper).eq(0))
      );

      // copper exclusive plastic
      C.postConstraint(
        counts.elementAt(copper).eq(0).or(counts.elementAt(plastic).eq(0))
      );
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer();
      try
      {
        buf.append("Bin#"+id + " (" + binTypes[type.value()]+"):");

        for(int i= 0; i < components.length; ++i)
        {
          int c = counts.elementAt(i).value();
          if (c>0)
            buf.append(" " + components[i]+"="+c);
        }
      }
      catch(Failure f)
      {
        System.out.println(f);
        buf.append("Bin-"+id+" is not instantiated");
      }
      return buf.toString();
    }

  }

  private Vector _bins = null;
  private int numberOfBins = 0;

  Bin bin(int i)
  {
    return (Bin)_bins.elementAt(i);
  }

  public Bins(int n) throws Failure
  {
    numberOfBins = n;

    C = new Constrainer("Bins "+n);
    C.expressionFactory().useCache(true);
    C.printInformation();

    capacities = new IntArray(C,binCapacities);

    System.out.println("Create "+numberOfBins+" bins..");
    _bins = new Vector(numberOfBins);
    for(int i=0; i < numberOfBins; i++)
    {
      _bins.add(new Bin(i+1));
      // avoid bins symmetry
      if (i>0)
      {
        Bin bin1 = (Bin)_bins.elementAt(i-1);
        Bin bin2 = (Bin)_bins.elementAt(i);
        C.postConstraint(bin1.type.le(bin2.type));
      }
    }

    // Post demand constraints..
    for(int j = 0; j < components.length; j++)
    {
      IntExpArray jcounts = new IntExpArray(C,numberOfBins);
      jcounts.name("type"+j+"Components");
      for(int b = 0; b < numberOfBins; b++)
      {
        jcounts.set(bin(b).count(j),b);
      }
      C.postConstraint(jcounts.sum().eq(demand[j]));
    }
  }

  boolean solve()
  {
    IntExpArray types = new IntExpArray(C,numberOfBins);
    IntExpArray counts = new IntExpArray(C,numberOfBins*components.length);
    int x = 0;
    for(int b = 0; b < numberOfBins; b++)
    {
      types.set(bin(b).type,b);
      for(int j = 0; j < components.length; j++)
      {
        counts.set(bin(b).count(j),x);
        x++;
      }
    }
    Goal instantiateBinTypes = new GoalGenerate(types);
    Goal instantiateBinComponents = new GoalGenerate(counts);
    Goal search = new GoalAnd(instantiateBinTypes,instantiateBinComponents);

    return C.execute(search);
  }


  public static void main(String[] args)
  {
    int size = 1;
    long executionStart = System.currentTimeMillis();

    if(args.length > 0)
      size = Integer.parseInt(args[0]);

    // Create demand = _demand*size;
    demand =  new int[_demand.length];
    for (int i=0; i<_demand.length; ++i)
    {
      demand[i] = _demand[i]*size;
    }

    int numberOfBins = 1;
    while(true)
    {
      try
      {
        System.out.println("Solve the problem for "+numberOfBins+" bins");
        Bins bins = new Bins(numberOfBins);
        System.out.println("Searching...");
        if (bins.solve())
        {
          long executionTime = System.currentTimeMillis() - executionStart;
          System.out.println("Execution time: "+executionTime+" msec");
          System.out.println("Solution: "+numberOfBins+" bins");
          for(int b = 0; b < numberOfBins; b++)
          {
            System.out.println(bins.bin(b));
          }
          break;
        }
      }
      catch(Failure f)
      {
        System.out.println(f);
      }
      System.out.println("No enough bins");
      numberOfBins++;
    }
  }
}
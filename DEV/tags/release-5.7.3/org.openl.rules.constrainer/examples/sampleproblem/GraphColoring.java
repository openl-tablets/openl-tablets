package sampleproblem;

import java.util.Date;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelector;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.IntVarSelector;
import org.openl.ie.constrainer.IntVarSelectorFirstUnbound;
import org.openl.ie.constrainer.IntVarSelectorMinSize;

/**
 * Title: GraphColoring<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * A graph coloring problem consists of choosing colors for the nodes
 * of a graph so that adjacent nodes are not the same color.
 * Our goal is to offer a general way to solve a graph coloring
 * problem when all the cliques of the graph are known. We'll consider only
 * a very special kind of graph for this example.<br>
 * <br>
 * The kind of graph that we'll color is one with n*(n-1)/2
 * nodes, where n is odd and where every node belongs to exactly
 * two maximal cliques of size n.  A clique is a complete subgraph.
 * In other words, a clique is a set of nodes linked pair-wise.<br>
 * <br>
 * For example, for n=5, there is a graph consisting of the following
 * maximal cliques:<br>
 *   &nbsp;&nbsp;c0 = {0, 1, 2, 3, 4}<br>
 *   &nbsp;&nbsp;c1 = {0, 5, 6, 7, 8}<br>
 *   &nbsp;&nbsp;c2 = {1, 5, 9, 10, 11}<br>
 *   &nbsp;&nbsp;c3 = {2, 6, 9, 12, 13}<br>
 *   &nbsp;&nbsp;c4 = {3, 7, 10, 12, 14}<br>
 *   &nbsp;&nbsp;c5 = {4, 8, 11, 13, 14}<br>
 * <br>
 * The minimum number of colors needed for this graph is n since
 * there is a click of size n.  Consequently, our problem is to find
 * whether there is a way to color such a graph in n colors.<br>
 * <br>
 * Copyright: Copyright (c) 2000<br>
 * Company: Exigen Group<br>
 * @author Jacob Feldman
 * @version 1.0
 */

public class GraphColoring
{
  static int f(int i, int j, int n)
  {
    if (j >= i)
      return (i*n-i*(i+1)/2+j-i);
    else
      return f(j,i-1,n);
  }

  public static void main(String[] args)
  {
    String arg = (args.length==0)?"41":args[0];
    int clique_size = Integer.parseInt(arg);
    System.out.println("Graph Coloring for "+clique_size+" cliques. "+new Date());
    int n = (clique_size%2>0)?clique_size+1:clique_size;
    boolean min_size_selector = true;
    boolean redundant_constraint = true;

    try
    {
      long start_millis = System.currentTimeMillis();
      Constrainer C = new Constrainer("Graph Coloring");
      C.printInformation();
      int size = n*(n-1)/2;

      int i, j;
      int nbColors = n-1;
      IntExpArray vars = new IntExpArray(C, size);
      for(i = 0; i < size; i++)
      {
        vars.set(C.addIntVar(0,nbColors-1,"var"+i), i);
      }

      C.traceFailures(5000);

      IntExpArray[] cliques = new IntExpArray[n];
      for(i = 0; i < n; i++)
      {
        cliques[i] = new IntExpArray(C, n - 1);
        for(j = 0; j < n-1; j++)
        {
          int node = f(i,j,n);
          IntVar v = (IntVar)vars.elementAt(node);
          cliques[i].set(v, j);
        }
        Constraint constraintAllDiff = C.allDiff(cliques[i]);
        constraintAllDiff.execute();
      }

      // Redundant Constraint: every color is used at least n/2 times
      IntExpArray cards = C.distribute(vars,nbColors);
      if (redundant_constraint)
      {
        int min_color_use = n/2;
        for(int v=0; v < cards.size(); ++v)
        {
          IntExp card = (IntExp)cards.elementAt(v);
          Constraint ct = card.moreOrEqual(min_color_use);
          ct.execute();
        }
      }

      // define search goal
      IntValueSelector value_selector = new IntValueSelectorMin();
      IntVarSelector var_selector;
      if (min_size_selector)
        var_selector = new IntVarSelectorMinSize(vars);
      else
        var_selector = new IntVarSelectorFirstUnbound(vars);

      Goal generate = new GoalGenerate(vars,var_selector,value_selector);

      C.execute(generate);

      // print Solution
      System.out.println("Solution:");

      long end_millis = System.currentTimeMillis();
      long processing_time = end_millis - start_millis +1;
      System.out.println("End Search: "+ new Date());
      System.out.println("Processing Time: "+ processing_time +" millis");

      for(i = 0; i < n; i++)
      {
        System.out.print("\nClique "+i+":");
//        Vector aux = new Vector(n-1);
        for(j = 0; j < n-1; j++)
        {
          int node = f(i,j,n);
          int color = ((IntVar)vars.elementAt(node)).value();
          System.out.print(" "+node+"="+color);
        }
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception: "+e);
    }
  }
}

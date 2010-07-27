package sampleproblem;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExpArray;

/**
 * Title: CarSequence<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * Car sequencing problems arise on assembly lines in factories
 * in the automotive industry.<br>
 * <br>
 * There, an assembly line makes it possible to build many different
 * types of cars, where the types correspond to a basic model with added
 * options.  In that context, one type of vehicle can be seen as a particular
 * configuration of options.  Even without loss of generality, we
 * can assume that it is possible to put multiple options on the same
 * vehicle while it is on the line.  In that way, virtually any
 * configuration (taken as an isolated case) could be produced
 * on the assembly line.  In contrast, for practical reasons (such
 * as the amount of time needed to do so), a given option really
 * cannot be installed on every vehicle on the line.  This constraint
 * is defined by what we call the ``capacity'' of an option.
 * The capacity of an option is usually represented as a ratio
 * p/q where for any sequence of q cars on the line, at most p of
 * them will have that option.<br>
 * <br>
 * The problem in car sequencing then consists of determining in which
 * order cars corresponding to each configuration should be assembled,
 * while keeping in mind that we must build a certain number of cars per
 * configuration.<br>
 * <br>
 * In this example, we'll consider the following version of the problem:
 * <UL>
 *   <LI>10 cars to build</LI>
 *   <LI>5 options available for installation</LI>
 *   <LI>6 configurations required</LI>
 * </UL>
 * The following chart indicates which options belong to which
 * configuration: 1 indicates that configuration j requires option i;
 * 0 means not so.  The chart also shows the capacity of each option
 * as well as the number of cars to build for each configuration.<br>
 * <br>
 * <samp>
 * option&nbsp;capacity&nbsp;&nbsp;&nbsp;&nbsp;configurations&nbsp;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0&nbsp;&nbsp;1&nbsp;&nbsp;2&nbsp;&nbsp;3&nbsp;&nbsp;4&nbsp;&nbsp;5<br>
 * &nbsp;0&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1/2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;1&nbsp;&nbsp;1<br>
 * &nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2/3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;1&nbsp;&nbsp;1&nbsp;&nbsp;0&nbsp;&nbsp;1<br>
 * &nbsp;2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1/3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;1&nbsp;&nbsp;0<br>
 * &nbsp;3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2/5&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;1&nbsp;&nbsp;0&nbsp;&nbsp;1&nbsp;&nbsp;0&nbsp;&nbsp;0<br>
 * &nbsp;4&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1/5&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;1&nbsp;&nbsp;0&nbsp;&nbsp;0&nbsp;&nbsp;0<br>
 * <br>
 * number&nbsp;of&nbsp;cars&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;&nbsp;1&nbsp;&nbsp;2&nbsp;&nbsp;2&nbsp;&nbsp;2&nbsp;&nbsp;2<br>
 * </samp>
 * <br>
 * For example, the chart indicates that option 1 can be put on
 * at most two cars for any sequence of three cars.  Option 1
 * is required by configurations 2, 3, and 5.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class CarSequence
{
  static Constrainer C = new Constrainer("CarSequence");
  static class Extractor{
    IntExpArray _vars = null;
    int _size;

    Extractor(IntExpArray array){
      _vars = array;
      _size = array.size();
    }

    IntExpArray extract(int initialNumber, int amount){
      if ((initialNumber + amount)>_size)
        amount = _size-initialNumber;
      IntExpArray tmp = new IntExpArray(C,amount);
      for (int i = initialNumber; i<initialNumber+amount; i++){
        tmp.set(_vars.get(i),i-initialNumber);
      }
      return tmp;
    }
  }

  static public void main (String[] args){
    try{
    IntExpArray cars = new IntExpArray(C,10,0,5,"cars");
//    final int nbConfs = 6;
    int[] demands = {1,1,2,2,2,2};

    int[][] options = {{0,4,5},
                       {2,3,5},
                       {0,4},
                       {0,1,3},
                       {2}};

    int[][] idleConfs = {{1,2,3},
                         {0,1,4},
                         {1,2,3,5},
                         {2,4,5},
                         {0,1,3,4,5}};

    int[][] optfreq = {{1,2},
                       {2,3},
                       {1,3},
                       {2,5},
                       {1,5}};

    IntExpArray expArray = new IntExpArray(C,cars.max()-cars.min() + 1);
    for (int optNum=0;optNum<options.length;optNum++){
//      int sequenceLength = optfreq[optNum][1];
      int nbConf = options[optNum].length;
      for (int seqStart=0;seqStart<(cars.size()-optfreq[optNum][1]);seqStart++){
        IntExpArray carSequence = new Extractor(cars).extract(seqStart,optfreq[optNum][1]);
        IntExpArray atMost = C.distribute(carSequence,options[optNum]);
        // configurations that include given option may be chosen
        // optfreq[optNum][0] times AT MOST
        for (int i=0;i<nbConf;i++){
          C.postConstraint(atMost.elementAt(i).le(optfreq[optNum][0]));
        }

        IntExpArray atLeast = C.distribute(carSequence,idleConfs[optNum]);
        // all others configurations may be chosen
        // optfreq[optNum][1] - optfreq[optNum][0] times AT LEAST
        C.postConstraint(atLeast.sum().ge(optfreq[optNum][1] - optfreq[optNum][0]));
      }
    }

    for (int i=0;i<expArray.size();i++){
      expArray.set(C.addIntVar(0,demands[i]),i);
    }

    C.distribute(cars,expArray);

    Goal gen = new GoalGenerate(cars);
    C.printInformation();

    if (C.execute(gen) == false)
      System.out.println("There are no solutions");
    else
      System.out.println(cars);
    }
    catch (Exception e){
      e.printStackTrace();
    }
  }
}
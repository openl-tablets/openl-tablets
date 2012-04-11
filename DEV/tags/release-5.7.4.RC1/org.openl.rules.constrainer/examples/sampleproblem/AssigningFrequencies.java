package sampleproblem;

import java.util.*;
import org.openl.ie.constrainer.*;
import org.openl.ie.constrainer.impl.*;

/**
 * Title: AssigningFrequencies<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * The problem is given here in the form of discrete data; that is,
 * each frequency is represented by a number that can be called its
 * channel number.  For practical purposes, the network is divided
 * into cells (this problem is an actual cellular phone problem).
 * In each cell, there is a transmitter which uses different
 * channels.  The shape of the cells have been determined, as well
 * as the precise location where the transmitters will be
 * installed.  For each of these cells, traffic requires a number
 * of frequencies.<br>
 * <br>
 * Between two cells, the distance between frequencies is given in
 * the matrix on the next page.<br>
 * <br>
 * The problem of frequency assignment is to avoid interference.
 * As a consequence, the distance between the frequencies within a
 * cell must be greater than 16.  To avoid inter-cell interference,
 * the distance must vary because of the geography.  In the
 * example, we're assuming that the same frequencies can be used in
 * the first cell and in the fourth cell, but we cannot use the same
 * frequencies in the first cell and the third cell.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class AssigningFrequencies
{
  // Data
  static final int nbCell    = 25;
  static final int nbAvailFreq   = 256;
  static final int[] nbChannel =
    { 8,6,6,1,4,4,8,8,8,8,4,9,8,4,4,10,8,9,8,4,5,4,8,1,1 };

  static final int[][] dist = {
    { 16,1,1,0,0,0,0,0,1,1,1,1,1,2,2,1,1,0,0,0,2,2,1,1,1 },
    { 1,16,2,0,0,0,0,0,2,2,1,1,1,2,2,1,1,0,0,0,0,0,0,0,0 },
    { 1,2,16,0,0,0,0,0,2,2,1,1,1,2,2,1,1,0,0,0,0,0,0,0,0 },
    { 0,0,0,16,2,2,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1,1 },
    { 0,0,0,2,16,2,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1,1 },
    { 0,0,0,2,2,16,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1,1 },
    { 0,0,0,0,0,0,16,2,0,0,1,1,1,0,0,1,1,1,1,2,0,0,0,1,1 },
    { 0,0,0,0,0,0,2,16,0,0,1,1,1,0,0,1,1,1,1,2,0,0,0,1,1 },
    { 1,2,2,0,0,0,0,0,16,2,2,2,2,2,2,1,1,1,1,1,1,1,0,1,1 },
    { 1,2,2,0,0,0,0,0,2,16,2,2,2,2,2,1,1,1,1,1,1,1,0,1,1 },
    { 1,1,1,0,0,0,1,1,2,2,16,2,2,2,2,2,2,1,1,2,1,1,0,1,1 },
    { 1,1,1,0,0,0,1,1,2,2,2,16,2,2,2,2,2,1,1,2,1,1,0,1,1 },
    { 1,1,1,0,0,0,1,1,2,2,2,2,16,2,2,2,2,1,1,2,1,1,0,1,1 },
    { 2,2,2,0,0,0,0,0,2,2,2,2,2,16,2,1,1,1,1,1,1,1,1,1,1 },
    { 2,2,2,0,0,0,0,0,2,2,2,2,2,2,16,1,1,1,1,1,1,1,1,1,1 },
    { 1,1,1,0,0,0,1,1,1,1,2,2,2,1,1,16,2,2,2,1,2,2,1,2,2 },
    { 1,1,1,0,0,0,1,1,1,1,2,2,2,1,1,2,16,2,2,1,2,2,1,2,2 },
    { 0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,2,2,16,2,2,1,1,0,2,2 },
    { 0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,16,2,1,1,0,2,2 },
    { 0,0,0,1,1,1,2,2,1,1,2,2,2,1,1,1,1,2,2,16,1,1,0,1,1 },
    { 2,0,0,0,0,0,0,0,1,1,1,1,1,1,1,2,2,1,1,1,16,2,1,2,2 },
    { 2,0,0,0,0,0,0,0,1,1,1,1,1,1,1,2,2,1,1,1,2,16,1,2,2 },
    { 1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,0,0,0,1,1,16,1,1 },
    { 1,0,0,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,1,2,2,1,16,2 },
    { 1,0,0,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,1,2,2,1,2,16 }
  };

  // end of Data

  static Constrainer C = new Constrainer("AssigningFrequencies");
  static IntExpArray freqUsage = new IntExpArray(C,nbAvailFreq,0,nbAvailFreq-1,"FrequencyUsage");
  static ArrayList channels = new ArrayList();

  static class LeastChannelClusterSelector implements IntVarSelector{
    public int select(){
      int curCapacity, minCapacity = IntVar.MAX_VALUE;
      int index = -1;
      int curDomain, minDomain = IntVar.MAX_VALUE;
      for (int i=0; i<channels.size(); i++){
         Channel chan = ((Channel)channels.get(i));
         if (!chan._frequency.bound()){
          curDomain = chan._frequency.size();
          if (curDomain < minDomain){
            minDomain = curDomain;
            index = i;
          }
          else
            if (curDomain == minDomain){
              curCapacity = nbChannel[chan._cell];
              if (curCapacity < minCapacity){
                minCapacity = curCapacity;
                index = i;
              }
            }
         }
      }
      return index;
    }
  } // end of LeastChannelClusterSelector

  static class MostUsedFreqSelector implements IntValueSelector{

    class MaxIndexFinder implements IntExp.IntDomainIterator{
      int _max = -1;
      int _maxIndex = 0;
      MaxIndexFinder(){
      }
      int getMaxIndex(){
        return _maxIndex;
      }
      public boolean doSomethingOrStop(int val) throws Failure{
        int current = freqUsage.get(val).min();
        if (current > _max){
          _max = current;
          _maxIndex = val;
        }
        return true;
      }
    } // end of MaxIndexFinder

    public int select(IntVar avar){
      try{
        IntExp.IntDomainIterator iter = new MaxIndexFinder();
        avar.iterateDomain(iter);
        return ((MaxIndexFinder)iter).getMaxIndex();
      }
      catch(Failure f){
        return -1;
      }
    }
  } // end of MostUsedFreqSelector

  static class ValueObserver extends org.openl.ie.constrainer.Observer
  {
    public int subscriberMask(){
      return ALL;
    }

    public void update(Subject exp, EventOfInterest interest)
                    throws Failure
    {

      IntEvent event = (IntEvent)interest;

      if (event.isValueEvent()){
         IntVar counter = (IntVar)freqUsage.get(event.max());
         int e = counter.min();
         counter.setMin(e+1);
      }
    }

    public Object master()
    {
      return null;
    }

  } // end of ValueObserver

  static class Channel{
    public IntVar _frequency;
    public int _cell;
    public Channel(IntVar frequency, int cell){
      _frequency = frequency;
      _cell = cell;
    }
  }
  public AssigningFrequencies()
  {
  }
  public static void main(String[] args)
  {
    try{

    int channelsTotal = 0;
    for (int i=0;i<nbChannel.length;i++){
      channelsTotal+=nbChannel[i];
    }

//    AssigningFrequencies assigningFrequencies1 = new AssigningFrequencies();
    IntExpArray frequencies = new IntExpArray(C,channelsTotal);

    int next = 0;
    for (int i=0;i<nbChannel.length;i++){
      for (int j=0;j<nbChannel[i];j++){
        IntVar freq = C.addIntVar(0,nbAvailFreq-1,"Cell_"+i+"["+j+"]",IntVar.DOMAIN_BIT_FAST);
        freq.attachObserver(new ValueObserver());
        int cell = i;
        channels.add(next,new Channel(freq,cell));
        frequencies.set(freq,next);
        next++;
        // posting table's constraints
        for (int k=0;k<next-1;k++){
          Channel chan = (Channel)channels.get(k);
          int anotherCell = chan._cell;
          IntVar anotherFreq = chan._frequency;
          if (dist[cell][anotherCell] > 0)
            C.postConstraint(freq.sub(anotherFreq).abs().ge(dist[cell][anotherCell]));
        }
      }
    }

    C.printInformation();
    Goal gen = new GoalGenerate(frequencies,
                                new LeastChannelClusterSelector(),
                                new MostUsedFreqSelector());
    C.execute(gen);
    next = 0;
    for (int i =0;i<nbCell;i++){
      for (int j=0;j<nbChannel[i];j++){
        System.out.print(frequencies.get(next).name() +":"+frequencies.get(next).value() + "   ");
        next++;
      }
      System.out.println();
    }

    int counter = 0;
    for (int i = 0; i<freqUsage.size();i++){
      if (freqUsage.get(i).min() > 0)
        counter++;
    }
    System.out.println("Total frequency usage: "+counter);


    }
    catch(Failure f){
      System.out.println("There are no solutions");
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}
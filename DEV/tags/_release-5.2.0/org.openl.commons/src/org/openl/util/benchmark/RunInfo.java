package org.openl.util.benchmark;


public class RunInfo
{

    public RunInfo(long runs, long ms)
    {
      this.times = runs;
      this.ms = ms;
    }

    long times;
    long ms;

    public double avgRunms()
    {
      return ms/(double) times;
    }

    public String toString()
    {
      return "[" + times + " : " + ms + "]";
    }

}
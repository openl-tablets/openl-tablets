package org.openl.util.benchmark;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkInfo
{

    BenchmarkUnit unit;
    long firstRunms;
    List<RunInfo> runs = new ArrayList<RunInfo>();
    Throwable error;
    String name;

    public static BenchmarkOrder[] order(BenchmarkInfo[] bi)
    {
	ArrayList<BenchmarkOrder> list = new ArrayList<BenchmarkOrder>();
	for (int i = 0; i < bi.length; i++)
	{
	    if (bi[i] != null)
		list.add(new BenchmarkOrder(i, bi[i]));
	}

	Collections.sort(list);
	BenchmarkOrder[] bo = list
		.toArray(new BenchmarkOrder[0]);
	for (int i = 0; i < bo.length; i++)
	{
	    bo[i].setOrder(i + 1);
	    bo[i].setRatio(bo[0].info.drunsunitsec()
		    / bo[i].info.drunsunitsec());
	}

	BenchmarkOrder[] bores = new BenchmarkOrder[bi.length];
	for (int i = 0; i < bo.length; i++)
	{
	    bores[bo[i].getIndex()] = bo[i];
	}
	return bores;
    }

    /**
     * @param t
     * @param bu
     */
    public BenchmarkInfo(Throwable t, BenchmarkUnit bu, String name)
    {
	this.error = t;
	this.unit = bu;
	this.name = name;
    }

    static public String printDouble(double d)
    {
	return printDouble(d, 6);
    }

    static public String printDouble(double d, int decimals)
    {
	NumberFormat fmt = NumberFormat.getNumberInstance();
	fmt.setMaximumFractionDigits(decimals);
	fmt.setMinimumFractionDigits(decimals);
	return fmt.format(d);
    }

    static public String printLargeDouble(double d)
    {
	DecimalFormat fmt = new DecimalFormat("#,##0.00");
	return fmt.format(d);
    }

    String unitDescription()
    {
	if (unit.getDescription() == null)
	    return unit.getName();
	return unit.getDescription();
    }

    public String toString()
    {

	// return printDouble(avg()) + " ms/run" + '\t' +
	// printLargeDouble(1000/avg()) + "runs/sec" + " \t" + runs + " \t" +
	// unitDescription() + " 1: " + printDouble(firstRunms) ;
	return msrun() + " ms/run" + '\t' + runssec() + " runs/sec" + " \t"
		+ " First Run: " + printDouble(firstRunms) + "ms";
    }

    public String msrun()
    {
	return printDouble(avg());
    }

    public String runssec()
    {
	return printLargeDouble(1000 / avg());
    }

    public String msrununit()
    {
	return printDouble(avg() / unit.nUnitRuns());
    }

    public String runsunitsec()
    {
	return printLargeDouble(drunsunitsec());
    }

    public double drunsunitsec()
    {
	return 1000 * unit.nUnitRuns() / avg();
    }

    public String unitName()
    {
	return unit.nUnitRuns() == 1 ? unit.unitName()[0] : unit.unitName()[1];
    }

    public double avg()
    {
	int n = 0;
	double sum = 0;
	for (int i = 0; i < runs.size(); ++i)
	{
	    RunInfo run = runs.get(i);
	    n += run.times;
	    sum += run.ms;
	}

	return sum / n;
    }

    public double deviation()
    {
	int n = 0;
	double sum = 0;
	double sum2 = 0;
	for (int i = 0; i < runs.size(); ++i)
	{
	    RunInfo run = runs.get(i);
	    n += run.times;
	    sum += run.ms;

	    sum2 += run.avgRunms() * run.ms;
	}

	return Math.sqrt((sum2 - sum / n * sum) / n);

    }

    public Throwable getError()
    {
	return this.error;
    }

    public String getName()
    {
	return this.name;
    }

    public BenchmarkUnit getUnit()
    {
	return this.unit;
    }

}
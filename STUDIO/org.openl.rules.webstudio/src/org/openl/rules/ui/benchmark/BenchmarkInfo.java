package org.openl.rules.ui.benchmark;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

public class BenchmarkInfo {

    BenchmarkUnit unit;
    long firstRunms;
    Throwable error;
    String name;
    long timesSum;
    long msSum;
    double ms2Sum;

    public void collect(long times, long ms) {
        timesSum += times;
        msSum += ms;
        ms2Sum += (double) ms / times * ms;
    }

    public static BenchmarkOrder[] order(BenchmarkInfo[] bi) {
        ArrayList<BenchmarkOrder> list = new ArrayList<>();
        for (int i = 0; i < bi.length; i++) {
            if (bi[i] != null) {
                list.add(new BenchmarkOrder(i, bi[i]));
            }
        }

        Collections.sort(list);
        BenchmarkOrder[] bo = list.toArray(new BenchmarkOrder[0]);
        for (int i = 0; i < bo.length; i++) {
            bo[i].setOrder(i + 1);
            bo[i].setRatio(bo[0].info.drunsunitsec() / bo[i].info.drunsunitsec());
        }

        BenchmarkOrder[] bores = new BenchmarkOrder[bi.length];
        for (int i = 0; i < bo.length; i++) {
            bores[bo[i].getIndex()] = bo[i];
        }
        return bores;
    }

    public static String printDouble(double d) {
        if (d >= 1000) {
            DecimalFormat fmt = new DecimalFormat("#,##0");
            return fmt.format(d);
        }
        if (d >= 1) {
            DecimalFormat fmt = new DecimalFormat("#,##0.00");
            return fmt.format(d);
        }

        if (d > 0.1)
            return printDouble(d, 3);
        if (d > 0.01)
            return printDouble(d, 4);
        if (d > 0.001)
            return printDouble(d, 5);

        return printDouble(d, 6);
    }

    public static String printDouble(double d, int decimals) {
        NumberFormat fmt = NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(decimals);
        fmt.setMinimumFractionDigits(decimals);
        return fmt.format(d);
    }

    public static String printLargeDouble(double d) {
        DecimalFormat fmt = new DecimalFormat("#,##0");
        return fmt.format(d);
    }

    /**
     * @param t
     * @param bu
     */
    public BenchmarkInfo(Throwable t, BenchmarkUnit bu, String name) {
        error = t;
        unit = bu;
        this.name = name;
    }

    public double avg() {
        return (double) msSum / timesSum;
    }

    public double deviation() {
        return Math.sqrt((ms2Sum - (double) msSum / timesSum * msSum) / timesSum);
    }

    public double drunsunitsec() {
        return 1000 * unit.nUnitRuns() / avg();
    }

    public Throwable getError() {
        return error;
    }

    public String getName() {
        return name;
    }

    public BenchmarkUnit getUnit() {
        return unit;
    }

    public String msrun() {
        return printDouble(avg());
    }

    public String msrununit() {
        return printDouble(avg() / unit.nUnitRuns());
    }

    public String runssec() {
        return printLargeDouble(1000 / avg());
    }

    public String runsunitsec() {
        return printLargeDouble(drunsunitsec());
    }

    @Override
    public String toString() {
        return getName() + " = \t" + msrun() + " ms/run\t" + runssec() + " runs/sec\t First Run: " + firstRunms + "ms";
    }
}

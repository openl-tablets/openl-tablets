package org.openl.util.benchmark;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.Log;

public class Benchmark {

    public List<BenchmarkInfo> measureAllInList(BenchmarkUnit[] units, int ms) throws Exception {
        List<BenchmarkInfo> list = new ArrayList<BenchmarkInfo>();
        for (BenchmarkUnit bu: units) {
            Log.info("Benchmarking Unit " + bu.getName());
            BenchmarkInfo bi = runUnit(bu, ms);
            list.add(bi);
        }
        return list;
    }

    public BenchmarkInfo runUnit(BenchmarkUnit bu, int ms) throws Exception {

        BenchmarkInfo bi = new BenchmarkInfo(null, bu, bu.getName());
        bi.firstRunms = bu.millisecondsToRun();

        int minMillis = ms == -1 ? bu.getMinms() : ms;
        long runs = bu.getMinRuns();
        while (true) {
            long time = bu.millisecondsToRun(runs);
            if (time > minMillis || runs >= Integer.MAX_VALUE) {
                bi.collect(runs, time);
                return bi;
            }

            // Calculate a growth rate for runs
            // division by zero is Double.POSITIVE_INFINITY
            double mult = Math.min(200.0, 1.1 * minMillis / time);
            // Calculate new quantity of runs
            runs = Math.max(runs + 1, (long) (runs * mult));
            // To avoid overflowing of Integer bits
            runs = Math.min(runs, Integer.MAX_VALUE);
        }
    }
}

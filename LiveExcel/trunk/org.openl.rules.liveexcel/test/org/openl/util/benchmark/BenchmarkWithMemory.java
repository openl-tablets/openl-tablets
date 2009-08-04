package org.openl.util.benchmark;

import java.util.HashMap;

import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;

public class BenchmarkWithMemory extends Benchmark {

    public BenchmarkWithMemory(BenchmarkUnit[] units) {
        super(units);
    }

    public BenchmarkInfo runUnit(BenchmarkUnit bu, int ms, boolean once) throws Exception {

        if (_measurements == null) {
            _measurements = new HashMap<String, BenchmarkInfo>();
        }

        BenchmarkInfo bi = _measurements.get(bu.getName());

        if (bi == null) {
            bi = new BenchmarkInfoWithMemory(null, bu, bu.getName());

            long previouslyMemoryUsed = getUsedMemorySizeBeforeTest();
            bi.firstRunms = bu.millisecondsToRun();
            ((BenchmarkInfoWithMemory) bi).setMemoryUsed(getUsedMemorySizeAfterTest() - previouslyMemoryUsed);
            bi.runs.add(new RunInfo(1, bi.firstRunms));
            _measurements.put(bu.getName(), bi);
        }

        if (once) {
            return bi;
        }

        RunInfo info = makeRun(bu, bu.getMinRuns(), ms);
        bi.runs.add(info);
        return bi;
    }

    public static long getUsedMemorySizeBeforeTest() {
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public static long getUsedMemorySizeAfterTest() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
}

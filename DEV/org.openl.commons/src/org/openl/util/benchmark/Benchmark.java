package org.openl.util.benchmark;

import org.openl.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Benchmark {
    BenchmarkUnit[] _units;

    HashMap<String, BenchmarkInfo> _measurements = null;

    public Benchmark(BenchmarkUnit[] units) {
        _units = units;
    }

    BenchmarkUnit findUnit(String name) {
        for (int i = 0; i < _units.length; ++i) {
            if (_units[i].getName().equals(name)) {
                return _units[i];
            }
        }

        throw new RuntimeException("Unit " + name + " not found");
    }

    RunInfo makeRun(BenchmarkUnit bu, int minRuns, int ms) throws Exception {

        int minMillis = ms == -1 ? bu.getMinms() : ms;
        long runs = minRuns;
        while (true) {
            long time = bu.millisecondsToRun(runs);
            if (time > minMillis || runs > Integer.MAX_VALUE) {
                return new RunInfo(runs, time);
            }

            if (time <= 0) {
                time = 1;
            }

            double mult = Math.min(200, (minMillis) * 1.1 / time);

            long newRuns = (long) Math.ceil(runs * mult);
            runs = Math.max(runs + 1, newRuns);

        }
    }

    public List<BenchmarkInfo> measureAllInList(int ms) throws Exception {
        _measurements = new HashMap<String, BenchmarkInfo>();
        List<BenchmarkInfo> list = new ArrayList<BenchmarkInfo>();
        for (int i = 0; i < _units.length; ++i) {
            list.add(measureUnit(_units[i], ms));
        }

        return list;
    }

    public BenchmarkInfo measureUnit(BenchmarkUnit bu, int ms) throws Exception {

        Log.info("Benchmarking Unit " + bu.getName());

        if (_measurements == null) {
            _measurements = new HashMap<String, BenchmarkInfo>();
        }

        satisfyPreconditions(bu);
        return runUnit(bu, ms, false);
    }

    public BenchmarkInfo runUnit(BenchmarkUnit bu, int ms, boolean once) throws Exception {

        if (_measurements == null) {
            _measurements = new HashMap<String, BenchmarkInfo>();
        }

        BenchmarkInfo bi = _measurements.get(bu.getName());

        if (bi == null) {
            bi = new BenchmarkInfo(null, bu, bu.getName());

            bi.firstRunms = bu.millisecondsToRun();
            _measurements.put(bu.getName(), bi);
        }

        if (once) {
            return bi;
        }

        RunInfo info = makeRun(bu, bu.getMinRuns(), ms);
        bi.collect(info.times, info.ms);
        return bi;
    }

    public void satisfyPreconditions(BenchmarkUnit bu) throws Exception {
        String[] names = bu.performAfter();
        for (int i = 0; i < names.length; ++i) {
            BenchmarkUnit prev = findUnit(names[i]);
            BenchmarkInfo bi = _measurements.get(bu.getName());
            if (bi == null) {
                satisfyPreconditions(prev);
                runUnit(prev, 1, true);
            }

        }

    }
}

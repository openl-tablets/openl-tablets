package org.openl.util.benchmark;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.util.Log;

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

    static public long getCleanMemorySize() {
        // System.gc();
        long prevUsedMemory = getUsedMemorySize();
        while (true) {
            System.gc();
            if (prevUsedMemory - getUsedMemorySize() < 1024)
                return getUsedMemorySize();
            prevUsedMemory = getUsedMemorySize();
        }

    }

    static public long getUsedMemorySize() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    RunInfo makeRun(BenchmarkUnit bu, int minRuns, int ms) throws Exception {

        int minMillis = ms == -1 ? bu.getMinms() : ms;
        int runs = minRuns;
        while (true) {
            long time = 0;
            if (bu.isTestMemory()) {
                long memoryBefore = getCleanMemorySize();
                time = bu.millisecondsToRun(runs);
                long memoryDirtyAfter = getUsedMemorySize();
                long memoryCleanAfter = getCleanMemorySize();
                if (time > minMillis) {
                    return new RunInfo(runs, time, memoryBefore, memoryDirtyAfter, memoryCleanAfter);
                }
            }
            else
            {
                time = bu.millisecondsToRun(runs);
                if (time > minMillis) {
                    return new RunInfo(runs, time);
                }
            }    
                

            if (time <= 0) {
                time = 1;
            }

            double mult = Math.min(200, (minMillis) * 1.1 / time);

            int newRuns = (int) Math.ceil(runs * mult);
            runs = Math.max(runs + 1, newRuns);

        }
    }

    public String makeTableHeader(String separator) {
        String res = "";
        for (int i = 0; i < _units.length; ++i) {
            res += _units[i].getName() + separator;
        }
        return res;
    }

    public String makeTableRow(Map<String, BenchmarkInfo> map, String separator) {
        String res = "";
        for (int i = 0; i < _units.length; ++i) {
            BenchmarkInfo info = map.get(_units[i].getName());
            res += BenchmarkInfo.printDouble(info.avg()) + separator;
        }

        return res;
    }

    public Map<String, BenchmarkInfo> measureAll(int ms) throws Exception {
        _measurements = new HashMap<String, BenchmarkInfo>();
        for (int i = 0; i < _units.length; ++i) {
            measureUnit(_units[i], ms);
        }

        return _measurements;
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

    public BenchmarkInfo measureUnit(String name, int ms) throws Exception {
        return measureUnit(findUnit(name), ms);
    }

    public void printResult(Map<String, BenchmarkInfo> map, PrintStream ps) {
        for (int i = 0; i < _units.length; ++i) {
            BenchmarkInfo info = map.get(_units[i].getName());
            ps.println(info);
        }
    }

    public void profileUnit(String name, int times) throws Exception {
        if (_measurements == null) {
            _measurements = new HashMap<String, BenchmarkInfo>();
        }

        BenchmarkUnit bu = findUnit(name);
        satisfyPreconditions(bu);

        bu.millisecondsToRun(times == -1 ? 1 : times);
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
        bi.runs.add(info);
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
/**
 * Created Jul 7, 2007
 */
package org.openl.util.benchmark;

/**
 * @author snshor
 *
 */
public class Profiler {

    public static abstract class Unit {
        public abstract BenchmarkUnit makeBenchMarkUnit() throws Exception;
    }
    int N = 10;

    int minms = 10000;

    public Profiler(int N) {
        this.N = N;
    }

    public void profileUnit(Unit unit) throws Exception {
        long start = System.currentTimeMillis();
        BenchmarkUnit bu = unit.makeBenchMarkUnit();
        long end = System.currentTimeMillis();

        long initTime = end - start;

        int ms = (int) Math.max(initTime * N, minms);

        System.out.println("Startup time " + initTime + "ms");

        if (N < 2) {
            bu.runNtimes(1);
            return;
        }

        System.out.println("Going to profile for " + ms + "ms");

        BenchmarkUnit[] buu = { bu };

        BenchmarkInfo bi = new Benchmark(buu).runUnit(bu, ms, false);

        System.out.println(bi);

    }

}

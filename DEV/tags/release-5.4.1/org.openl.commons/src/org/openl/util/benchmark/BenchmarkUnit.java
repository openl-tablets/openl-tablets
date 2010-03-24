package org.openl.util.benchmark;

public abstract class BenchmarkUnit {

    /**
     * return not null if this is a part of the chain; it means that multiple
     * executions(iterations) can be done only on the whole chain from the very
     * beginning
     */

    public String[] chain() {
        return new String[0];
    }

    public String getDescription() {
        return null;
    }

    public int getMinms() {
        return 1000;
    }

    public int getMinRuns() {
        return 1;
    }

    public String getName() {
        String name = getNameSpecial();
        if (name != null) {
            return name;
        }

        String className = getClass().getName();

        int idx = className.lastIndexOf('$');
        if (idx >= 0) {
            return className.substring(idx + 1);
        }

        idx = className.lastIndexOf('.');
        if (idx >= 0) {
            return className.substring(idx + 1);
        }

        return className;

    }

    protected String getNameSpecial() {
        return null;
    }

    public long millisecondsToRun() throws Exception {
        long start = System.nanoTime();
        runNtimes(1);
        long end = System.nanoTime();
        return ((end - start) + 500L * 1000) / 1000000;
    }

    public long millisecondsToRun(int times) throws Exception {
        long start = System.nanoTime();
        runNtimes(times);
        long end = System.nanoTime();
        return (end - start + 500000) / 1000000;
    }

    public long millisecondsToRun1() throws Exception {
        long start = System.currentTimeMillis();
        runNtimes(1);
        long end = System.currentTimeMillis();
        return end - start;
    }

    public long millisecondsToRun1(int times) throws Exception {
        long start = System.currentTimeMillis();
        runNtimes(times);
        long end = System.currentTimeMillis();
        return end - start;
    }

    public int nUnitRuns() {
        return 1;
    }

    /**
     * Returns a list of units that must be executed before this unit or null
     */

    public String[] performAfter() {
        return new String[0];
    }

    /**
     * This is the method you usually want to redefine. There can be cases
     * though when you want to redefine runNtimes.
     *
     * @throws Exception
     */
    protected abstract void run() throws Exception;

    public void runNtimes(int times) throws Exception {
        for (int i = 0; i < times; ++i) {
            run();
        }
    }

    public String[] unitName() {
        return new String[] { "Run", "Runs" };
    }
    
    public boolean isTestMemory()
    {
        return false;
    }

}
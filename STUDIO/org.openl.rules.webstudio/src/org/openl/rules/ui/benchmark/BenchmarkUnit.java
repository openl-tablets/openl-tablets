package org.openl.rules.ui.benchmark;

public abstract class BenchmarkUnit {

    /**
     * return not null if this is a part of the chain; it means that multiple executions(iterations) can be done only on
     * the whole chain from the very beginning
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

    public long millisecondsToRun() throws Exception {
        return millisecondsToRun(1);
    }

    public long millisecondsToRun(long times) throws Exception {
        long start = System.nanoTime();
        runNtimes(times);
        long end = System.nanoTime();
        return (end - start + 500000) / 1000000;
    }

    public int nUnitRuns() {
        return 1;
    }

    /**
     * This is the method you usually want to redefine. There can be cases though when you want to redefine runNtimes.
     * 
     * @throws Exception
     */
    protected abstract void run() throws Exception;

    public void runNtimes(long times) throws Exception {
        for (long i = 0; i < times; ++i) {
            run();
        }
    }
}

package org.openl.util.benchmark;

public class RunInfo {

    long times;

    long ms;
    public RunInfo(long runs, long ms) {
        times = runs;
        this.ms = ms;
    }

    public double avgRunms() {
        return ms / (double) times;
    }

    @Override
    public String toString() {
        return "[" + times + " : " + ms + "]";
    }

}
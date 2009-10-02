package org.openl.util.benchmark;

public class RunInfo {

    long times;

    long ms;

    public RunInfo(long times, long ms, long memoryBefore, long memoryDirtyAfter, long memoryCleanAfter) {
        super();
        this.times = times;
        this.ms = ms;
        this.memoryBefore = memoryBefore;
        this.memoryDirtyAfter = memoryDirtyAfter;
        this.memoryCleanAfter = memoryCleanAfter;
    }

    public RunInfo(long runs, long ms) {
        times = runs;
        this.ms = ms;
    }

    public double avgRunms() {
        return ms / (double) times;
    }

    public double avgTimesSec() {
        return  (double) times * 1000 / ms ;
    }

    @Override
    public String toString() {
        return "[" + times + " : " + ms + "]";
    }

    long memoryBefore;
    long memoryDirtyAfter;
    long memoryCleanAfter;

    public double bytesPerRun()
    {
        return (memoryDirtyAfter - memoryBefore)/(double)times;
    }
    
    public double leakedBytesPerRun()
    {
        return (memoryCleanAfter - memoryBefore)/(double)times;
    }

    public double usedBytes()
    {
        return (memoryDirtyAfter - memoryBefore);
    }

    public double leakedBytes()
    {
        return (memoryCleanAfter - memoryBefore);
    }
    
}
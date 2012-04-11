package org.openl.util.benchmark;

public class BenchmarkInfoWithMemory extends BenchmarkInfo{

    private long memoryUsed; 
    
    public BenchmarkInfoWithMemory(Throwable t, BenchmarkUnit bu, String name) {
        super(t, bu, name);
    }
    
    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

}

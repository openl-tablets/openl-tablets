package org.openl.rules.perf.load;

import java.util.List;

import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.rules.table.SubGridTable;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;

public class Loadtest1 {
    
    static interface Dummy1
    {
    }
    
    static String src = "test/rules/perf/load/Dummy1.xls";
    
    public static void main(String[] args) throws Exception {
        
        
        BenchmarkUnit[] bu = { 
                
                new LoadDummy1()
                
        };

        List<BenchmarkInfo> res = new Benchmark(bu).measureAllInList(1000);

        for (BenchmarkInfo bi : res) {

            System.out.println(bi);

        }
        
        
    }
    
    
    
    static class LoadDummy1 extends BenchmarkUnit
    {

        LoadDummy1() throws Exception
        {
            run();
        }
        
        @Override
        protected void run() throws Exception {
            RuleEngineFactory<Dummy1> re = new RuleEngineFactory<Dummy1>(src, Dummy1.class);
            re.setExecutionMode(true);
            Dummy1 d1 = re.makeInstance();
        }
        
        
        public int getMinRuns()
        {
            return 10;
        }
        
    }

}

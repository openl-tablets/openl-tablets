package org.openl.rules.perf.load;

import java.util.List;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;

public class LoadTest1 {

    static interface Dummy1 {
    }

    static String src = "test/rules/perf/load/Dummy1.xls";

    public static void main(String[] args) throws Exception {

        BenchmarkUnit[] bu = {

        new LoadDummy1()

        };

        List<BenchmarkInfo> res = new Benchmark(null).measureAllInList(bu, 1000);

        for (BenchmarkInfo bi : res) {

            System.out.println(bi);

        }

    }

    static class LoadDummy1 extends BenchmarkUnit {

        LoadDummy1() throws Exception {
            run();
        }

        @Override
        protected void run() throws Exception {
            RulesEngineFactory<Dummy1> engineFactory = new RulesEngineFactory<Dummy1>(src, Dummy1.class);
            engineFactory.setExecutionMode(true);
            engineFactory.newEngineInstance();
        }

        public int getMinRuns() {
            return 10;
        }

    }

}

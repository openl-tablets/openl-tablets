package org.openl.rules.testmethod;

import java.io.File;

import org.junit.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;

public class BenchmarkPerformanceTest extends TestCase {

    private static final String FILE_NAME = "test/rules/testmethod/Tutorial_5.xls";

    public interface ITest {
        TestUnitsResults largeTableTestTestAll();

        TestUnitsResults largeTableIndTestTestAll();
    }

    ITest instance;
    IEngineWrapper iw;
    IOpenClass type;

    @Override
    protected void setUp() throws Exception {
        File xlsFile = new File(FILE_NAME);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);

        instance = testHelper.getInstance();

        iw = (IEngineWrapper) instance;
        DynamicObject dobj = (DynamicObject) iw.getInstance();

        type = dobj.getType();
    }

    @Test
    public void testBenchmark1() throws Exception {
        measureRatio("largeTableTestTestAll", "largeTableIndTestTestAll", 1);  //50 NOT ANYMORE :)

        measureRatio("ampmTo24TestTestAll", "ampmTo24Ind1TestTestAll", 1);
        measureRatio("ampmTo24TestTestAll", "ampmTo24Ind2TestTestAll", 3);

        measureRatio("regionTestTestAll", "regionIndTestTestAll", 3);

        measureRatio("driverPremiumTestTestAll", "driverPremiumIndTestTestAll", 6);

    }

    void measureRatio(String test1, String test2, int ratioAtLeast) throws Exception {

        BenchmarkInfo biSlow = benchmark(type, test1, instance, iw);
        System.out.println(biSlow);
        BenchmarkInfo biFast = benchmark(type, test2, instance, iw);
        System.out.println(biFast);

        double ratio = biSlow.avg() / biFast.avg();
        System.out.println("Ratio " + test1 + " / " + test2 + " = " + ratio);

        Assert.assertTrue("Indexed should be much faster than non-indexed", ratio * 1.1 > ratioAtLeast);

    }

    BenchmarkInfo benchmark(IOpenClass type, String name, Object instance, IEngineWrapper iw) throws Exception {

        IOpenMethod m = type.getMethod(name, IOpenClass.EMPTY);

        IBenchmarkableMethod bm = (IBenchmarkableMethod) m;

        IBenchmarkableMethod.BMBenchmarkUnit bu = new IBenchmarkableMethod.BMBenchmarkUnit(bm, instance, null,
                iw.getRuntimeEnv());

        BenchmarkUnit[] buu = { bu };
        BenchmarkInfo bi = new Benchmark(buu).measureUnit(bu, 3000);

        return bi;

    }

}

package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

@ManagedBean
@SessionScoped
public class BenchmarkBean {

    @ManagedProperty("#{runTestHelper}")
    private RunTestHelper runTestHelper;

    private List<BenchmarkInfoView> benchmarks = new ArrayList<>();
    private List<BenchmarkInfoView> comparedBenchmarks = Collections.emptyList();
    private List<BenchmarkInfoView> benchmarkOrders;

    public void setRunTestHelper(RunTestHelper runTestHelper) {
        this.runTestHelper = runTestHelper;
    }

    private boolean isTestForOverallTestSuiteMethod(TestSuite testSuite) {
        return testSuite.getTestSuiteMethod() != null && testSuite.getNumberOfTests() == testSuite.getTestSuiteMethod()
            .getNumberOfTestsCases();
    }

    public void addLastBenchmark() {
        TestSuite testSuite = runTestHelper.getTestSuite();
        String testSuiteUri = testSuite.getUri();
        WebStudio studio = WebStudioUtils.getWebStudio();
        IOpenMethod table = studio.getModel().getMethod(testSuiteUri);
        String tableId = TableUtils.makeTableId(testSuiteUri);
        String testName = TableSyntaxNodeUtils.getTestName(table);
        String testInfo = ProjectHelper.getTestInfo(testSuite);
        if (isTestForOverallTestSuiteMethod(testSuite)) {
            IOpenClass openClass = studio.getModel().getCompiledOpenClass().getOpenClassWithErrors();
            ToLongFunction<Integer> bu = times -> testSuite.invokeSequentially(openClass, times).getExecutionTime();
            BenchmarkInfoView biv = runBenchmark(tableId,
                testName,
                testInfo,
                bu,
                new ParameterWithValueDeclaration[0],
                testSuite.getNumberOfTests());
            benchmarks.add(0, biv);
        } else {
            for (int i = 0; i < testSuite.getNumberOfTests(); i++) {
                IOpenClass openClass = studio.getModel().getCompiledOpenClass().getOpenClassWithErrors();
                int numTest = i;
                ToLongFunction<Integer> bu = times -> testSuite.executeTest(openClass, numTest, times)
                    .getExecutionTime();

                ParameterWithValueDeclaration[] params = testSuite.getTest(i).getExecutionParams();
                BenchmarkInfoView biv = runBenchmark(tableId, testName, testInfo, bu, params, 1);
                benchmarks.add(0, biv);
            }
        }
        comparedBenchmarks = Collections.emptyList();
    }

    public String compare() {
        List<BenchmarkInfoView> bi = new ArrayList<>();
        for (BenchmarkInfoView biv : benchmarks) {
            if (biv.isSelected()) {
                bi.add(biv);
            }
        }
        comparedBenchmarks = bi;
        benchmarkOrders = new ArrayList<>(bi);
        benchmarkOrders.sort((o1, o2) -> (int) (o2.drunsunitsec() - o1.drunsunitsec()));
        return null;
    }

    public String delete() {
        benchmarks.removeIf(BenchmarkInfoView::isSelected);
        comparedBenchmarks = Collections.emptyList();
        return null;
    }

    public List<BenchmarkInfoView> getBenchmarks() {
        return benchmarks;
    }

    public boolean isAnyBencmarkSelected() {
        for (BenchmarkInfoView bi : benchmarks) {
            if (bi.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public boolean getAllBencmarkSelected() {
        if (benchmarks.isEmpty()) {
            return false;
        }
        for (BenchmarkInfoView bi : benchmarks) {
            if (!bi.isSelected()) {
                return false;
            }
        }
        return true;
    }

    public void setAllBencmarkSelected(boolean allBencmarkSelected) {
        for (BenchmarkInfoView bi : benchmarks) {
            bi.setSelected(allBencmarkSelected);
        }
    }

    public List<BenchmarkInfoView> getComparedBenchmarks() {
        return comparedBenchmarks;
    }

    public int getComparedOrder(BenchmarkInfoView bi) {
        return benchmarkOrders.indexOf(bi) + 1;
    }

    public String getComparedRatio(BenchmarkInfoView bi) {
        double rated = bi.drunsunitsec();
        double base = benchmarkOrders.get(0).drunsunitsec();
        return BenchmarkInfoView.printDouble(base / rated, 2);
    }

    public int getI(BenchmarkInfoView bi) {
        return benchmarks.indexOf(bi) + 1;
    }

    // The lowest total time for benchmark.
    private static final long MIN_NANOS = 3 * 1_000_000_000L;

    private BenchmarkInfoView runBenchmark(String tableId,
            String testName,
            String testInfo,
            ToLongFunction<Integer> bu,
            ParameterWithValueDeclaration[] params,
            int nUnitRuns) {

        int runs = 1;
        while (true) {
            long time = bu.applyAsLong(runs);// run test
            if (time > MIN_NANOS || runs >= Integer.MAX_VALUE) {
                return new BenchmarkInfoView(runs, time, nUnitRuns, tableId, testName, testInfo, params);
            }

            // Calculate a growth rate for runs
            // division by zero is Double.POSITIVE_INFINITY
            double mult = Math.min(200.0, 1.1 * MIN_NANOS / time);
            // Calculate new quantity of runs
            runs = Math.max(runs + 1, (int) (runs * mult));
        }
    }
}

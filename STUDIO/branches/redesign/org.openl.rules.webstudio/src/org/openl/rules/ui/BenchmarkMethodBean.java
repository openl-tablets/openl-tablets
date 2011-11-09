package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.html.HtmlDataTable;

import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkOrder;

@ManagedBean
@SessionScoped
public class BenchmarkMethodBean {
    private WebStudio studio;
    private ArrayList<BenchmarkInfo> benchmarkResults = new ArrayList<BenchmarkInfo>();
    private BenchmarkInfo[] comparedBenchmarks = new BenchmarkInfo[0];
    private BenchmarkOrder[] benchmarkOrders;
    private boolean[] bencmarkSelected;
    private HtmlDataTable htmlDataTableBM;
    private HtmlDataTable htmlDataTableBMCompared;

    
    private boolean isTestForOverallTestSuiteMethod(TestSuite testSuite) {
        if (testSuite.getTestSuiteMethod() != null && testSuite.getNumberOfTests() == testSuite.getTestSuiteMethod()
            .getNumberOfTests()) {
            return true;
        } else {
            return false;
        }
    }

    public void addLastBenchmark() {
        studio = WebStudioUtils.getWebStudio();
        TestSuite testSuite = studio.getModel().popLastTest();
        if (isTestForOverallTestSuiteMethod(testSuite)) {
            try {
                BenchmarkInfo buLast = studio.getModel().benchmarkTestsSuite(testSuite, 3000);
                studio.addBenchmark(buLast);
                benchmarkResults.add(buLast);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        } else {
            for (int i = 0; i < testSuite.getNumberOfTests(); i++) {
                try {
                    BenchmarkInfo buLast = studio.getModel().benchmarkSingleTest(testSuite, i, 3000);
                    studio.addBenchmark(buLast);
                    benchmarkResults.add(buLast);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }

            }
        }
        comparedBenchmarks = new BenchmarkInfo[0];
    }

    public String compare() {
        compareBenchmarks();
        return null;
    }

    private void compareBenchmarks() {
        BenchmarkInfo[] benchmarks = studio.getBenchmarks();
        comparedBenchmarks = new BenchmarkInfo[benchmarks.length];
        for (int i = 0; i < benchmarks.length; ++i) {
            if (bencmarkSelected[benchmarkResults.indexOf(benchmarks[i])]) {
                comparedBenchmarks[i] = benchmarks[i];
            }
        }
        benchmarkOrders = BenchmarkInfo.order(comparedBenchmarks);
    }

    public String delete() {
        deleteBenchmark();
        return null;
    }

    private void deleteBenchmark() {
        BenchmarkInfo[] benchmarks = studio.getBenchmarks();
        for (int i = benchmarks.length - 1; i >= 0; i--) {
            int indexOfBenchmark = benchmarkResults.indexOf(benchmarks[i]);
            if (bencmarkSelected[indexOfBenchmark]) {
                studio.removeBenchmark(i);
                benchmarkResults.remove(i);
            }
        }
        comparedBenchmarks = new BenchmarkInfo[0];
    }

    public int getBenchmarkCount() {
        return benchmarkResults.size() + 1;
    }

    public List<BenchmarkInfo> getBenchmarks() {
        studio = WebStudioUtils.getWebStudio();
        if (studio.getModel().hasLastTest()) {
            addLastBenchmark();
        }
        BenchmarkInfo[] benchmarks = studio.getBenchmarks();
        benchmarkResults.clear();
        for (BenchmarkInfo bi : benchmarks) {
            benchmarkResults.add(bi);
        }
        bencmarkSelected = new boolean[benchmarkResults.size()];
        return benchmarkResults;
    }

    public boolean getBencmarkSelected() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bencmarkSelected[benchmarkResults.indexOf(bi)];
    }

    public List<BenchmarkInfo> getComparedBenchmarks() {
        List<BenchmarkInfo> benchmarks = new ArrayList<BenchmarkInfo>();
        for (int i = 0; i < comparedBenchmarks.length; i++) {
            if (comparedBenchmarks[i] != null) {
                benchmarks.add(comparedBenchmarks[i]);
            }
        }
        return benchmarks;
    }

    public int getComparedI() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        return benchmarkResults.indexOf(bi) + 1;
    }

    public int getComparedOrder() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        return benchmarkOrders[benchmarkResults.indexOf(bi)].getOrder();
    }

    public String getComparedRatio() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        double ratio = benchmarkOrders[benchmarkResults.indexOf(bi)].getRatio();
        return BenchmarkInfo.printDouble(ratio, 2);
    }

    public String getComparedRunsunitsec() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        return bi.runsunitsec();
    }

    public HtmlDataTable getHtmlDataTableBM() {
        return htmlDataTableBM;
    }

    public HtmlDataTable getHtmlDataTableBMCompared() {
        return htmlDataTableBMCompared;
    }

    public int getI() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return benchmarkResults.indexOf(bi) + 1;
    }

    public String getMsrun() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.msrun();
    }

    public String getMsrununit() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.msrununit();
    }

    public String getRunssec() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.runssec();
    }

    public String getRunsunitsec() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.runsunitsec();
    }

    public String getStyleForOrder() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        switch (benchmarkOrders[benchmarkResults.indexOf(bi)].getOrder()) {
            case 1:
                return "color: red; font-size: large;";
            case 2:
                return "color: green; font-size: medium;";
            case 3:
                return "color: blue;font-size: medium;";
        }
        return "color: black;";
    }

    public String getUnitName() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.unitName();
    }

    public int getUnitRuns() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.getUnit().nUnitRuns();
    }

    public void setBencmarkSelected(boolean bencmarkSelected) {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        this.bencmarkSelected[benchmarkResults.indexOf(bi)] = bencmarkSelected;
    }

    public void setHtmlDataTableBM(HtmlDataTable htmlDataTable) {
        htmlDataTableBM = htmlDataTable;
    }

    public void setHtmlDataTableBMCompared(HtmlDataTable htmlDataTableBMComapre) {
        htmlDataTableBMCompared = htmlDataTableBMComapre;
    }

}

package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.html.HtmlDataTable;

import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkOrder;

public class BenchmarkMethodBean {
    private WebStudio studio;
    private ArrayList<BenchmarkInfo> benchmarkResults = new ArrayList<BenchmarkInfo>();
    private BenchmarkInfo[] comparedBenchmarks = new BenchmarkInfo[0];
    private BenchmarkOrder[] benchmarkOrders;
    private boolean[] bencmarkSelected;
    private HtmlDataTable htmlDataTableBM;
    private HtmlDataTable htmlDataTableBMCompared;
    public int getBenchmarkCount() {
        return benchmarkResults.size()+1;
    }

    public HtmlDataTable getHtmlDataTableBM() {
    	return htmlDataTableBM;
    }

    public List<BenchmarkInfo> getComparedBenchmarks() {
        List<BenchmarkInfo> benchmarks = new ArrayList<BenchmarkInfo>();
        for (int i = 0; i < comparedBenchmarks.length; i++) {
            if (comparedBenchmarks[i]!= null){
            	benchmarks.add(comparedBenchmarks[i]);
            }
        }
        return benchmarks;
    }

    public void setHtmlDataTableBM(HtmlDataTable htmlDataTable) {
        this.htmlDataTableBM = htmlDataTable;
    }

    public String getMsrun() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.msrun();
    }

    public String getRunssec() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.runssec();
    }

    public String getUnitName() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.unitName();
    }

    public int getUnitRuns() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.getUnit().nUnitRuns();
    }

    public String getMsrununit() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.msrununit();
    }

    public String getRunsunitsec() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bi.runsunitsec();
    }

    public String getComparedRunsunitsec() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        return bi.runsunitsec();
    }

    public int getI() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return benchmarkResults.indexOf(bi) + 1;
    }

    public int getComparedI() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBMCompared.getRowData();
        return benchmarkResults.indexOf(bi) + 1;
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

    public List<BenchmarkInfo> getBenchmarks() {
        studio = WebStudioUtils.getWebStudio();
        if (FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI) != null) {
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

    public void addLastBenchmark() {
        String elementUri = getElementUri();
        String testName = getTestName();
        String testID = getTestID();
        String testDescr = getTestDescr();
        try {
            BenchmarkInfo buLast = studio.getModel().benchmarkElement(
                    elementUri, testName, testID, testDescr, 3000);
            studio.addBenchmark(buLast);
            benchmarkResults.add(buLast);
        } catch (Exception e) {
            e.printStackTrace(System.out); 
        }
        comparedBenchmarks = new BenchmarkInfo[0];
    }
   
    public String getTestName() {
        return FacesUtils.getRequestParameter("testName");
    }

    private String getTestID() {
        return FacesUtils.getRequestParameter("testID");
    }

    public String getTestDescr() {
        String testDescr = FacesUtils.getRequestParameter("testDescr");
        if (testDescr == null) {
            testDescr = "";
        }
        return testDescr;
    }

    private String getElementUri() {
        return FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);
    }

    public boolean getBencmarkSelected() {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        return bencmarkSelected[benchmarkResults.indexOf(bi)];
    }

    public void setBencmarkSelected(boolean bencmarkSelected) {
        BenchmarkInfo bi = (BenchmarkInfo) htmlDataTableBM.getRowData();
        this.bencmarkSelected[benchmarkResults.indexOf(bi)] = bencmarkSelected;
    }

    public HtmlDataTable getHtmlDataTableBMCompared() {
        return htmlDataTableBMCompared;
    }

    public void setHtmlDataTableBMCompared(HtmlDataTable htmlDataTableBMComapre) {
        this.htmlDataTableBMCompared = htmlDataTableBMComapre;
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

    public String compare() {
        compareBenchmarks();
        return null;
    }

    public String delete() {
        deleteBenchmark();
        return null;
    }

}

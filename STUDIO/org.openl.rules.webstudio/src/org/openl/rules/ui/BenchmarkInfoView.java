package org.openl.rules.ui;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.benchmark.BenchmarkInfo;
import org.openl.rules.ui.benchmark.BenchmarkUnit;

import java.util.Objects;

/**
 * Benchmark Info that will be displayed in the form. Contains BenchmarkInfo and
 * URI of a table that was used to measure the benchmark.
 * 
 * @author NSamatov
 */
public class BenchmarkInfoView {
    private final BenchmarkInfo benchmarkInfo;
    private final String tableId;
    private final String testName;
    private final String testInfo;
    private final ParameterWithValueDeclaration params[];
    private boolean selected;

    public BenchmarkInfoView(BenchmarkInfo benchmarkInfo, String tableId, String testName, String testInfo) {
        this(benchmarkInfo, tableId, testName, testInfo, new ParameterWithValueDeclaration[0]);
    }
    
    public BenchmarkInfoView(BenchmarkInfo benchmarkInfo, String tableId, String testName,
            String testInfo, ParameterWithValueDeclaration params[]) {
        this.benchmarkInfo = benchmarkInfo;
        this.tableId = tableId;
        this.testName = testName;
        this.testInfo = testInfo;
        this.params = params;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public BenchmarkInfo getBenchmarkInfo() {
        return benchmarkInfo;
    }

    /**
     * Get an id of a table that was used to measure the benchmark
     * 
     * @return id of a table that was used to measure the benchmark
     */
    public String getTableId() {
        return tableId;
    }

    /**
     * Get a test name that was used to measure the benchmark
     * 
     * @return test name
     */
    public String getTestName() {
        return testName;
    }

    public String getTestInfo() {
        return testInfo;
    }

    /**
     * Get test execution parameters
     * 
     * @return execution parameters
     */
    public ParameterWithValueDeclaration[] getParameters() {
        return params;
    }

    @Override
    public int hashCode() {
        return Objects.hash(benchmarkInfo, tableId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof BenchmarkInfoView))
            return false;

        BenchmarkInfoView other = (BenchmarkInfoView) obj;

        return Objects.equals(benchmarkInfo, other.benchmarkInfo) &&
                Objects.equals(tableId, other.tableId);
    }

    // delegated methods

    public double avg() {
        return benchmarkInfo.avg();
    }

    public double deviation() {
        return benchmarkInfo.deviation();
    }

    public double drunsunitsec() {
        return benchmarkInfo.drunsunitsec();
    }

    public Throwable getError() {
        return benchmarkInfo.getError();
    }

    public String getName() {
        return benchmarkInfo.getName();
    }

    public BenchmarkUnit getUnit() {
        return benchmarkInfo.getUnit();
    }

    public String msrun() {
        return benchmarkInfo.msrun();
    }

    public String msrununit() {
        return benchmarkInfo.msrununit();
    }

    public String runssec() {
        return benchmarkInfo.runssec();
    }

    public String runsunitsec() {
        return benchmarkInfo.runsunitsec();
    }
}

package org.openl.rules.ui;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;

/**
 * Benchmark Info that will be displayed in the form. Contains BenchmarkInfo and
 * URI of a table that was used to measure the benchmark.
 * 
 * @author NSamatov
 */
public class BenchmarkInfoView {
    private final BenchmarkInfo benchmarkInfo;
    private final String uri;
    private final String testName;
    private final String testInfo;
    private final ParameterWithValueDeclaration params[];

    public BenchmarkInfoView(BenchmarkInfo benchmarkInfo, String uri, String testName, String testInfo) {
        this(benchmarkInfo, uri, testName, testInfo, new ParameterWithValueDeclaration[0]);
    }
    
    public BenchmarkInfoView(BenchmarkInfo benchmarkInfo, String uri, String testName,
            String testInfo, ParameterWithValueDeclaration params[]) {
        this.benchmarkInfo = benchmarkInfo;
        this.uri = uri;
        this.testName = testName;
        this.testInfo = testInfo;
        this.params = params;
    }

    public BenchmarkInfo getBenchmarkInfo() {
        return benchmarkInfo;
    }

    /**
     * Get an URI of a table that was used to measure the benchmark
     * 
     * @return URI of a table that was used to measure the benchmark
     */
    public String getUri() {
        return uri;
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
        return new HashCodeBuilder().append(benchmarkInfo).append(uri).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof BenchmarkInfoView))
            return false;

        BenchmarkInfoView other = (BenchmarkInfoView) obj;

        return new EqualsBuilder().append(benchmarkInfo, other.benchmarkInfo).append(uri, other.uri).isEquals();
    }

    // delegated methods

    public double avg() {
        return benchmarkInfo.avg();
    }

    public double avgMemory() {
        return benchmarkInfo.avgMemory();
    }

    public double avgLeaked() {
        return benchmarkInfo.avgLeaked();
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

    public String printUnits() {
        return benchmarkInfo.printUnits();
    }

    public String unitName() {
        return benchmarkInfo.unitName();
    }

}

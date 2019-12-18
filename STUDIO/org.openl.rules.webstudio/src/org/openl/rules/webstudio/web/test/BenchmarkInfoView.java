package org.openl.rules.webstudio.web.test;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;

/**
 * Benchmark Info that will be displayed in the form. Contains BenchmarkInfo and URI of a table that was used to measure
 * the benchmark.
 *
 * @author NSamatov
 */
public class BenchmarkInfoView {
    private final long times;
    private final long nanos;
    private final int nUnitRuns;
    private final String tableId;
    private final String testName;
    private final String testInfo;
    private final ParameterWithValueDeclaration params[];
    private boolean selected;

    BenchmarkInfoView(long times,
            long nanos,
            int nUnitRuns,
            String tableId,
            String testName,
            String testInfo,
            ParameterWithValueDeclaration params[]) {
        this.nUnitRuns = nUnitRuns;
        this.times = times;
        this.nanos = nanos;
        this.tableId = tableId;
        this.testName = testName;
        this.testInfo = testInfo;
        this.params = params;
    }

    private static String printDouble(double d) {
        if (d >= 1000) {
            DecimalFormat fmt = new DecimalFormat("#,##0");
            return fmt.format(d);
        }
        if (d >= 1) {
            DecimalFormat fmt = new DecimalFormat("#,##0.00");
            return fmt.format(d);
        }

        if (d > 0.1) {
            return printDouble(d, 3);
        }
        if (d > 0.01) {
            return printDouble(d, 4);
        }
        if (d > 0.001) {
            return printDouble(d, 5);
        }

        return printDouble(d, 6);
    }

    static String printDouble(double d, int decimals) {
        NumberFormat fmt = NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(decimals);
        fmt.setMinimumFractionDigits(decimals);
        return fmt.format(d);
    }

    private static String printLargeDouble(double d) {
        DecimalFormat fmt = new DecimalFormat("#,##0");
        return fmt.format(d);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
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

    public int nUnitRuns() {
        return nUnitRuns;
    }

    public String msrun() {
        return printDouble(1e-6 * nanos / times);
    }

    public String msrununit() {
        return printDouble(1e-6 * nanos / times / nUnitRuns);
    }

    public String runssec() {
        return printLargeDouble(1e9 * times / nanos);
    }

    public String runsunitsec() {
        return printLargeDouble(drunsunitsec());
    }

    public double drunsunitsec() {
        return 1e9 * nUnitRuns * times / nanos;
    }
}

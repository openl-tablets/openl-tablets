package org.openl.studio.projects.service.benchmark;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;

/**
 * One row of a benchmark: how long a table took, and what was measured.
 *
 * <p>The execution time covers every run of the measurement together. The time of a single run is that time
 * divided by the number of runs, and the time of a single test case divides it further by the cases a run
 * covers.
 *
 * @param id            identifier of the measurement, by which it is deleted from the results
 * @param tableId       the measured table
 * @param module        module the measured table is written in, absent when no module of the project holds it
 * @param name          display name of the measured table
 * @param testTable     whether the measured table is a test table, whose cases were run
 * @param runTable      {@code true} when the measured table is a Run table, absent otherwise
 * @param testCases     how many test cases one run covers
 * @param runs          how many times the measurement ran
 * @param executionTime how long every run took together, in nanoseconds
 * @param parameters    the input of the measured case, empty when a whole test table was measured
 */
public record BenchmarkMeasurement(String id,
                                   String tableId,
                                   @Nullable String module,
                                   String name,
                                   boolean testTable,
                                   @Nullable Boolean runTable,
                                   int testCases,
                                   int runs,
                                   long executionTime,
                                   List<ParameterWithValueDeclaration> parameters) {
}

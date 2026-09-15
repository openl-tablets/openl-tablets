package org.openl.studio.projects.model.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;

import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.studio.projects.model.ExecutionValueMapper;
import org.openl.studio.projects.service.benchmark.BenchmarkMeasurement;

/**
 * Writes a measurement the way the benchmark results are read.
 *
 * <p>The time is reported in milliseconds, the unit every execution API reports it in. The input of the
 * measured case is written as it stands, without the schema of its type, because the results are read and
 * never edited.
 */
public class BenchmarkResultMapper {

    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;

    private final ExecutionValueMapper valueMapper;

    public BenchmarkResultMapper(ObjectMapper objectMapper,
                                 SchemaGenerator schemaGenerator,
                                 SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy) {
        this.valueMapper = new ExecutionValueMapper(objectMapper, schemaGenerator, sprNamingStrategy);
    }

    public BenchmarkResult mapResult(BenchmarkMeasurement measurement) {
        return BenchmarkResult.builder()
                .id(measurement.id())
                .tableId(measurement.tableId())
                .module(measurement.module())
                .name(measurement.name())
                .testTable(measurement.testTable())
                .runTable(measurement.runTable())
                .testCases(measurement.testCases())
                .runs(measurement.runs())
                .executionTimeMs(measurement.executionTime() / NANOS_IN_MILLISECOND)
                .parameters(measurement.parameters().stream()
                        .map(parameter -> valueMapper.writeParameter(parameter, null, false))
                        .toList())
                .build();
    }
}

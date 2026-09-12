package org.openl.studio.projects.model.benchmark;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.ParameterValue;

@Builder
@Schema(description = "benchmark.type.result.desc")
public record BenchmarkResult(
        @Parameter(description = "benchmark.field.id.desc")
        String id,

        @Parameter(description = "benchmark.field.table-id.desc")
        String tableId,

        @Parameter(description = "benchmark.field.module.desc")
        @Nullable String module,

        @Parameter(description = "benchmark.field.name.desc")
        String name,

        @Parameter(description = "benchmark.field.test-table.desc")
        boolean testTable,

        @Parameter(description = "benchmark.field.run-table.desc")
        @Nullable Boolean runTable,

        @Parameter(description = "benchmark.field.test-cases.desc")
        int testCases,

        @Parameter(description = "benchmark.field.runs.desc")
        int runs,

        @Parameter(description = "benchmark.field.execution-time.desc")
        double executionTimeMs,

        @Parameter(description = "benchmark.field.parameters.desc")
        List<ParameterValue> parameters
) {
}

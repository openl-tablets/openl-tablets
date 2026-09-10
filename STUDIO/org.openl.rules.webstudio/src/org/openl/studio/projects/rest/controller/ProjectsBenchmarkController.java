package org.openl.studio.projects.rest.controller;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketBenchmarkExecutionProgressListenerFactory;
import org.openl.studio.projects.model.benchmark.BenchmarkResult;
import org.openl.studio.projects.model.benchmark.BenchmarkResultMapper;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ExecutionStatus;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.ProjectObjectMapperService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.benchmark.BenchmarkExecutorService;
import org.openl.studio.projects.service.benchmark.ExecutionBenchmarkResultRegistry;
import org.openl.studio.projects.service.trace.TableInputParserService;
import org.openl.util.StringUtils;

/**
 * REST controller for measuring how fast a table runs.
 * <p>
 * A benchmark runs the table over and over until the measurement lasts long enough to be meaningful, and
 * reports how long a single run took. The measurements of a session are kept, so that several of them can be
 * compared with each other.
 * </p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/benchmarks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Benchmark (BETA)", description = "Experimental benchmark API")
@Validated
public class ProjectsBenchmarkController {

    private final WorkspaceProjectService projectService;
    private final BenchmarkExecutorService benchmarkExecutorService;
    private final ExecutionBenchmarkResultRegistry benchmarkResultRegistry;
    private final SocketBenchmarkExecutionProgressListenerFactory listenerFactory;
    private final TableInputParserService inputParserService;
    private final ProjectObjectMapperService objectMapperService;
    private final ProjectIdentifierMapper projectIdentifierMapper;

    @Lookup
    protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
        return null;
    }

    @Operation(summary = "benchmark.start.summary", description = "benchmark.start.desc",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Object.class))))
    @ApiResponse(responseCode = "202", description = "benchmark.start.202.desc")
    @ApiResponse(responseCode = "404", description = "benchmark.start.404.desc")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startBenchmark(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("tableId") @Parameter(description = "benchmark.param.table-id.desc") String tableId,
            @RequestParam(value = "testRanges", required = false) @Parameter(description = "benchmark.param.test-ranges.desc") String testRanges,
            @RequestParam(value = "fromModule", required = false) @Parameter(description = "benchmark.param.from-module.desc") String fromModule,
            @Parameter(description = "benchmark.param.input-json.desc") @RequestBody(required = false) String inputJson) {

        var projectId = projectIdentifierMapper.map(project);
        var user = projectService.getUserWorkspace().getUser();
        // a blank `?fromModule=` means the whole project, not a module named "" (which would fail to resolve)
        var moduleName = StringUtils.trimToNull(fromModule);
        var projectModel = projectService.openProject(project, moduleName).awaitCompiled();
        var currentOpenedModule = moduleName != null;

        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }

        var uri = table.getUri();
        var method = currentOpenedModule
                ? projectModel.getOpenedModuleMethod(uri)
                : projectModel.getMethod(uri);

        if (method == null) {
            throw new NotFoundException("table.message");
        }

        // A test table states its own input, in the test cases the benchmark is taken over. Any other table is
        // measured over the input the caller gives it, the way the Run API runs it.
        Object[] params = null;
        IRulesRuntimeContext runtimeContext = null;
        if (!(method instanceof TestSuiteMethod)) {
            var parseResult = inputParserService.parseInput(inputJson, method,
                    objectMapperService.createObjectMapper());
            params = parseResult.params();
            runtimeContext = parseResult.runtimeContext();
        }

        benchmarkResultRegistry.cancelIfAny();

        var listener = listenerFactory.create(user, projectId, tableId);
        listener.onStatusChanged(ExecutionStatus.PENDING);

        var benchmarkTask = benchmarkExecutorService.benchmark(listener, projectModel, table, testRanges, params,
                runtimeContext, currentOpenedModule);

        benchmarkResultRegistry.setTask(projectId, tableId, benchmarkTask);
    }

    @Operation(summary = "benchmark.list.summary", description = "benchmark.list.desc")
    @ApiResponse(responseCode = "200", description = "benchmark.list.200.desc")
    @ApiResponse(responseCode = "409", description = "benchmark.execution.not.completed.message")
    @GetMapping
    public List<BenchmarkResult> getBenchmarks(@ProjectId @PathVariable("projectId") RulesProject project) {
        var projectId = projectIdentifierMapper.map(project);
        if (benchmarkResultRegistry.hasTask(projectId) && !benchmarkResultRegistry.isDone(projectId)) {
            throw new ConflictException("benchmark.execution.not.completed.message");
        }
        var measurements = benchmarkResultRegistry.collect(projectId);
        if (measurements.isEmpty()) {
            return List.of();
        }
        var objectMapper = objectMapperService.createObjectMapper();
        var mapper = new BenchmarkResultMapper(objectMapper, getSchemaGenerator(objectMapper),
                projectService.getSpreadsheetResultNamingStrategy());
        return measurements.stream()
                .map(mapper::mapResult)
                .toList();
    }

    @Operation(summary = "benchmark.delete.summary", description = "benchmark.delete.desc")
    @ApiResponse(responseCode = "204", description = "benchmark.delete.204.desc")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBenchmarks(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "id", required = false) @Parameter(description = "benchmark.param.id.desc") List<String> ids) {
        benchmarkResultRegistry.forget(projectIdentifierMapper.map(project), ids == null ? List.of() : ids);
    }
}

package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
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

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.projects.messaging.SocketTraceExecutionProgressListenerFactory;
import org.openl.studio.projects.model.trace.TraceNodeView;
import org.openl.studio.projects.model.trace.TraceNodeViewMapper;
import org.openl.studio.projects.model.trace.TraceParameterValue;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.trace.ExecutionTraceResultRegistry;
import org.openl.studio.projects.service.trace.TableInputParserService;
import org.openl.studio.projects.service.trace.TraceExecutionStatus;
import org.openl.studio.projects.service.trace.TraceExecutorService;
import org.openl.studio.projects.service.trace.TraceExportService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.studio.projects.service.trace.TraceTableHtmlService;
import org.openl.types.IOpenMethod;

/**
 * REST controller for trace execution API.
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/trace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Trace (BETA)", description = "Experimental trace execution API")
@Validated
public class ProjectsTraceController {

    private final WorkspaceProjectService projectService;
    private final TraceExecutorService traceExecutorService;
    private final ExecutionTraceResultRegistry traceResultRegistry;
    private final SocketTraceExecutionProgressListenerFactory listenerFactory;
    private final TraceParameterRegistry parameterRegistry;
    private final TraceTableHtmlService traceTableHtmlService;
    private final TableInputParserService inputParserService;
    private final TraceExportService traceExportService;
    private final Environment environment;

    public ProjectsTraceController(WorkspaceProjectService projectService,
                                   TraceExecutorService traceExecutorService,
                                   ExecutionTraceResultRegistry traceResultRegistry,
                                   SocketTraceExecutionProgressListenerFactory listenerFactory,
                                   TraceParameterRegistry parameterRegistry,
                                   TraceTableHtmlService traceTableHtmlService,
                                   TableInputParserService inputParserService,
                                   TraceExportService traceExportService,
                                   Environment environment) {
        this.projectService = projectService;
        this.traceExecutorService = traceExecutorService;
        this.traceResultRegistry = traceResultRegistry;
        this.listenerFactory = listenerFactory;
        this.parameterRegistry = parameterRegistry;
        this.traceTableHtmlService = traceTableHtmlService;
        this.inputParserService = inputParserService;
        this.traceExportService = traceExportService;
        this.environment = environment;
    }

    @Operation(summary = "trace.start.summary", description = "trace.start.desc")
    @ApiResponse(responseCode = "202", description = "trace.start.202.desc")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startTrace(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("tableId") @Parameter(description = "trace.param.table-id.desc") String tableId,
            @RequestParam(value = "testRanges", required = false) @Parameter(description = "trace.param.test-ranges.desc") String testRanges,
            @RequestParam(value = "fromModule", required = false) @Parameter(description = "trace.param.from-module.desc") String fromModule,
            @RequestBody(required = false) @Parameter(description = "trace.param.input-json.desc") String inputJson) {

        traceResultRegistry.cancelIfAny();
        parameterRegistry.clear();

        var projectId = projectService.resolveProjectId(project);
        var user = projectService.getUserWorkspace().getUser();
        var projectModel = projectService.getProjectModel(project, fromModule);
        var currentOpenedModule = fromModule != null;

        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }

        var listener = listenerFactory.create(user, projectId, tableId);
        listener.onStatusChanged(TraceExecutionStatus.PENDING);

        // Create a new TraceHelper for this trace session
        var traceHelper = new TraceHelper();

        CompletableFuture<ITracerObject> traceTask;

        String uri = table.getUri();
        IOpenMethod method = currentOpenedModule
                ? projectModel.getOpenedModuleMethod(uri)
                : projectModel.getMethod(uri);

        if (method instanceof TestSuiteMethod) {
            // TestSuiteMethod - use testRanges
            traceTask = traceExecutorService.traceTestSuite(
                    listener, projectModel, table, testRanges, currentOpenedModule, traceHelper);
        } else {
            // Regular method - parse JSON input using service (auto-detects format)
            var parseResult = inputParserService.parseInput(inputJson, method, configureObjectMapper());

            traceTask = traceExecutorService.traceMethod(
                    listener, projectModel, table, parseResult.params(), parseResult.runtimeContext(),
                    currentOpenedModule, traceHelper);
        }

        traceResultRegistry.setTask(projectId, tableId, traceTask, traceHelper);
    }

    @Operation(summary = "trace.get-nodes.summary", description = "trace.get-nodes.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-nodes.200.desc")
    @GetMapping("/nodes")
    @JsonView(GenericView.Short.class)
    public List<TraceNodeView> getNodes(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "id", required = false) @Parameter(description = "trace.param.node-id.desc") Integer id,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "trace.param.show-real-numbers.desc") boolean showRealNumbers) {

        var traceHelper = getCompletedTraceHelper(project);
        var element = traceHelper.getTableTracer(id == null ? 0 : id);
        if (element == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }
        return createMapper().createSimpleNodes(element.getChildren(), traceHelper, showRealNumbers);
    }

    @Operation(summary = "trace.get-node-details.summary", description = "trace.get-node-details.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-node-details.200.desc")
    @GetMapping("/nodes/{nodeId}")
    @JsonView(GenericView.Full.class)
    public TraceNodeView getNodeDetails(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("nodeId") @Parameter(description = "trace.param.node-id.desc") int nodeId,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "trace.param.show-real-numbers.desc") boolean showRealNumbers) {

        var traceHelper = getCompletedTraceHelper(project);
        var element = traceHelper.getTableTracer(nodeId);
        if (element == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }

        return createMapper().createDetailedNode(element, traceHelper, showRealNumbers);
    }

    @Operation(summary = "trace.cancel.summary", description = "trace.cancel.desc")
    @ApiResponse(responseCode = "204", description = "trace.cancel.204.desc")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTrace(@ProjectId @PathVariable("projectId") RulesProject project) {
        traceResultRegistry.clear();
        parameterRegistry.clear();
    }

    @Operation(summary = "trace.get-parameter.summary", description = "trace.get-parameter.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-parameter.200.desc")
    @GetMapping("/parameters/{parameterId}")
    public TraceParameterValue getParameterValue(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("parameterId") @Parameter(description = "trace.param.parameter-id.desc") int parameterId) {

        getCompletedTraceHelper(project); // Validate trace exists and is completed

        var param = parameterRegistry.get(parameterId);
        if (param == null) {
            throw new NotFoundException("trace.parameter.not.found.message");
        }

        return createMapper().buildParameterValue(param, false);
    }

    @Operation(summary = "trace.get-table-html.summary", description = "trace.get-table-html.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-table-html.200.desc")
    @GetMapping(value = "/nodes/{nodeId}/table", produces = MediaType.TEXT_HTML_VALUE)
    public String getTraceTableHtml(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("nodeId") @Parameter(description = "trace.param.node-id.desc") int nodeId,
            @RequestParam(value = "showFormulas", defaultValue = "false") @Parameter(description = "trace.param.show-formulas.desc") boolean showFormulas) {

        var traceHelper = getCompletedTraceHelper(project);
        var projectModel = projectService.getProjectModel(project, null);

        return traceTableHtmlService.renderTraceTableHtml(traceHelper, nodeId, projectModel, showFormulas);
    }

    @Operation(summary = "trace.export.summary", description = "trace.export.desc")
    @ApiResponse(responseCode = "200", description = "trace.export.200.desc")
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public void exportTrace(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "trace.param.show-real-numbers.desc") boolean showRealNumbers,
            @RequestParam(value = "release", defaultValue = "false") @Parameter(description = "trace.param.release.desc") boolean release,
            HttpServletResponse response) throws IOException {

        var traceHelper = getCompletedTraceHelper(project);

        // Set headers for file download
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue("trace.txt"));
        response.setCharacterEncoding("UTF-8");

        // Stream directly to response (no RAM buffering)
        try (Writer writer = response.getWriter()) {
            try {
                traceExportService.exportTrace(traceHelper, writer, showRealNumbers);
            } catch (TimeoutException e) {
                // Trace already partially written, append timeout message
                writer.write("\n!!!TRACE WAS LIMITED BY TIMEOUT!!!\n");
            }
        } finally {
            if (release) {
                traceResultRegistry.clear();
                parameterRegistry.clear();
            }
        }
    }

    private TraceHelper getCompletedTraceHelper(RulesProject project) {
        var projectId = projectService.resolveProjectId(project);

        if (!traceResultRegistry.hasTask(projectId)) {
            throw new NotFoundException("trace.execution.task.message");
        }
        if (!traceResultRegistry.isDone(projectId)) {
            throw new ConflictException("trace.execution.not.completed.message");
        }

        var traceHelper = traceResultRegistry.getTraceHelperIfDone(projectId);
        if (traceHelper == null) {
            throw new NotFoundException("trace.execution.task.message");
        }
        return traceHelper;
    }

    private TraceNodeViewMapper createMapper() {
        return new TraceNodeViewMapper(configureObjectMapper(), parameterRegistry);
    }

    private ObjectMapper configureObjectMapper() {
        try {
            var objectMapperFactory = projectService.getWebStudio()
                    .getCurrentProjectJacksonObjectMapperFactoryBean();
            objectMapperFactory.setEnvironment(environment);
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw new ConflictException("object.mapper.configuration.failed.message");
        }
    }
}

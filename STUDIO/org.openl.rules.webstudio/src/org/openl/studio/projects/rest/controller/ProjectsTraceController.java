package org.openl.studio.projects.rest.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
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

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.serialization.JsonUtils;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.messaging.SocketTraceExecutionProgressListenerFactory;
import org.openl.studio.projects.model.trace.TraceInputRequest;
import org.openl.studio.projects.model.trace.TraceNodeView;
import org.openl.studio.projects.model.trace.TraceNodeViewMapper;
import org.openl.studio.projects.model.trace.TraceParameterValue;
import org.openl.studio.projects.model.trace.TraceResultResponse;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.trace.ExecutionTraceResultRegistry;
import org.openl.studio.projects.service.trace.TraceExecutionStatus;
import org.openl.studio.projects.service.trace.TraceExecutorService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.studio.projects.service.trace.TraceTableHtmlService;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * REST controller for trace execution API.
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/trace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects Trace (BETA)", description = "Experimental trace execution API")
@Validated
public class ProjectsTraceController {

    private final WorkspaceProjectService projectService;
    private final TraceExecutorService traceExecutorService;
    private final ExecutionTraceResultRegistry traceResultRegistry;
    private final SocketTraceExecutionProgressListenerFactory listenerFactory;
    private final TraceParameterRegistry parameterRegistry;
    private final TraceTableHtmlService traceTableHtmlService;
    private final Environment environment;

    public ProjectsTraceController(WorkspaceProjectService projectService,
                                   TraceExecutorService traceExecutorService,
                                   ExecutionTraceResultRegistry traceResultRegistry,
                                   SocketTraceExecutionProgressListenerFactory listenerFactory,
                                   TraceParameterRegistry parameterRegistry,
                                   TraceTableHtmlService traceTableHtmlService,
                                   Environment environment) {
        this.projectService = projectService;
        this.traceExecutorService = traceExecutorService;
        this.traceResultRegistry = traceResultRegistry;
        this.listenerFactory = listenerFactory;
        this.parameterRegistry = parameterRegistry;
        this.traceTableHtmlService = traceTableHtmlService;
        this.environment = environment;
    }

    @Operation(summary = "Start trace execution (BETA)", description = "Starts asynchronous trace execution for a table")
    @ApiResponse(responseCode = "202", description = "Trace started successfully")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startTrace(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("tableId") @Parameter(description = "Table ID to trace") String tableId,
            @RequestParam(value = "testRanges", required = false) @Parameter(description = "Test ranges (e.g., '1-3,5') for TestSuiteMethod") String testRanges,
            @RequestParam(value = "fromModule", required = false) @Parameter(description = "Module name to run trace from") String fromModule,
            @RequestBody(required = false) TraceInputRequest inputRequest) {

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
            // Regular method - parse JSON input
            var params = parseInputParams(inputRequest, method);
            var runtimeContext = parseRuntimeContext(inputRequest);

            traceTask = traceExecutorService.traceMethod(
                    listener, projectModel, table, params, runtimeContext, currentOpenedModule, traceHelper);
        }

        traceResultRegistry.setTask(projectId, tableId, traceTask, traceHelper);
    }

    @Operation(summary = "Get trace result (BETA)", description = "Retrieves the completed trace result")
    @ApiResponse(responseCode = "200", description = "Trace result retrieved successfully")
    @GetMapping
    @JsonView(GenericView.Short.class)
    public TraceResultResponse getTraceResult(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "Show exact numbers instead of formatted") boolean showRealNumbers) {

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

        var rootElement = traceHelper.getTableTracer(0);
        var mapper = createMapper();
        var rootNodes = mapper.createSimpleNodes(rootElement.getChildren(), traceHelper, showRealNumbers);

        return new TraceResultResponse(rootNodes, countTotalNodes(rootElement));
    }

    @Operation(summary = "Get trace node children (BETA)", description = "Retrieves child nodes for lazy loading")
    @ApiResponse(responseCode = "200", description = "Children retrieved successfully")
    @GetMapping("/nodes")
    @JsonView(GenericView.Short.class)
    public List<TraceNodeView> getNodes(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "id", required = false) @Parameter(description = "Node ID (0 for root)") Integer id,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "Show exact numbers") boolean showRealNumbers) {

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

        var element = traceHelper.getTableTracer(id == null ? 0 : id);
        return createMapper().createSimpleNodes(element.getChildren(), traceHelper, showRealNumbers);
    }

    @Operation(summary = "Get trace node details (BETA)", description = "Retrieves detailed info for a single trace node including parameters, context, and result")
    @ApiResponse(responseCode = "200", description = "Node details retrieved successfully")
    @GetMapping("/nodes/{nodeId}")
    @JsonView(GenericView.Full.class)
    public TraceNodeView getNodeDetails(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("nodeId") @Parameter(description = "Node ID") int nodeId,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "Show exact numbers") boolean showRealNumbers) {

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

        var element = traceHelper.getTableTracer(nodeId);
        if (element == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }

        return createMapper().createDetailedNode(element, traceHelper, showRealNumbers);
    }

    @Operation(summary = "Cancel trace execution (BETA)", description = "Cancels the current trace execution if running")
    @ApiResponse(responseCode = "204", description = "Trace cancelled or no active trace")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTrace(@ProjectId @PathVariable("projectId") RulesProject project) {
        traceResultRegistry.cancelIfAny();
        parameterRegistry.clear();
    }

    @Operation(summary = "Get lazy parameter value (BETA)", description = "Retrieves full JSON value for a lazy-loaded parameter")
    @ApiResponse(responseCode = "200", description = "Parameter value retrieved successfully")
    @GetMapping("/parameters/{parameterId}")
    public TraceParameterValue getParameterValue(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("parameterId") @Parameter(description = "Parameter ID from trace result") int parameterId) {

        var projectId = projectService.resolveProjectId(project);

        if (!traceResultRegistry.hasTask(projectId)) {
            throw new NotFoundException("trace.execution.task.message");
        }
        if (!traceResultRegistry.isDone(projectId)) {
            throw new ConflictException("trace.execution.not.completed.message");
        }

        var param = parameterRegistry.get(parameterId);
        if (param == null) {
            throw new NotFoundException("trace.parameter.not.found.message");
        }

        return createMapper().buildParameterValue(param, false);
    }

    @Operation(summary = "Get traced table as HTML (BETA)", description = "Returns HTML fragment for the traced table with highlighting")
    @ApiResponse(responseCode = "200", description = "HTML table fragment")
    @GetMapping(value = "/nodes/{nodeId}/table", produces = MediaType.TEXT_HTML_VALUE)
    public String getTraceTableHtml(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("nodeId") @Parameter(description = "Trace node ID") int nodeId,
            @RequestParam(value = "showFormulas", defaultValue = "false") @Parameter(description = "Show formulas instead of values") boolean showFormulas) {

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

        var projectModel = projectService.getProjectModel(project, null);

        return traceTableHtmlService.renderTraceTableHtml(traceHelper, nodeId, projectModel, showFormulas);
    }

    private TraceNodeViewMapper createMapper() {
        return new TraceNodeViewMapper(configureObjectMapper(), parameterRegistry);
    }

    private int countTotalNodes(ITracerObject element) {
        int count = 1;
        for (ITracerObject child : element.getChildren()) {
            count += countTotalNodes(child);
        }
        return count;
    }

    private Object[] parseInputParams(TraceInputRequest request, IOpenMethod method) {
        IMethodSignature signature = method.getSignature();
        Object[] params = new Object[signature.getNumberOfParameters()];

        if (request == null || request.params() == null || request.params().isEmpty()) {
            return params;
        }

        ObjectMapper mapper = configureObjectMapper();
        Map<String, Object> inputParams = request.params();

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String paramName = signature.getParameterName(i);
            IOpenClass paramType = signature.getParameterType(i);

            if (inputParams.containsKey(paramName)) {
                Object rawValue = inputParams.get(paramName);
                params[i] = convertValue(rawValue, paramType.getInstanceClass(), mapper);
            }
        }

        return params;
    }

    private IRulesRuntimeContext parseRuntimeContext(TraceInputRequest request) {
        if (request == null || request.runtimeContext() == null || request.runtimeContext().isEmpty()) {
            return null;
        }

        ObjectMapper mapper = configureObjectMapper();
        try {
            String json = mapper.writeValueAsString(request.runtimeContext());
            return JsonUtils.fromJSON(json, IRulesRuntimeContext.class, mapper);
        } catch (Exception e) {
            return new DefaultRulesRuntimeContext();
        }
    }

    private Object convertValue(Object rawValue, Class<?> targetType, ObjectMapper mapper) {
        if (rawValue == null) {
            return null;
        }
        try {
            String json = mapper.writeValueAsString(rawValue);
            return JsonUtils.fromJSON(json, targetType, mapper);
        } catch (Exception e) {
            return rawValue;
        }
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

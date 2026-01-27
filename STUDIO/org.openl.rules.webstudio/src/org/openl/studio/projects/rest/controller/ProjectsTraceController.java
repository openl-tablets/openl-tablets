package org.openl.studio.projects.rest.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
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

import org.openl.base.INamedThing;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.serialization.JsonUtils;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TraceFormatter;
import org.openl.rules.webstudio.web.trace.node.ATableTracerNode;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketTraceExecutionProgressListenerFactory;
import org.openl.studio.projects.model.trace.TraceInputRequest;
import org.openl.studio.projects.model.trace.TraceNodeView;
import org.openl.studio.projects.model.trace.TraceParameterValue;
import org.openl.studio.projects.model.trace.TraceResultResponse;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.trace.ExecutionTraceResultRegistry;
import org.openl.studio.projects.service.trace.TraceExecutionStatus;
import org.openl.studio.projects.service.trace.TraceExecutorService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

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
    private final Environment environment;

    public ProjectsTraceController(WorkspaceProjectService projectService,
                                   TraceExecutorService traceExecutorService,
                                   ExecutionTraceResultRegistry traceResultRegistry,
                                   SocketTraceExecutionProgressListenerFactory listenerFactory,
                                   TraceParameterRegistry parameterRegistry,
                                   Environment environment) {
        this.projectService = projectService;
        this.traceExecutorService = traceExecutorService;
        this.traceResultRegistry = traceResultRegistry;
        this.listenerFactory = listenerFactory;
        this.parameterRegistry = parameterRegistry;
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
        var rootNodes = createNodes(rootElement.getChildren(), traceHelper, showRealNumbers);
        var totalNodes = countTotalNodes(rootElement);

        return new TraceResultResponse(rootNodes, totalNodes);
    }

    @Operation(summary = "Get trace node children (BETA)", description = "Retrieves child nodes for lazy loading")
    @ApiResponse(responseCode = "200", description = "Children retrieved successfully")
    @GetMapping("/nodes")
    public List<TraceNodeView> getNodes(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "id", required = false) @Parameter(description = "Node ID (0 for root)") Integer id,
            @RequestParam(value = "showRealNumbers", defaultValue = "false") @Parameter(description = "Show exact numbers") boolean showRealNumbers) {

        var projectId = projectService.resolveProjectId(project);

        if (!traceResultRegistry.isDone(projectId)) {
            throw new ConflictException("trace.execution.not.completed.message");
        }

        var traceHelper = traceResultRegistry.getTraceHelperIfDone(projectId);
        if (traceHelper == null) {
            throw new NotFoundException("trace.execution.task.message");
        }

        var element = traceHelper.getTableTracer(id == null ? 0 : id);
        return createNodes(element.getChildren(), traceHelper, showRealNumbers);
    }

    @Operation(summary = "Cancel trace execution (BETA)", description = "Cancels the current trace execution if running")
    @ApiResponse(responseCode = "204", description = "Trace cancelled or no active trace")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTrace(@ProjectId @PathVariable("projectId") RulesProject project) {
        traceResultRegistry.cancelIfAny();
    }

    @Operation(summary = "Get lazy parameter value (BETA)", description = "Retrieves full JSON value for a lazy-loaded parameter")
    @ApiResponse(responseCode = "200", description = "Parameter value retrieved successfully")
    @GetMapping("/parameters/{parameterId}")
    public TraceParameterValue getParameterValue(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("parameterId") @Parameter(description = "Parameter ID from trace result") int parameterId) {

        var projectId = projectService.resolveProjectId(project);

        if (!traceResultRegistry.isDone(projectId)) {
            throw new ConflictException("trace.execution.not.completed.message");
        }

        var param = parameterRegistry.get(parameterId);
        if (param == null) {
            throw new NotFoundException("trace.parameter.not.found.message");
        }

        ObjectMapper objectMapper = configureObjectMapper();
        SchemaGenerator schemaGenerator = initSchemaGenerator(objectMapper);

        return buildParameterValue(param, objectMapper, schemaGenerator, null, false);
    }

    private List<TraceNodeView> createNodes(Iterable<ITracerObject> children,
                                            TraceHelper traceHelper,
                                            boolean showRealNumbers) {
        ObjectMapper objectMapper = configureObjectMapper();
        SchemaGenerator schemaGenerator = initSchemaGenerator(objectMapper);

        List<TraceNodeView> nodes = new ArrayList<>();
        for (ITracerObject child : children) {
            nodes.add(createNode(child, traceHelper, showRealNumbers, objectMapper, schemaGenerator));
        }
        return nodes;
    }

    private TraceNodeView createNode(ITracerObject element,
                                     TraceHelper traceHelper,
                                     boolean showRealNumbers,
                                     ObjectMapper objectMapper,
                                     SchemaGenerator schemaGenerator) {
        if (element == null) {
            return TraceNodeView.builder()
                    .key(-1)
                    .title("null")
                    .tooltip("null")
                    .type("value")
                    .lazy(false)
                    .extraClasses("value")
                    .build();
        }

        String name = TraceFormatter.getDisplayName(element, !showRealNumbers);
        int key = traceHelper.getNodeKey(element);
        String type = getType(element);
        boolean lazy = !element.isLeaf();

        // Build parameters, context, and result
        List<TraceParameterValue> parameters = buildInputParameters(element, objectMapper, schemaGenerator);
        TraceParameterValue context = buildContext(element, objectMapper, schemaGenerator);
        TraceParameterValue result = buildResult(element, objectMapper, schemaGenerator);

        return TraceNodeView.builder()
                .key(key)
                .title(name)
                .tooltip(name)
                .type(type)
                .lazy(lazy)
                .extraClasses(type)
                .parameters(parameters)
                .context(context)
                .result(result)
                .build();
    }

    private String getType(ITracerObject element) {
        String type = element.getType();
        if (type == null) {
            type = StringUtils.EMPTY;
        }
        if (element instanceof DTRuleTraceObject condition) {
            if (!condition.isSuccessful()) {
                return type + " fail";
            } else {
                ITracerObject result = findResult(element.getChildren());
                if (result != null) {
                    return type + " result";
                }
                return type + " no_result";
            }
        }
        return type;
    }

    private ITracerObject findResult(Iterable<ITracerObject> children) {
        for (ITracerObject child : children) {
            if (child instanceof DTRuleTracerLeaf) {
                return child;
            }
            ITracerObject result = findResult(child.getChildren());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private int countTotalNodes(ITracerObject element) {
        int count = 1;
        for (ITracerObject child : element.getChildren()) {
            count += countTotalNodes(child);
        }
        return count;
    }

    private SchemaGenerator initSchemaGenerator(ObjectMapper objectMapper) {
        var config = new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new Swagger2Module())
                .build();
        return new SchemaGenerator(config);
    }

    private List<TraceParameterValue> buildInputParameters(ITracerObject tto,
                                                            ObjectMapper objectMapper,
                                                            SchemaGenerator schemaGenerator) {
        ATableTracerNode tracerNode = getTableTracerNode(tto);
        if (tracerNode == null || tracerNode.getTraceObject() == null) {
            return Collections.emptyList();
        }

        ExecutableRulesMethod method = tracerNode.getTraceObject();
        Object[] params = tracerNode.getParameters();

        List<TraceParameterValue> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            var param = new ParameterWithValueDeclaration(
                    method.getSignature().getParameterName(i),
                    params[i],
                    method.getSignature().getParameterType(i)
            );
            // Parameters: lazy=true for complex types
            result.add(buildParameterValue(param, objectMapper, schemaGenerator, parameterRegistry, true));
        }
        return result;
    }

    private TraceParameterValue buildContext(ITracerObject tto,
                                              ObjectMapper objectMapper,
                                              SchemaGenerator schemaGenerator) {
        ATableTracerNode tracerNode = getTableTracerNode(tto);
        if (tracerNode == null || tracerNode.getContext() == null) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("context", tracerNode.getContext());
        // Context: always eager (lazy=false)
        return buildParameterValue(param, objectMapper, schemaGenerator, null, false);
    }

    private TraceParameterValue buildResult(ITracerObject tto,
                                             ObjectMapper objectMapper,
                                             SchemaGenerator schemaGenerator) {
        Object resultValue = tto.getResult();
        if (resultValue == null) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("return", resultValue);
        // Result: lazy=true for complex types
        return buildParameterValue(param, objectMapper, schemaGenerator, parameterRegistry, true);
    }

    private TraceParameterValue buildParameterValue(ParameterWithValueDeclaration param,
                                                     ObjectMapper objectMapper,
                                                     SchemaGenerator schemaGenerator,
                                                     TraceParameterRegistry registry,
                                                     boolean preferLazy) {
        if (param == null) {
            return null;
        }

        String name = param.getName();
        Object rawValue = param.getValue();
        IOpenClass type = param.getType();

        // Generate schema for UI tree building
        ObjectNode schema = null;
        if (type != null && type.getInstanceClass() != null) {
            try {
                schema = schemaGenerator.generateSchema(type.getInstanceClass());
            } catch (Exception ignored) {
                // Schema generation may fail for some types
            }
        }

        // Determine if value should be lazy loaded
        boolean isSimple = type != null && type.isSimple();
        boolean shouldBeLazy = preferLazy && rawValue != null && !isSimple;

        if (shouldBeLazy && registry != null) {
            // Lazy: register for later resolution, don't include value
            int id = registry.register(param);
            String typeName = type != null ? type.getDisplayName(INamedThing.SHORT) : null;
            return TraceParameterValue.builder()
                    .name(name)
                    .description(typeName)
                    .lazy(true)
                    .parameterId(id)
                    .schema(schema)
                    .build();
        } else {
            // Eager: include value in response
            JsonNode value = null;
            if (rawValue != null) {
                try {
                    value = objectMapper.valueToTree(rawValue);
                } catch (Exception ignored) {
                    // Value serialization may fail for some types
                }
            }
            String typeName = type != null ? type.getDisplayName(INamedThing.SHORT) : null;
            return TraceParameterValue.builder()
                    .name(name)
                    .description(typeName)
                    .lazy(false)
                    .value(value)
                    .schema(schema)
                    .build();
        }
    }

    private ATableTracerNode getTableTracerNode(ITracerObject tto) {
        if (tto instanceof RefToTracerNodeObject refNode) {
            return getTableTracerNode(refNode.getOriginalTracerNode());
        } else if (tto instanceof ATableTracerNode tableNode) {
            return tableNode;
        } else if (tto != null && tto.getParent() instanceof ATableTracerNode tableNode) {
            return tableNode;
        }
        return null;
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

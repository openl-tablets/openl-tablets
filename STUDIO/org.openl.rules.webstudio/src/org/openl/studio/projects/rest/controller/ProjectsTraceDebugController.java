package org.openl.studio.projects.rest.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.rules.webstudio.web.trace.debug.DefaultSourceClassifier;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.messaging.SocketDebugListenerFactory;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.model.trace.BreakpointTableView;
import org.openl.studio.projects.model.trace.BreakpointsRequest;
import org.openl.studio.projects.model.trace.CellHighlight;
import org.openl.studio.projects.model.trace.DebugFrameVariables;
import org.openl.studio.projects.model.trace.DebugStackView;
import org.openl.studio.projects.model.trace.DebugStatusView;
import org.openl.studio.projects.model.trace.StepType;
import org.openl.studio.projects.model.trace.TraceDebugMapper;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.tables.graph.GraphDirection;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.trace.DebugSession;
import org.openl.studio.projects.service.trace.DebugSessionRegistry;
import org.openl.studio.projects.service.trace.TraceDebugService;
import org.openl.studio.projects.service.trace.TraceDebugStartRequest;
import org.openl.studio.projects.service.trace.TraceHighlightService;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenMethod;

/**
 * REST controller for the interactive trace debugger.
 *
 * <p>Drives a single debug session per user: start, step, resume, pause, breakpoints, and
 * stack/variable inspection. Execution runs on a dedicated worker thread and suspends at breakpoints
 * and step points; the controller reads the live stack while the worker is parked.
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/trace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Trace (BETA)", description = "Interactive trace debugger API")
@Validated
@RequiredArgsConstructor
public class ProjectsTraceDebugController {

    private static final long STEP_TIMEOUT_MILLIS = 30_000;

    private final WorkspaceProjectService projectService;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final TraceDebugService traceDebugService;
    private final DebugSessionRegistry sessionRegistry;
    private final SocketDebugListenerFactory listenerFactory;
    private final TraceParameterRegistry parameterRegistry;
    private final TraceHighlightService traceHighlightService;
    private final ProjectTablesGraphService tablesGraphService;
    private final Environment environment;

    @Lookup
    protected SchemaGenerator getSchemaGenerator(ObjectMapper objectMapper) {
        return null;
    }

    @Operation(summary = "trace.start.summary", description = "trace.start.desc")
    @ApiResponse(responseCode = "200", description = "trace.start.200.desc")
    @PostMapping
    public DebugStackView startTrace(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("tableId") @Parameter(description = "trace.param.table-id.desc") String tableId,
            @RequestParam(value = "testRanges", required = false) @Parameter(description = "trace.param.test-ranges.desc") String testRanges,
            @RequestParam(value = "fromModule", required = false) @Parameter(description = "trace.param.from-module.desc") String fromModule,
            @RequestParam(value = "stopAtEntry", defaultValue = "true") @Parameter(description = "trace.param.stop-at-entry.desc") boolean stopAtEntry,
            @RequestParam(value = "profiling", defaultValue = "false") @Parameter(description = "trace.param.profiling.desc") boolean profiling,
            @RequestBody(required = false) @Parameter(description = "trace.param.input-json.desc") String inputJson) {

        parameterRegistry.clear();
        sessionRegistry.clear();

        var projectId = projectIdentifierMapper.map(project);
        var user = projectService.getUserWorkspace().getUser();
        var projectModel = projectService.openProject(project, fromModule).awaitCompiled();
        var currentOpenedModule = fromModule != null;

        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        IOpenMethod method = currentOpenedModule
                ? projectModel.getOpenedModuleMethod(table.getUri())
                : projectModel.getMethod(table.getUri());
        if (method == null) {
            throw new NotFoundException("table.message");
        }

        DebugListener listener = listenerFactory.create(user, projectId, tableId);
        var objectMapper = configureObjectMapper();
        // The launcher sends the input server-side once; a restart (profiling toggle, replay) re-runs the trace
        // without resending it. Reuse the remembered input when this call carries neither input nor test ranges;
        // otherwise it is a fresh launch, so remember its input for the next restart.
        String effectiveInputJson = inputJson;
        if (inputJson == null && testRanges == null) {
            effectiveInputJson = sessionRegistry.lastInputJson();
        } else {
            sessionRegistry.rememberInputJson(inputJson);
        }
        var request = new TraceDebugStartRequest(projectModel, table, method, projectId, tableId, testRanges,
                currentOpenedModule, effectiveInputJson, objectMapper, sessionRegistry.breakpoints(), stopAtEntry,
                profiling, listener);

        DebugSession session = sessionRegistry.start(traceDebugService.startSession(request));
        // Build the inspection mapper now, while the traced module is the current module, so the session
        // cache is not later pinned to a different module by a concurrent open (e.g. GET /breakpoint-tables).
        createMapper(session);
        session.getDebugger().awaitInitialHalt(STEP_TIMEOUT_MILLIS);
        return inspectStack(session);
    }

    @Operation(summary = "trace.status.summary", description = "trace.status.desc")
    @ApiResponse(responseCode = "200", description = "trace.status.200.desc")
    @GetMapping("/status")
    public DebugStatusView status(@ProjectId @PathVariable("projectId") RulesProject project) {
        return new DebugStatusView(requireSession(project).getDebugger().status().name());
    }

    @Operation(summary = "trace.stack.summary", description = "trace.stack.desc")
    @ApiResponse(responseCode = "200", description = "trace.stack.200.desc")
    @GetMapping("/stack")
    public DebugStackView stack(@ProjectId @PathVariable("projectId") RulesProject project) {
        return inspectStack(requireSession(project));
    }

    @Operation(summary = "trace.step.summary", description = "trace.step.desc")
    @ApiResponse(responseCode = "200", description = "trace.step.200.desc")
    @PostMapping("/step")
    public DebugStackView step(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("type") @Parameter(description = "trace.param.step-type.desc") StepType type) {
        DebugSession session = requireSession(project);
        return session.inLock(() -> {
            requireSuspendedState(session);
            session.getDebugger().command(type.toCommand(), STEP_TIMEOUT_MILLIS);
            return stackView(session);
        });
    }

    @Operation(summary = "trace.resume.summary", description = "trace.resume.desc")
    @ApiResponse(responseCode = "202", description = "trace.resume.202.desc")
    @PostMapping("/resume")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resume(@ProjectId @PathVariable("projectId") RulesProject project) {
        DebugSession session = requireSession(project);
        session.inLock(() -> {
            requireSuspendedState(session);
            session.getDebugger().resume();
        });
    }

    @Operation(summary = "trace.pause.summary", description = "trace.pause.desc")
    @ApiResponse(responseCode = "202", description = "trace.pause.202.desc")
    @PostMapping("/pause")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void pause(@ProjectId @PathVariable("projectId") RulesProject project) {
        requireSession(project).getDebugger().pause();
    }

    @Operation(summary = "trace.get-variables.summary", description = "trace.get-variables.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-variables.200.desc")
    @GetMapping("/frames/{index}/variables")
    public DebugFrameVariables variables(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index) {
        DebugSession session = requireSession(project);
        TraceDebugMapper mapper = createMapper(session);
        return withSuspendedFrame(session, index, frame -> mapper.freezeVariables(frame, session.getClassLoader()));
    }

    @Operation(summary = "trace.get-highlights.summary", description = "trace.get-highlights.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-highlights.200.desc")
    @GetMapping("/frames/{index}/highlights")
    public List<CellHighlight> highlights(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index) {
        DebugSession session = requireSession(project);
        return withSuspendedFrame(session, index, traceHighlightService::computeHighlights);
    }

    @Operation(summary = "trace.get-parameter.summary", description = "trace.get-parameter.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-parameter.200.desc")
    @GetMapping("/parameters/{parameterId}")
    public ParameterValue parameterValue(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("parameterId") @Parameter(description = "trace.param.parameter-id.desc") int parameterId) {
        DebugSession session = requireSession(project);
        var param = parameterRegistry.get(parameterId);
        if (param == null) {
            throw new NotFoundException("trace.parameter.not.found.message");
        }
        return createMapper(session).buildParameterValue(param, false);
    }

    @Operation(summary = "trace.get-breakpoints.summary", description = "trace.get-breakpoints.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-breakpoints.200.desc")
    @GetMapping("/breakpoints")
    public List<String> getBreakpoints(@ProjectId @PathVariable("projectId") RulesProject project) {
        return List.copyOf(sessionRegistry.breakpoints());
    }

    @Operation(summary = "trace.set-breakpoints.summary", description = "trace.set-breakpoints.desc")
    @ApiResponse(responseCode = "204", description = "trace.set-breakpoints.204.desc")
    @PutMapping("/breakpoints")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBreakpoints(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestBody BreakpointsRequest request) {
        sessionRegistry.setBreakpoints(request.safeUris());
    }

    @Operation(summary = "trace.breakpoint-tables.summary", description = "trace.breakpoint-tables.desc")
    @ApiResponse(responseCode = "200", description = "trace.breakpoint-tables.200.desc")
    @GetMapping("/breakpoint-tables")
    public List<BreakpointTableView> breakpointTables(@ProjectId @PathVariable("projectId") RulesProject project) {
        ProjectModel projectModel = projectService.openProject(project, null).awaitCompiled();
        var classifier = new DefaultSourceClassifier();

        // Offer only the tables reachable from the table being traced, so a breakpoint is suggested only
        // where it could fire. With no active session (or a root outside the dependency graph, e.g. a test
        // table), fall back to every table.
        DebugSession session = sessionRegistry.find(projectIdentifierMapper.map(project));
        Set<String> reachable = session == null ? Set.of()
                : tablesGraphService.reachableTableIds(projectModel, session.getTableId(),
                        GraphDirection.DEPENDENCIES, null);
        boolean limitToReachable = !reachable.isEmpty();

        // Distinct by name: one target per name, keyed by the name so a breakpoint stops on any same-named
        // table (every overloaded or dimensional version).
        Map<String, BreakpointTableView> byName = new LinkedHashMap<>();
        projectModel.getAllTableSyntaxNodes().stream()
                .filter(tsn -> !limitToReachable || reachable.contains(tsn.getId()))
                .map(TableSyntaxNode::getMember)
                .filter(ExecutableRulesMethod.class::isInstance)
                .map(classifier::describeFrame)
                .filter(Objects::nonNull)
                .forEach(d -> byName.putIfAbsent(d.name(), new BreakpointTableView(d.name(), d.kind())));
        return byName.values().stream()
                .sorted(Comparator.comparing(BreakpointTableView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Operation(summary = "trace.cancel.summary", description = "trace.cancel.desc")
    @ApiResponse(responseCode = "204", description = "trace.cancel.204.desc")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTrace(@ProjectId @PathVariable("projectId") RulesProject project) {
        sessionRegistry.clear();
        parameterRegistry.clear();
    }

    private DebugSession requireSession(RulesProject project) {
        DebugSession session = sessionRegistry.find(projectIdentifierMapper.map(project));
        if (session == null) {
            throw new NotFoundException("trace.execution.task.message");
        }
        return session;
    }

    private void requireSuspendedState(DebugSession session) {
        if (session.getDebugger().status() != DebugStatus.SUSPENDED) {
            throw new ConflictException("trace.execution.not.suspended.message");
        }
    }

    /** Run an inspection of frame {@code index} under the session lock, requiring a suspended worker. */
    private <T> T withSuspendedFrame(DebugSession session, int index, Function<DebugFrame, T> inspection) {
        return session.inLock(() -> {
            requireSuspendedState(session);
            DebugFrame frame = session.getDebugger().frameAt(index);
            if (frame == null) {
                throw new NotFoundException("trace.frame.not.found.message");
            }
            return inspection.apply(frame);
        });
    }

    /**
     * Map the live stack under the session lock, refusing while the worker is still RUNNING. The worker mutates
     * its frames as it executes, so reading them is safe only once it has parked (suspended) or finished; the lock
     * keeps a concurrent step or resume from waking it mid-read.
     */
    private DebugStackView inspectStack(DebugSession session) {
        return session.inLock(() -> {
            if (session.getDebugger().status() == DebugStatus.RUNNING) {
                throw new ConflictException("trace.execution.not.suspended.message");
            }
            return stackView(session);
        });
    }

    private DebugStackView stackView(DebugSession session) {
        var debugger = session.getDebugger();
        return TraceDebugMapper.toStackView(debugger.status(), debugger.stack(), debugger.error(),
                debugger.completedTree());
    }

    private TraceDebugMapper createMapper(DebugSession session) {
        return session.mapper(this::buildMapper);
    }

    private TraceDebugMapper buildMapper() {
        var objectMapper = configureObjectMapper();
        return new TraceDebugMapper(objectMapper, getSchemaGenerator(objectMapper), parameterRegistry);
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

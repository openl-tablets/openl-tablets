package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.projects.messaging.SocketDebugListenerFactory;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.model.trace.BreakpointTableView;
import org.openl.studio.projects.model.trace.BreakpointsRequest;
import org.openl.studio.projects.model.trace.CellHighlight;
import org.openl.studio.projects.model.trace.DebugFrameVariables;
import org.openl.studio.projects.model.trace.DebugStackView;
import org.openl.studio.projects.model.trace.DebugStatus;
import org.openl.studio.projects.model.trace.DebugStatusView;
import org.openl.studio.projects.model.trace.StackRenderOptions;
import org.openl.studio.projects.model.trace.StackViewMode;
import org.openl.studio.projects.model.trace.StepInputsView;
import org.openl.studio.projects.model.trace.StepType;
import org.openl.studio.projects.model.trace.TraceDebugMapper;
import org.openl.studio.projects.model.trace.TreeChildrenView;
import org.openl.studio.projects.model.trace.WatchView;
import org.openl.studio.projects.model.trace.WatchesRequest;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.tables.graph.GraphDirection;
import org.openl.studio.projects.service.tables.graph.ProjectTablesGraphService;
import org.openl.studio.projects.service.trace.DebugFrame;
import org.openl.studio.projects.service.trace.DebugSession;
import org.openl.studio.projects.service.trace.DebugSessionRegistry;
import org.openl.studio.projects.service.trace.DefaultSourceClassifier;
import org.openl.studio.projects.service.trace.TraceDebugService;
import org.openl.studio.projects.service.trace.TraceDebugStartRequest;
import org.openl.studio.projects.service.trace.TraceExportService;
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
    private final TraceExportService traceExportService;
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
            @RequestParam(value = "detailedTitles", defaultValue = "false") @Parameter(description = "trace.param.detailed-titles.desc") boolean detailedTitles,
            @RequestParam(value = "breakOnErrors", defaultValue = "true") @Parameter(description = "trace.param.break-on-errors.desc") boolean breakOnErrors,
            @RequestParam(value = "includeTree", defaultValue = "true") @Parameter(description = "trace.param.include-tree.desc") boolean includeTree,
            @RequestParam(value = "profileTop", defaultValue = "20") @Min(1) @Parameter(description = "trace.param.profile-top.desc") int profileTop,
            @RequestParam(value = "view", defaultValue = "full") @Parameter(description = "trace.param.view.desc") StackViewMode view,
            @RequestParam(value = "fullTree", defaultValue = "false") @Parameter(description = "trace.param.full-tree.desc") boolean fullTree,
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

        // The session id lets a client tell this session's status events from a stale session's — sessions
        // of the same user and table share one notification topic, and an old one may be reaped much later.
        var sessionId = UUID.randomUUID().toString();
        var listener = listenerFactory.create(user, projectId, tableId, sessionId);
        var objectMapper = configureObjectMapper();
        // The launcher sends the input server-side once; a restart (profiling toggle, replay) re-runs the trace
        // without resending it. Reuse the remembered input when this call carries neither input nor test ranges;
        // otherwise it is a fresh launch, so remember its input for the next restart.
        var effectiveInputJson = inputJson;
        if (inputJson == null && testRanges == null) {
            effectiveInputJson = sessionRegistry.lastInputJson();
        } else {
            sessionRegistry.rememberInputJson(inputJson);
        }
        var request = new TraceDebugStartRequest(projectModel, table, method, projectId, tableId, testRanges,
                currentOpenedModule, effectiveInputJson, objectMapper, sessionRegistry.breakpoints(),
                sessionRegistry.watches(), stopAtEntry, profiling, detailedTitles, breakOnErrors, listener, sessionId);

        var session = sessionRegistry.start(traceDebugService.startSession(request));
        // Build the inspection mapper now, while the traced module is the current module, so the session
        // cache is not later pinned to a different module by a concurrent open (e.g. GET /breakpoint-tables).
        createMapper(session);
        session.getDebugger().awaitInitialHalt(STEP_TIMEOUT_MILLIS);
        return inspectStack(session, renderOptions(includeTree, profileTop, view, fullTree));
    }

    @Operation(summary = "trace.status.summary", description = "trace.status.desc")
    @ApiResponse(responseCode = "200", description = "trace.status.200.desc")
    @GetMapping("/status")
    public DebugStatusView status(@ProjectId @PathVariable("projectId") RulesProject project) {
        return new DebugStatusView(requireSession(project).getDebugger().status());
    }

    @Operation(summary = "trace.stack.summary", description = "trace.stack.desc")
    @ApiResponse(responseCode = "200", description = "trace.stack.200.desc")
    @GetMapping("/stack")
    public DebugStackView stack(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "includeTree", defaultValue = "true") @Parameter(description = "trace.param.include-tree.desc") boolean includeTree,
            @RequestParam(value = "profileTop", defaultValue = "20") @Min(1) @Parameter(description = "trace.param.profile-top.desc") int profileTop,
            @RequestParam(value = "view", defaultValue = "full") @Parameter(description = "trace.param.view.desc") StackViewMode view) {
        return inspectStack(requireSession(project), renderOptions(includeTree, profileTop, view, false));
    }

    @Operation(summary = "trace.tree-children.summary", description = "trace.tree-children.desc")
    @ApiResponse(responseCode = "200", description = "trace.tree-children.200.desc")
    @GetMapping("/tree/children")
    public TreeChildrenView treeChildren(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("uri") @Parameter(description = "trace.param.node-uri.desc") String uri,
            @RequestParam("instance") @Parameter(description = "trace.param.node-instance.desc") int instance,
            @RequestParam("step") @Parameter(description = "trace.param.step-ref.desc") String step,
            @RequestParam(value = "offset", defaultValue = "0") @Min(0) @Parameter(description = "trace.param.offset.desc") int offset,
            @RequestParam(value = "limit", defaultValue = "100") @Min(1) @Parameter(description = "trace.param.limit.desc") int limit) {
        var session = requireSession(project);
        return session.inLock(() -> {
            requireNotRunning(session);
            return TraceDebugMapper.toChildrenView(session.getDebugger().completedTree(), uri, instance, step,
                    offset, limit);
        });
    }

    @Operation(summary = "trace.step.summary", description = "trace.step.desc")
    @ApiResponse(responseCode = "200", description = "trace.step.200.desc")
    @PostMapping("/step")
    public DebugStackView step(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("type") @Parameter(description = "trace.param.step-type.desc") StepType type,
            @RequestParam(value = "includeTree", defaultValue = "true") @Parameter(description = "trace.param.include-tree.desc") boolean includeTree,
            @RequestParam(value = "profileTop", defaultValue = "20") @Min(1) @Parameter(description = "trace.param.profile-top.desc") int profileTop,
            @RequestParam(value = "view", defaultValue = "full") @Parameter(description = "trace.param.view.desc") StackViewMode view) {
        var session = requireSession(project);
        return session.inLock(() -> {
            requireSuspendedState(session);
            var result = session.getDebugger().command(type.toCommand(), STEP_TIMEOUT_MILLIS);
            if (result == DebugStatus.RUNNING) {
                // The step did not reach a suspend point within the timeout, so the worker is still executing.
                // Reading its live, mutating frames here would race the worker; report RUNNING with no frames
                // and let the WebSocket deliver the next stop.
                return TraceDebugMapper.toStackView(result, List.of(), null);
            }
            return stackView(session, renderOptions(includeTree, profileTop, view, false));
        });
    }

    @Operation(summary = "trace.resume.summary", description = "trace.resume.desc")
    @ApiResponse(responseCode = "202", description = "trace.resume.202.desc")
    @PostMapping("/resume")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resume(@ProjectId @PathVariable("projectId") RulesProject project) {
        var session = requireSession(project);
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
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index,
            @RequestParam(value = "includeSchema", defaultValue = "false") @Parameter(description = "trace.param.include-schema.desc") boolean includeSchema) {
        var session = requireSession(project);
        var mapper = createMapper(session);
        return withInspectableFrame(session, index,
                frame -> mapper.freezeVariables(frame, session.getClassLoader(), includeSchema));
    }

    @Operation(summary = "trace.step-inputs.summary", description = "trace.step-inputs.desc")
    @ApiResponse(responseCode = "200", description = "trace.step-inputs.200.desc")
    @GetMapping("/frames/{index}/step-inputs")
    public StepInputsView stepInputs(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index,
            @RequestParam("ref") @Parameter(description = "trace.param.step-ref.desc") String ref,
            @RequestParam(value = "includeSchema", defaultValue = "false") @Parameter(description = "trace.param.include-schema.desc") boolean includeSchema) {
        DebugSession session = requireSession(project);
        TraceDebugMapper mapper = createMapper(session);
        return withInspectableFrame(session, index,
                frame -> mapper.freezeStepInputs(frame, ref, session.getClassLoader(), includeSchema));
    }

    @Operation(summary = "trace.get-highlights.summary", description = "trace.get-highlights.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-highlights.200.desc")
    @GetMapping("/frames/{index}/highlights")
    public List<CellHighlight> highlights(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("index") @Parameter(description = "trace.param.frame-index.desc") int index) {
        var session = requireSession(project);
        return withInspectableFrame(session, index, traceHighlightService::computeHighlights);
    }

    @Operation(summary = "trace.get-parameter.summary", description = "trace.get-parameter.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-parameter.200.desc")
    @GetMapping("/parameters/{parameterId}")
    public ParameterValue parameterValue(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable("parameterId") @Parameter(description = "trace.param.parameter-id.desc") int parameterId,
            @RequestParam(value = "includeSchema", defaultValue = "false") @Parameter(description = "trace.param.include-schema.desc") boolean includeSchema) {
        var session = requireSession(project);
        var param = parameterRegistry.get(parameterId);
        if (param == null) {
            throw new NotFoundException("trace.parameter.not.found.message");
        }
        return createMapper(session).buildParameterValue(param, false, includeSchema);
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
            @Valid @RequestBody BreakpointsRequest request) {
        sessionRegistry.setBreakpoints(request.safeUris());
    }

    @Operation(summary = "trace.watch.summary", description = "trace.watch.desc")
    @ApiResponse(responseCode = "200", description = "trace.watch.200.desc")
    @GetMapping("/watch")
    public WatchView watch(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "includeSchema", defaultValue = "false") @Parameter(description = "trace.param.include-schema.desc") boolean includeSchema) {
        var session = requireSession(project);
        var mapper = createMapper(session);
        return session.inLock(() -> {
            requireNotRunning(session);
            var debugger = session.getDebugger();
            return mapper.toWatchView(debugger.watchCaptures(), debugger.isWatchTruncated(), session.getClassLoader(),
                    includeSchema);
        });
    }

    @Operation(summary = "trace.get-watches.summary", description = "trace.get-watches.desc")
    @ApiResponse(responseCode = "200", description = "trace.get-watches.200.desc")
    @GetMapping("/watches")
    public List<String> getWatches(@ProjectId @PathVariable("projectId") RulesProject project) {
        return List.copyOf(sessionRegistry.watches());
    }

    @Operation(summary = "trace.set-watches.summary", description = "trace.set-watches.desc")
    @ApiResponse(responseCode = "204", description = "trace.set-watches.204.desc")
    @PutMapping("/watches")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setWatches(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Valid @RequestBody WatchesRequest request) {
        sessionRegistry.setWatches(request.safeCells());
    }

    @Operation(summary = "trace.breakpoint-tables.summary", description = "trace.breakpoint-tables.desc")
    @ApiResponse(responseCode = "200", description = "trace.breakpoint-tables.200.desc")
    @GetMapping("/breakpoint-tables")
    public List<BreakpointTableView> breakpointTables(@ProjectId @PathVariable("projectId") RulesProject project) {
        var projectModel = projectService.openProject(project, null).awaitCompiled();
        var classifier = new DefaultSourceClassifier();

        // Offer only the tables reachable from the table being traced, so a breakpoint is suggested only
        // where it could fire. With no active session (or a root outside the dependency graph, e.g. a test
        // table), fall back to every table.
        var session = sessionRegistry.find(projectIdentifierMapper.map(project));
        Set<String> reachable = session == null ? Set.of()
                : tablesGraphService.reachableTableIds(projectModel, session.getTableId(),
                        GraphDirection.DEPENDENCIES, null);
        var limitToReachable = !reachable.isEmpty();

        // Distinct by name: one target per name, keyed by the name so a breakpoint stops on any same-named
        // table (every overloaded or dimensional version).
        var byName = new LinkedHashMap<String, BreakpointTableView>();
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
        releaseSession();
    }

    @Operation(summary = "trace.export.summary", description = "trace.export.desc")
    @ApiResponse(responseCode = "200", description = "trace.export.200.desc")
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> exportTrace(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "release", defaultValue = "false") @Parameter(description = "trace.param.release.desc") boolean release,
            @RequestParam(value = "smartNumbers", defaultValue = "true") @Parameter(description = "trace.param.smart-numbers.desc") boolean smartNumbers)
            throws IOException {
        var session = requireSession(project);
        // The tree is node-capped, so the whole trace fits in memory; render it and return it in one response.
        var buffer = new StringWriter();
        traceExportService.exportTrace(session, buffer, smartNumbers);
        if (release) {
            releaseSession();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue("trace.txt"))
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(buffer.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Terminate the active session and drop its worker plus cached parameter values. */
    private void releaseSession() {
        sessionRegistry.clear();
        parameterRegistry.clear();
    }

    private DebugSession requireSession(RulesProject project) {
        var session = sessionRegistry.find(projectIdentifierMapper.map(project));
        if (session == null) {
            throw new NotFoundException("trace.execution.task.message");
        }
        if (session.getDebugger().status() == DebugStatus.TERMINATED) {
            // The reaper (idle timeout or global session cap) terminated this session out from under the
            // session-scoped registry, which still references it. Drop the dangling reference so the client
            // gets a clean "no active trace" instead of driving a dead worker.
            sessionRegistry.clear();
            throw new NotFoundException("trace.execution.task.message");
        }
        return session;
    }

    private void requireSuspendedState(DebugSession session) {
        if (session.getDebugger().status() != DebugStatus.SUSPENDED) {
            throw new ConflictException("trace.execution.not.suspended.message");
        }
    }

    /** The stack and watch views may be read once the worker has parked or finished, but not while it runs. */
    private void requireNotRunning(DebugSession session) {
        if (session.getDebugger().status() == DebugStatus.RUNNING) {
            throw new ConflictException("trace.execution.not.suspended.message");
        }
    }

    /**
     * Run an inspection of frame {@code index} under the session lock. Allowed whenever the worker is not
     * running — while suspended at a step, and once the run has completed or failed — so an analyst can read
     * the final result after a profiling run to completion, not only at a breakpoint.
     */
    private <T> T withInspectableFrame(DebugSession session, int index, Function<DebugFrame, T> inspection) {
        return session.inLock(() -> {
            requireNotRunning(session);
            var frame = session.getDebugger().frameAt(index);
            if (frame == null) {
                throw new NotFoundException("trace.frame.not.found.message");
            }
            return inspection.apply(frame);
        });
    }

    private static StackRenderOptions renderOptions(boolean includeTree, int profileTop, StackViewMode view,
                                                    boolean fullTree) {
        return new StackRenderOptions(includeTree, profileTop, view == StackViewMode.COMPACT, fullTree);
    }

    /**
     * Map the live stack under the session lock, refusing while the worker is still RUNNING. The worker mutates
     * its frames as it executes, so reading them is safe only once it has parked (suspended) or finished; the lock
     * keeps a concurrent step or resume from waking it mid-read.
     */
    private DebugStackView inspectStack(DebugSession session, StackRenderOptions options) {
        return session.inLock(() -> {
            requireNotRunning(session);
            return stackView(session, options);
        });
    }

    private DebugStackView stackView(DebugSession session, StackRenderOptions options) {
        var debugger = session.getDebugger();
        return TraceDebugMapper.toStackView(debugger.status(), debugger.stack(), debugger.error(),
                debugger.completedTree(), debugger.profileStats(), options, debugger.isTreeTruncated())
                .toBuilder().sessionId(session.getId()).build();
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

package org.openl.studio.projects.model.trace;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cloner.Cloner;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.webstudio.web.trace.debug.ConditionCheck;
import org.openl.rules.webstudio.web.trace.debug.CurrentLocation;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.rules.webstudio.web.trace.debug.SpreadsheetCellNames;
import org.openl.studio.config.SafeSchemaGenerator;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;

/**
 * Maps the debugger's live stack to view models and freezes a frame's variables on demand.
 *
 * <p>Freezing deep-clones a frame's parameters, context, and result while execution is suspended, so
 * the snapshot stays stable even after execution resumes or the frame returns. Large values are
 * registered in the {@link TraceParameterRegistry} and fetched lazily.
 */
@Slf4j
@RequiredArgsConstructor
public class TraceDebugMapper {

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final TraceParameterRegistry parameterRegistry;

    /** Upper bound on the technical stack-trace detail, so a deep failure cannot bloat the response. */
    private static final int MAX_DETAIL = 8_000;

    /** Map the live stack (root to current frame) to a stack view. */
    public DebugStackView toStackView(DebugStatus status, List<DebugFrame> frames, @Nullable Throwable error) {
        List<DebugFrameView> views = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            DebugFrame frame = frames.get(i);
            views.add(DebugFrameView.builder()
                    .index(i)
                    .depth(frame.getDepth())
                    .uri(frame.getUri())
                    .name(frame.getName())
                    .kind(frame.getKind().getCode())
                    .location(toLocationView(frame.getLocation()))
                    .active(i == frames.size() - 1)
                    .completed(frame.isCompleted())
                    .error(frame.getError() != null)
                    .build());
        }
        return DebugStackView.builder()
                .status(status.name())
                .frames(views)
                .error(buildStackError(frames, error))
                .build();
    }

    /** Build a non-technical error view: cleaned message, the table that failed, and a technical drill-down. */
    private static @Nullable DebugError buildStackError(List<DebugFrame> frames, @Nullable Throwable error) {
        if (error == null) {
            return null;
        }
        DebugFrame failing = failingFrame(frames);
        return DebugError.builder()
                .summary(cleanSummary(error))
                .table(failing == null ? null : failing.getName())
                .location(failing == null ? null : locationLabel(failing.getLocation()))
                .type(rootCause(error).getClass().getSimpleName())
                .detail(stackTrace(error))
                .build();
    }

    /** The frame that failed: the deepest one marked with an error, otherwise the current (deepest) frame. */
    private static @Nullable DebugFrame failingFrame(List<DebugFrame> frames) {
        DebugFrame failing = null;
        for (DebugFrame frame : frames) {
            if (frame.getError() != null) {
                failing = frame;
            }
        }
        if (failing == null && !frames.isEmpty()) {
            failing = frames.get(frames.size() - 1);
        }
        return failing;
    }

    private static @Nullable String locationLabel(@Nullable CurrentLocation location) {
        if (location == null) {
            return null;
        }
        return location.label() != null ? location.label() : location.ref();
    }

    /** Prefer the engine's cleaned OpenL message over the raw Java exception text. */
    private static String cleanSummary(Throwable error) {
        Throwable cause = Objects.requireNonNullElse(error.getCause(), error);
        for (OpenLMessage message : OpenLMessagesUtils.newErrorMessages(cause)) {
            if (message.getSummary() != null && !message.getSummary().isBlank()) {
                return message.getSummary();
            }
        }
        Throwable root = rootCause(error);
        return Objects.requireNonNullElse(root.getMessage(), root.getClass().getSimpleName());
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static String stackTrace(Throwable error) {
        var writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String trace = writer.toString();
        return trace.length() > MAX_DETAIL ? trace.substring(0, MAX_DETAIL) + "…" : trace;
    }

    private static @Nullable DebugLocationView toLocationView(@Nullable CurrentLocation location) {
        if (location == null) {
            return null;
        }
        return DebugLocationView.builder()
                .kind(location.kind())
                .row(location.row() < 0 ? null : location.row())
                .column(location.column() < 0 ? null : location.column())
                .ref(location.ref())
                .label(location.label())
                .build();
    }

    /** Freeze a frame's variables. Must be called while the session is suspended. */
    public DebugFrameVariables freezeVariables(DebugFrame frame, @Nullable ClassLoader classLoader) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            Map<Object, Object> clones = new IdentityHashMap<>();
            return DebugFrameVariables.builder()
                    .parameters(freezeParameters(frame, clones))
                    .context(freezeContext(frame, clones))
                    .result(freezeResult(frame, clones))
                    .steps(freezeSteps(frame, clones))
                    .gridColumns(gridNames(frame, true))
                    .gridRows(gridNames(frame, false))
                    .decision(decisionFor(frame))
                    .errors(buildErrors(frame))
                    .build();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private List<ParameterValue> freezeParameters(DebugFrame frame, Map<Object, Object> clones) {
        if (!(frame.getSource() instanceof ExecutableRulesMethod method)) {
            return Collections.emptyList();
        }
        IMethodSignature signature = method.getSignature();
        Object[] params = frame.getParams();
        int count = Math.min(params.length, signature.getNumberOfParameters());
        List<ParameterValue> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var param = new ParameterWithValueDeclaration(
                    signature.getParameterName(i),
                    safeClone(params[i], clones),
                    signature.getParameterType(i));
            result.add(buildParameterValue(param, true));
        }
        return result;
    }

    private @Nullable ParameterValue freezeContext(DebugFrame frame, Map<Object, Object> clones) {
        if (frame.getContext() == null) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("context", safeClone(frame.getContext(), clones));
        return buildParameterValue(param, false);
    }

    private List<StepValueView> freezeSteps(DebugFrame frame, Map<Object, Object> clones) {
        if (frame.getSource() instanceof Spreadsheet spreadsheet) {
            return spreadsheetSteps(frame, spreadsheet, clones);
        }
        // Non-spreadsheet frames: just the executed sub-steps.
        List<DebugFrame.ExecutedStep> executed = frame.getExecutedSteps();
        List<StepValueView> result = new ArrayList<>(executed.size());
        for (DebugFrame.ExecutedStep step : executed) {
            String name = step.label() != null ? step.label() : step.ref();
            var param = new ParameterWithValueDeclaration(name, safeClone(step.value(), clones));
            result.add(StepValueView.builder()
                    .ref(step.ref())
                    .label(step.label())
                    .status("executed")
                    .value(buildParameterValue(param, true))
                    .build());
        }
        return result;
    }

    /** All cells of a spreadsheet with their status (executed, current, pending) and executed values. */
    private List<StepValueView> spreadsheetSteps(DebugFrame frame, Spreadsheet spreadsheet, Map<Object, Object> clones) {
        Map<String, Object> executed = new HashMap<>();
        for (DebugFrame.ExecutedStep step : frame.getExecutedSteps()) {
            executed.put(step.ref(), step.value());
        }
        CurrentLocation location = frame.getLocation();
        String currentRef = location == null ? null : location.ref();
        List<StepValueView> steps = new ArrayList<>();
        for (SpreadsheetCell[] row : spreadsheet.getCells()) {
            for (SpreadsheetCell cell : row) {
                if (cell == null || cell.isEmpty()) {
                    continue;
                }
                String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
                var builder = StepValueView.builder().ref(ref).label(SpreadsheetCellNames.of(spreadsheet, cell));
                if (executed.containsKey(ref)) {
                    var param = new ParameterWithValueDeclaration(ref, safeClone(executed.get(ref), clones), cell.getType());
                    steps.add(builder.status("executed").value(buildParameterValue(param, true)).build());
                } else if (ref.equals(currentRef)) {
                    steps.add(builder.status("current").build());
                } else {
                    steps.add(builder.status("pending").build());
                }
            }
        }
        return steps;
    }

    /** Spreadsheet column or row names, so the UI can lay the steps out as a grid like the source table. */
    private static @Nullable List<String> gridNames(DebugFrame frame, boolean columns) {
        if (!(frame.getSource() instanceof Spreadsheet spreadsheet)) {
            return null;
        }
        String[] names = columns ? spreadsheet.getColumnNames() : spreadsheet.getRowNames();
        return Arrays.stream(names).map(name -> name == null ? "" : name).toList();
    }

    private @Nullable ParameterValue freezeResult(DebugFrame frame, Map<Object, Object> clones) {
        if (!frame.isCompleted() || frame.getResult() == null
                || !(frame.getSource() instanceof ExecutableRulesMethod method)) {
            return null;
        }
        var param = new ParameterWithValueDeclaration("return", safeClone(frame.getResult(), clones), method.getType());
        return buildParameterValue(param, true);
    }

    private static Object safeClone(Object value, Map<Object, Object> clones) {
        if (value == null) {
            return null;
        }
        try {
            return Cloner.clone(value, clones);
        } catch (Exception e) {
            log.debug("Failed to freeze a value, using the live reference", e);
            return value;
        }
    }

    private List<MessageDescription> buildErrors(DebugFrame frame) {
        Throwable error = frame.getError();
        if (error == null) {
            return Collections.emptyList();
        }
        Throwable cause = Objects.requireNonNullElse(error.getCause(), error);
        List<MessageDescription> result = new ArrayList<>();
        for (OpenLMessage message : OpenLMessagesUtils.newErrorMessages(cause)) {
            result.add(new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()));
        }
        return result;
    }

    /** Decision-table outcome explanation, or {@code null} for non-decision-table frames. */
    private static @Nullable DecisionView decisionFor(DebugFrame frame) {
        if (!(frame.getSource() instanceof IDecisionTable decisionTable)) {
            return null;
        }
        return buildDecision(decisionTable, frame.getConditionChecks(), firedRuleIndices(frame));
    }

    private static int[] firedRuleIndices(DebugFrame frame) {
        return frame.getCurrentStep() instanceof ActionInvoker invoker ? invoker.getRules() : new int[0];
    }

    /**
     * Build the plain-language decision outcome from the rules that fired and the conditions evaluated.
     * Mirrors the green/red table highlight: one entry per condition cell that was checked, so the
     * explanation never claims more than the engine actually evaluated.
     */
    static @Nullable DecisionView buildDecision(IDecisionTable decisionTable, List<ConditionCheck> checks,
                                                int[] firedRules) {
        if (checks.isEmpty() && firedRules.length == 0) {
            return null;
        }
        List<String> fired = Arrays.stream(firedRules).mapToObj(decisionTable::getRuleName).toList();
        List<DecisionConditionView> conditions = new ArrayList<>();
        for (ConditionCheck check : checks) {
            if (!(check.condition() instanceof IBaseCondition condition)) {
                continue;
            }
            String name = condition.getName();
            for (int rule : check.rules()) {
                conditions.add(new DecisionConditionView(name, decisionTable.getRuleName(rule), check.successful()));
            }
        }
        return new DecisionView(fired, conditions);
    }

    /** Build a parameter value, registering large values for lazy retrieval. */
    public ParameterValue buildParameterValue(ParameterWithValueDeclaration param, boolean preferLazy) {
        var type = param.getType();
        var rawValue = param.getValue();
        var description = type != null ? type.getDisplayName(INamedThing.SHORT) : null;
        var isSimple = type != null && type.isSimple();
        if (preferLazy && rawValue != null && !isSimple) {
            return ParameterValue.builder()
                    .name(param.getName())
                    .description(description)
                    .lazy(true)
                    .parameterId(parameterRegistry.register(param))
                    .schema(generateSchema(type))
                    .build();
        }
        return ParameterValue.builder()
                .name(param.getName())
                .description(description)
                .lazy(false)
                .value(serializeValue(rawValue, type))
                .schema(generateSchema(type))
                .build();
    }

    private @Nullable ObjectNode generateSchema(@Nullable IOpenClass type) {
        if (type == null || type.getInstanceClass() == null) {
            return null;
        }
        var clazz = type instanceof CustomSpreadsheetResultOpenClass csr ? csr.getBeanClass() : type.getInstanceClass();
        return SafeSchemaGenerator.generate(schemaGenerator, clazz);
    }

    private @Nullable JsonNode serializeValue(@Nullable Object value, @Nullable IOpenClass type) {
        if (value == null) {
            return null;
        }
        try {
            Class<?> toType = type instanceof CustomSpreadsheetResultOpenClass csr ? csr.getBeanClass() : null;
            Object converted = SpreadsheetResult.convertSpreadsheetResult(value, toType, type, null);
            return objectMapper.valueToTree(converted);
        } catch (Exception e) {
            log.debug("Failed to serialize a frozen value", e);
            return null;
        }
    }
}

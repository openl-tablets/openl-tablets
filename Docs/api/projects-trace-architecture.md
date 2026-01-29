# Projects Trace API - Architecture Design

**Version**: 6.0.0-SNAPSHOT
**Status**: BETA
**Last Updated**: 2026-01-29

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Design](#component-design)
3. [Data Flow](#data-flow)
4. [Session Management](#session-management)
5. [Lazy Loading Strategy](#lazy-loading-strategy)
6. [Trace Collection Architecture](#trace-collection-architecture)
7. [Integration with Rules Engine](#integration-with-rules-engine)
8. [WebSocket Progress Notifications](#websocket-progress-notifications)
9. [Performance Considerations](#performance-considerations)
10. [Security Architecture](#security-architecture)
11. [Design Decisions](#design-decisions)
12. [Future Enhancements](#future-enhancements)

---

## Architecture Overview

### System Context

The Projects Trace API is a debugging facility within OpenL Tablets Studio that enables step-by-step execution tracing of rules. It captures the complete execution tree, including method calls, decision table evaluations, and spreadsheet cell computations.

``` mermaid
flowchart TB
    subgraph CLIENTS["External Clients"]
        UI["Web UI<br/>(React)"]
        WS["WebSocket<br/>(Progress)"]
    end

    subgraph REST["REST Controller Layer"]
        C["ProjectsTraceController<br/>- 7 REST endpoints<br/>- Request validation<br/>- Response mapping"]
    end

    subgraph SERVICE["Service Layer"]
        TES["TraceExecutorService<br/>- Async execution<br/>- Test suite tracing"]
        THS["TraceTableHtmlService<br/>- HTML rendering"]
        TEXP["TraceExportService<br/>- Text export"]
        TIPS["TableInputParserService<br/>- JSON input parsing"]
        TM["TraceNodeViewMapper<br/>- DTO mapping<br/>- Schema generation"]
    end

    subgraph SESSION["Session-Scoped Components"]
        ETRR["ExecutionTraceResultRegistry<br/>- Task tracking<br/>- CompletableFuture storage"]
        TPR["TraceParameterRegistry<br/>- Lazy parameter storage"]
    end

    subgraph CORE["Core Tracing Engine"]
        TBT["TreeBuildTracer<br/>- ThreadLocal trace tree<br/>- Lazy node support"]
        TH["TraceHelper<br/>- BidiMap node cache"]
        PM["ProjectModel<br/>- Rule execution"]
    end

    subgraph LISTENERS["Progress Listeners"]
        SPL["SocketTraceExecutionProgressListenerFactory<br/>- WebSocket notifications"]
    end

    UI -->|HTTP| C
    WS <-->|WebSocket| SPL
    C --> TES
    C --> THS
    C --> TEXP
    C --> TIPS
    C --> ETRR
    C --> TPR
    TES --> TBT
    TES --> PM
    TES --> SPL
    TBT --> TH
    TM --> TPR
```

### Layered Architecture

The API follows a strict layered architecture with clear separation of concerns:

``` mermaid
flowchart TB
    subgraph PRESENTATION["Presentation Layer"]
        P["ProjectsTraceController<br/>- REST endpoints<br/>- Input validation<br/>- JSON views"]
    end

    subgraph SERVICE["Service Layer"]
        S["TraceExecutorService<br/>- Async orchestration<br/>- TraceNodeViewMapper"]
    end

    subgraph SESSION["Session Layer"]
        SS["ExecutionTraceResultRegistry<br/>TraceParameterRegistry<br/>- Per-session state"]
    end

    subgraph CORE["Core Layer"]
        CO["TreeBuildTracer<br/>TraceHelper<br/>- Trace collection"]
    end

    subgraph ENGINE["Rules Engine"]
        E["ProjectModel<br/>TestSuite<br/>- Rule execution"]
    end

    P --> S
    S --> SS
    S --> CO
    CO --> E
```

**Design Principle**: Each layer depends only on layers below it. No upward dependencies.

---

## Component Design

### 1. ProjectsTraceController

**Location**: `org.openl.studio.projects.rest.controller.ProjectsTraceController`

#### Responsibilities

1. **Request Handling**
   - Expose 7 REST endpoints for trace operations
   - Validate incoming requests (tableId, testRanges, parameters)
   - Map DTOs to service layer models

2. **Session Coordination**
   - Access ExecutionTraceResultRegistry for task management
   - Access TraceParameterRegistry for lazy parameters
   - Create TraceHelper for each trace session

3. **Input Parsing**
   - Parse JSON input parameters to Java objects
   - Parse runtime context from JSON
   - Parse test ranges (e.g., "1-3,5")

4. **Response Mapping**
   - Use `@JsonView` for conditional field serialization
   - Map ITracerObject to TraceNodeView DTOs
   - Generate JSON Schema for parameter types

#### Key Design Patterns

**Pattern 1: Dependency Injection**
```java
public ProjectsTraceController(
    WorkspaceProjectService projectService,
    TraceExecutorService traceExecutorService,
    ExecutionTraceResultRegistry traceResultRegistry,
    SocketTraceExecutionProgressListenerFactory listenerFactory,
    TraceParameterRegistry parameterRegistry,
    TraceTableHtmlService traceTableHtmlService,
    TableInputParserService inputParserService,
    TraceExportService traceExportService,
    Environment environment
) {
    // All dependencies injected via constructor
    // Enables testability and loose coupling
}
```

**Pattern 2: Async Task Submission**
```java
@PostMapping
@ResponseStatus(HttpStatus.ACCEPTED)
public void startTrace(...) {
    // Cancel previous trace
    traceResultRegistry.cancelIfAny();
    parameterRegistry.clear();

    // Create new TraceHelper
    var traceHelper = new TraceHelper();

    // Submit async task
    CompletableFuture<ITracerObject> traceTask = traceExecutorService.traceMethod(
        listener, projectModel, table, params, runtimeContext, currentOpenedModule, traceHelper
    );

    // Register task for later retrieval
    traceResultRegistry.setTask(projectId, tableId, traceTask, traceHelper);
}
```

**Pattern 3: JsonView for Partial Responses**
```java
@GetMapping("/nodes")
@JsonView(GenericView.Short.class)  // Only basic fields
public List<TraceNodeView> getNodes(...) { }

@GetMapping("/nodes/{nodeId}")
@JsonView(GenericView.Full.class)   // All fields including parameters
public TraceNodeView getNodeDetails(...) { }
```

---

### 2. TraceExecutorService

**Location**: `org.openl.studio.projects.service.trace.TraceExecutorService`

#### Interface

```java
public interface TraceExecutorService {
    /**
     * Traces a test suite with optional range selection.
     */
    CompletableFuture<ITracerObject> traceTestSuite(
        TraceExecutionProgressListener listener,
        ProjectModel projectModel,
        IOpenLTable table,
        String testRanges,
        boolean currentOpenedModule,
        TraceHelper traceHelper
    );

    /**
     * Traces a regular method with provided parameters.
     */
    CompletableFuture<ITracerObject> traceMethod(
        TraceExecutionProgressListener listener,
        ProjectModel projectModel,
        IOpenLTable table,
        Object[] params,
        IRulesRuntimeContext runtimeContext,
        boolean currentOpenedModule,
        TraceHelper traceHelper
    );
}
```

#### Implementation Details

**Async Execution with Spring**:
```java
@Service
public class TraceExecutorServiceImpl implements TraceExecutorService {

    @Async("testSuiteExecutor")
    @Override
    public CompletableFuture<ITracerObject> traceMethod(...) {
        try {
            listener.onStatusChanged(TraceExecutionStatus.STARTED);

            // Initialize thread-local tracer
            TreeBuildTracer.initialize(true);  // Enable lazy nodes

            try {
                // Execute with tracing
                ITracerObject traceRoot = projectModel.traceElement(testSuite, currentOpenedModule);

                // Cache trace tree
                traceHelper.cacheTraceTree(traceRoot);

                listener.onStatusChanged(TraceExecutionStatus.COMPLETED);
                return CompletableFuture.completedFuture(traceRoot);

            } finally {
                TreeBuildTracer.destroy();  // Clean up ThreadLocal
            }

        } catch (Exception e) {
            listener.onError(e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

**Test Range Parsing**:
```java
private int[] parseTestRanges(String testRanges, int maxTests) {
    if (testRanges == null || testRanges.isEmpty()) {
        return IntStream.range(0, maxTests).toArray();  // All tests
    }

    Set<Integer> indices = new TreeSet<>();
    for (String part : testRanges.split(",")) {
        if (part.contains("-")) {
            String[] range = part.split("-");
            int start = Integer.parseInt(range[0].trim()) - 1;
            int end = Integer.parseInt(range[1].trim()) - 1;
            for (int i = start; i <= end; i++) {
                if (i >= 0 && i < maxTests) indices.add(i);
            }
        } else {
            int index = Integer.parseInt(part.trim()) - 1;
            if (index >= 0 && index < maxTests) indices.add(index);
        }
    }
    return indices.stream().mapToInt(Integer::intValue).toArray();
}
```

---

### 3. ExecutionTraceResultRegistry

**Location**: `org.openl.studio.projects.service.trace.ExecutionTraceResultRegistry`

#### Design

```java
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionTraceResultRegistry {

    private String currentProjectId;
    private String currentTableId;
    private CompletableFuture<ITracerObject> currentTask;
    private TraceHelper currentTraceHelper;

    /**
     * Registers new task, cancelling previous if exists.
     */
    public void setTask(String projectId, String tableId,
                        CompletableFuture<ITracerObject> task,
                        TraceHelper traceHelper) {
        cancelIfAny();  // Cancel previous

        this.currentProjectId = projectId;
        this.currentTableId = tableId;
        this.currentTask = task;
        this.currentTraceHelper = traceHelper;
    }

    /**
     * Checks if task exists for project.
     */
    public boolean hasTask(String projectId) {
        return projectId.equals(currentProjectId) && currentTask != null;
    }

    /**
     * Checks if task is completed.
     */
    public boolean isDone(String projectId) {
        return hasTask(projectId) && currentTask.isDone();
    }

    /**
     * Gets TraceHelper if task completed.
     */
    public TraceHelper getTraceHelperIfDone(String projectId) {
        if (!isDone(projectId)) return null;
        return currentTraceHelper;
    }

    /**
     * Cancels current task if running.
     */
    public void cancelIfAny() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        clear();
    }
}
```

**Why Session Scope?**
- One trace per user session
- Automatic cleanup on session expiration
- Isolation between users
- No cross-session interference

---

### 4. TraceParameterRegistry

**Location**: `org.openl.studio.projects.service.trace.TraceParameterRegistry`

#### Design

```java
@Component
@SessionScope
public class TraceParameterRegistry {

    private final AtomicInteger idGenerator = new AtomicInteger(0);
    private final Map<Integer, ParameterWithValueDeclaration> parameters = new ConcurrentHashMap<>();

    /**
     * Registers parameter and returns unique ID.
     */
    public int register(ParameterWithValueDeclaration param) {
        int id = idGenerator.incrementAndGet();
        parameters.put(id, param);
        return id;
    }

    /**
     * Retrieves parameter by ID.
     */
    public ParameterWithValueDeclaration get(int id) {
        return parameters.get(id);
    }

    /**
     * Clears all registered parameters.
     */
    public void clear() {
        idGenerator.set(0);
        parameters.clear();
    }
}
```

**Purpose**: Store large parameter values for lazy loading
- Prevents bloated initial responses
- On-demand retrieval via parameter ID
- Session-scoped to prevent memory leaks

---

### 5. TraceNodeViewMapper

**Location**: `org.openl.studio.projects.model.trace.TraceNodeViewMapper`

#### Responsibilities

1. Convert ITracerObject to TraceNodeView DTOs
2. Serialize parameter values with JSON Schema generation
3. Decide lazy loading based on type complexity
4. Format display names via TraceFormatter

#### Key Methods

```java
public class TraceNodeViewMapper {

    private final ObjectMapper objectMapper;
    private final TraceParameterRegistry parameterRegistry;

    /**
     * Creates simple node view (for list responses).
     */
    public TraceNodeView createSimpleNode(ITracerObject element,
                                          TraceHelper traceHelper,
                                          boolean showRealNumbers) {
        Integer key = traceHelper.getNodeKey(element);
        String title = TraceFormatter.getDisplayName(element, showRealNumbers);
        String tooltip = element.getTooltip();
        String type = getNodeType(element);
        boolean lazy = hasChildren(element);
        String extraClasses = getExtraClasses(element);

        return new TraceNodeView.Builder()
            .key(key)
            .title(title)
            .tooltip(tooltip)
            .type(type)
            .lazy(lazy)
            .extraClasses(extraClasses)
            .build();
    }

    /**
     * Creates detailed node view with parameters/context/result.
     */
    public TraceNodeView createDetailedNode(ITracerObject element,
                                            TraceHelper traceHelper,
                                            boolean showRealNumbers) {
        var builder = createSimpleNode(element, traceHelper, showRealNumbers).toBuilder();

        if (element instanceof ATableTracerNode tableNode) {
            // Extract parameters
            List<TraceParameterValue> params = extractParameters(tableNode, true);
            builder.parameters(params);

            // Extract context
            if (tableNode.getRuntimeContext() != null) {
                builder.context(buildParameterValue("context",
                    tableNode.getRuntimeContext(), false));
            }
        }

        // Extract result
        if (element.getResult() != null) {
            builder.result(buildParameterValue("result",
                element.getResult(), false));
        }

        // Extract errors
        if (element instanceof SimpleTracerObject simpleTracer) {
            builder.errors(simpleTracer.getErrors());
        }

        return builder.build();
    }

    /**
     * Builds parameter value with optional lazy loading.
     */
    public TraceParameterValue buildParameterValue(ParameterWithValueDeclaration param,
                                                    boolean preferLazy) {
        Class<?> type = param.getType().getInstanceClass();
        Object value = param.getValue();

        // Decide if lazy loading needed
        boolean shouldBeLazy = preferLazy && isComplexType(type, value);

        if (shouldBeLazy) {
            // Register for lazy loading
            int parameterId = parameterRegistry.register(param);
            return new TraceParameterValue(
                param.getName(),
                param.getType().getDisplayName(0),
                true,   // lazy
                parameterId,
                null,   // value not included
                generateSchema(type)
            );
        } else {
            // Include value directly
            return new TraceParameterValue(
                param.getName(),
                param.getType().getDisplayName(0),
                false,  // not lazy
                null,   // no parameter ID
                objectMapper.valueToTree(value),
                generateSchema(type)
            );
        }
    }

    private boolean isComplexType(Class<?> type, Object value) {
        // Arrays and collections
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return true;
        }
        // Custom objects (not primitives/wrappers/strings)
        if (!type.isPrimitive() && !isWrapperType(type) && type != String.class) {
            return true;
        }
        return false;
    }
}
```

---

### 6. TraceTableHtmlService

**Location**: `org.openl.studio.projects.service.trace.TraceTableHtmlService`

#### Purpose

Renders traced tables as HTML with execution path highlighting.

#### Implementation

```java
@Service
public class TraceTableHtmlService {

    public String renderTraceTableHtml(TraceHelper traceHelper,
                                        int nodeId,
                                        ProjectModel projectModel,
                                        boolean showFormulas) {
        // Get trace object
        ITracerObject traceObject = traceHelper.getTableTracer(nodeId);
        if (traceObject == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }

        // Extract table syntax node
        TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(traceObject);
        if (tableSyntaxNode == null) {
            return "";  // No table to render
        }

        // Create grid filters for trace highlighting
        List<IGridFilter> filters = createTraceFilters(traceObject);

        // Build table model with filters
        TableModel tableModel = TableModel.builder()
            .tableSyntaxNode(tableSyntaxNode)
            .filters(filters)
            .showFormulas(showFormulas)
            .build();

        // Render to HTML
        HTMLRenderer renderer = new HTMLRenderer(tableModel);
        return renderer.render();
    }

    private List<IGridFilter> createTraceFilters(ITracerObject traceObject) {
        List<IGridFilter> filters = new ArrayList<>();

        // Highlight traced cells
        for (ITracerObject child : traceObject.getChildren()) {
            if (child instanceof DTRuleTraceObject ruleTrace) {
                // Highlight matched/unmatched conditions
                filters.add(new TracedCellFilter(ruleTrace.getCellRef(),
                    ruleTrace.isSuccessful() ? "matched" : "unmatched"));
            }
            // ... other trace types
        }

        return filters;
    }
}
```

---

### 7. TraceExportService

**Location**: `org.openl.studio.projects.service.trace.TraceExportService`

#### Purpose

Exports trace tree to plain text format for archiving and offline analysis.

#### Implementation

```java
@Service
public class TraceExportService {

    private static final long EXPORT_TIMEOUT_MS = 30_000; // 30 seconds

    /**
     * Exports trace tree to writer with streaming (no RAM buffering).
     *
     * @param traceHelper Contains the trace tree to export
     * @param writer Output destination (usually response writer)
     * @param showRealNumbers Whether to show exact numbers
     * @throws TimeoutException if export exceeds timeout
     */
    public void exportTrace(TraceHelper traceHelper, Writer writer,
                            boolean showRealNumbers) throws TimeoutException, IOException {
        long startTime = System.currentTimeMillis();

        ITracerObject root = traceHelper.getTableTracer(0);
        if (root == null) {
            return;
        }

        // Export with tree formatting
        exportNode(root, writer, "", true, showRealNumbers, startTime);
    }

    private void exportNode(ITracerObject node, Writer writer,
                            String prefix, boolean isLast,
                            boolean showRealNumbers, long startTime)
                            throws TimeoutException, IOException {
        // Check timeout
        if (System.currentTimeMillis() - startTime > EXPORT_TIMEOUT_MS) {
            throw new TimeoutException("Export timeout exceeded");
        }

        // Write current node
        String title = TraceFormatter.getDisplayName(node, showRealNumbers);
        writer.write(prefix);
        writer.write(isLast ? "└── " : "├── ");
        writer.write(title);
        writer.write("\n");

        // Recurse to children
        List<ITracerObject> children = new ArrayList<>(node.getChildren());
        for (int i = 0; i < children.size(); i++) {
            boolean childIsLast = (i == children.size() - 1);
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            exportNode(children.get(i), writer, childPrefix, childIsLast,
                       showRealNumbers, startTime);
        }
    }
}
```

**Features**:
- **Streaming output**: Writes directly to response writer, no buffering
- **Timeout protection**: Limits export time for very large traces
- **Tree formatting**: Uses box-drawing characters for visual hierarchy
- **Memory efficient**: Processes nodes one at a time

---

### 8. TableInputParserService

**Location**: `org.openl.studio.projects.service.trace.TableInputParserService`

#### Purpose

Parses JSON input for trace execution, supporting multiple input formats.

#### Supported Formats

**Format 1: Structured (with runtime context)**
```json
{
  "runtimeContext": {
    "lob": "Auto",
    "usState": "CA"
  },
  "params": {
    "age": 25,
    "coverage": "Full"
  }
}
```

**Format 2: Raw parameters (no context)**
```json
{
  "age": 25,
  "coverage": "Full"
}
```

**Format 3: Array of positional parameters**
```json
[25, "Full"]
```

#### Implementation

```java
@Service
public class TableInputParserService {

    /**
     * Parses input JSON, auto-detecting format.
     *
     * @param inputJson Raw JSON string (may be null)
     * @param method Method to match parameters against
     * @param objectMapper Configured Jackson mapper
     * @return ParseResult with params array and optional runtime context
     */
    public ParseResult parseInput(String inputJson, IOpenMethod method,
                                   ObjectMapper objectMapper) {
        if (inputJson == null || inputJson.isBlank()) {
            return new ParseResult(new Object[0], null);
        }

        JsonNode root = objectMapper.readTree(inputJson);

        // Check for structured format (has "params" or "runtimeContext")
        if (root.has("params") || root.has("runtimeContext")) {
            return parseStructured(root, method, objectMapper);
        }

        // Check for array format
        if (root.isArray()) {
            return parseArray(root, method, objectMapper);
        }

        // Default: raw object with named parameters
        return parseRawObject(root, method, objectMapper);
    }

    public record ParseResult(Object[] params, IRulesRuntimeContext runtimeContext) {}
}
```

**Features**:
- **Format auto-detection**: Supports multiple JSON formats
- **Type conversion**: Converts JSON to correct Java types using method signature
- **Runtime context extraction**: Parses IRulesRuntimeContext properties
- **Error handling**: Clear error messages for malformed input

---

### 9. TreeBuildTracer (Core)

**Location**: `org.openl.rules.webstudio.web.trace.TreeBuildTracer`

#### Core Tracing Engine

The central component that collects trace information during rule execution.

``` mermaid
classDiagram
    class Tracer {
        <<singleton>>
        +static instance: Tracer
        +put(TracerKey, Object)
        +invoke(...)
    }

    class TreeBuildTracer {
        -ThreadLocal~ITracerObject~ traceTreeHolder
        -ThreadLocal~Boolean~ lazyNodesHolder
        -ThreadLocal~Map~ cacheHolder
        +initialize(boolean useLazy)
        +destroy()
        +doPut(TracerKey, Object)
        +doBegin(...)
        +doEnd(...)
        +doInvoke(...)
    }

    class ITracerObject {
        <<interface>>
        +getParent()
        +setParent(parent)
        +addChild(child)
        +getChildren()
        +getResult()
        +getParameters()
    }

    class LazyTracerNodeObject {
        -IOpenMethodExecutor executor
        -Object target
        -Object[] params
        +materialize()
    }

    Tracer <|-- TreeBuildTracer
    TreeBuildTracer --> ITracerObject
    ITracerObject <|-- LazyTracerNodeObject
```

#### ThreadLocal Design

```java
public class TreeBuildTracer extends Tracer {

    // Per-thread trace tree root
    private static final ThreadLocal<ITracerObject> traceTreeHolder = new ThreadLocal<>();

    // Whether to create lazy nodes
    private static final ThreadLocal<Boolean> lazyNodesHolder = new ThreadLocal<>();

    // Cache for node deduplication
    private static final ThreadLocal<Map<TracerKeyNode, SimpleTracerObject>> cacheHolder =
        new ThreadLocal<>();

    /**
     * Initializes tracing for current thread.
     */
    public static void initialize(boolean useLazy) {
        traceTreeHolder.set(new RootTracerObject());
        lazyNodesHolder.set(useLazy);
        cacheHolder.set(new HashMap<>());

        // Replace global tracer instance
        Tracer.instance = new TreeBuildTracer();
    }

    /**
     * Cleans up after tracing.
     */
    public static void destroy() {
        ITracerObject root = traceTreeHolder.get();

        // Clean up ThreadLocal state
        traceTreeHolder.remove();
        lazyNodesHolder.remove();
        cacheHolder.remove();

        // Restore default tracer
        Tracer.instance = Tracer.DEFAULT;
    }

    /**
     * Called when entering a traced method.
     */
    @Override
    protected void doBegin(IOpenMethodExecutor executor, Object target,
                           Object[] params, IRuntimeEnv env) {
        ITracerObject parent = getCurrentNode();

        ITracerObject traceObject;
        if (useLazyNodes() && canBeLazyNode(executor)) {
            // Create lazy node (materializes on first access)
            traceObject = new LazyTracerNodeObject(executor, target, params, env);
        } else {
            // Create immediate trace object
            traceObject = TracedObjectFactory.getTracedObject(executor, target, params, env);
        }

        // Link to parent
        traceObject.setParent(parent);
        parent.addChild(traceObject);

        // Push to stack
        pushNode(traceObject);
    }

    /**
     * Called when exiting a traced method.
     */
    @Override
    protected void doEnd(IOpenMethodExecutor executor, Object result, Throwable error) {
        ITracerObject current = popNode();

        if (current instanceof SimpleTracerObject simpleTracer) {
            simpleTracer.setResult(result);
            if (error != null) {
                simpleTracer.addError(error);
            }
        }
    }
}
```

**Why ThreadLocal?**
- Thread safety for concurrent trace execution
- Isolated trace trees per thread
- No synchronization overhead
- Automatic cleanup on thread completion

---

### 10. TraceHelper

**Location**: `org.openl.rules.ui.TraceHelper`

#### Bidirectional Node Cache

```java
public class TraceHelper {

    // BidiMap: Integer ID <-> ITracerObject
    private final BidiMap<Integer, ITracerObject> traceTreeCache = new DualHashBidiMap<>();

    /**
     * Caches the entire trace tree with integer IDs.
     */
    public void cacheTraceTree(ITracerObject root) {
        int id = 0;
        Queue<ITracerObject> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            ITracerObject node = queue.poll();
            traceTreeCache.put(id++, node);

            // Initialize lazy children
            if (node instanceof LazyTracerNodeObject lazyNode) {
                lazyNode.materialize();
            }

            for (ITracerObject child : node.getChildren()) {
                queue.add(child);
            }
        }
    }

    /**
     * Gets node by ID (O(1) lookup).
     */
    public ITracerObject getTableTracer(int elementId) {
        ITracerObject node = traceTreeCache.get(elementId);

        // Initialize lazy children if needed
        if (node != null && node instanceof LazyTracerNodeObject lazyNode) {
            lazyNode.materialize();
        }

        return node;
    }

    /**
     * Gets ID for node (O(1) reverse lookup).
     */
    public Integer getNodeKey(ITracerObject node) {
        return traceTreeCache.getKey(node);
    }
}
```

**Why BidiMap?**
- O(1) lookup by ID (for API requests)
- O(1) reverse lookup by node (for key generation)
- Memory efficient (single storage)

---

## Data Flow

### Flow 1: Start Trace Execution

```
┌──────┐            ┌──────────┐           ┌─────────┐          ┌───────────┐
│Client│            │Controller│           │Executor │          │TreeBuild  │
│      │            │          │           │Service  │          │Tracer     │
└──┬───┘            └────┬─────┘           └────┬────┘          └─────┬─────┘
   │                     │                      │                     │
   │ POST /trace         │                      │                     │
   ├────────────────────>│                      │                     │
   │                     │                      │                     │
   │                     │ cancelIfAny()        │                     │
   │                     ├──────┐               │                     │
   │                     │      │               │                     │
   │                     │<─────┘               │                     │
   │                     │                      │                     │
   │                     │ traceMethod()        │                     │
   │                     ├─────────────────────>│                     │
   │                     │                      │                     │
   │                     │                      │ initialize(lazy)    │
   │                     │                      ├────────────────────>│
   │                     │                      │                     │
   │                     │                      │ projectModel.trace()│
   │                     │                      ├──────┐              │
   │                     │                      │      │ (execution)  │
   │                     │                      │<─────┘              │
   │                     │                      │                     │
   │                     │                      │ destroy()           │
   │                     │                      ├────────────────────>│
   │                     │                      │                     │
   │                     │ setTask(future)      │                     │
   │                     ├──────┐               │                     │
   │                     │      │               │                     │
   │                     │<─────┘               │                     │
   │                     │                      │                     │
   │  202 Accepted       │                      │                     │
   │<────────────────────┤                      │                     │
```

---

### Flow 2: Get Root Nodes

```
┌──────┐            ┌──────────┐           ┌──────────┐          ┌───────┐
│Client│            │Controller│           │Registry  │          │Mapper │
└──┬───┘            └────┬─────┘           └────┬─────┘          └───┬───┘
   │                     │                      │                    │
   │ GET /trace/nodes    │                      │                    │
   ├────────────────────>│                      │                    │
   │                     │                      │                    │
   │                     │ hasTask(projectId)   │                    │
   │                     ├─────────────────────>│                    │
   │                     │                      │                    │
   │                     │      true            │                    │
   │                     │<─────────────────────┤                    │
   │                     │                      │                    │
   │                     │ isDone(projectId)    │                    │
   │                     ├─────────────────────>│                    │
   │                     │                      │                    │
   │                     │      true            │                    │
   │                     │<─────────────────────┤                    │
   │                     │                      │                    │
   │                     │ getTraceHelper()     │                    │
   │                     ├─────────────────────>│                    │
   │                     │                      │                    │
   │                     │    TraceHelper       │                    │
   │                     │<─────────────────────┤                    │
   │                     │                      │                    │
   │                     │ createSimpleNodes()  │                    │
   │                     ├───────────────────────────────────────────>│
   │                     │                      │                    │
   │                     │  List<TraceNodeView> │                    │
   │                     │<───────────────────────────────────────────┤
   │                     │                      │                    │
   │ List<TraceNodeView> │                      │                    │
   │<────────────────────┤                      │                    │
```

---

### Flow 3: Get Node Details with Lazy Parameter

```
┌──────┐            ┌──────────┐           ┌───────┐          ┌──────────┐
│Client│            │Controller│           │Mapper │          │ParamReg  │
└──┬───┘            └────┬─────┘           └───┬───┘          └────┬─────┘
   │                     │                     │                   │
   │ GET /nodes/5        │                     │                   │
   ├────────────────────>│                     │                   │
   │                     │                     │                   │
   │                     │ createDetailedNode()│                   │
   │                     ├────────────────────>│                   │
   │                     │                     │                   │
   │                     │                     │ isComplexType()   │
   │                     │                     ├───────┐           │
   │                     │                     │       │ true      │
   │                     │                     │<──────┘           │
   │                     │                     │                   │
   │                     │                     │ register(param)   │
   │                     │                     ├──────────────────>│
   │                     │                     │                   │
   │                     │                     │     parameterId=7 │
   │                     │                     │<──────────────────┤
   │                     │                     │                   │
   │                     │   TraceNodeView     │                   │
   │                     │   (lazy param)      │                   │
   │                     │<────────────────────┤                   │
   │                     │                     │                   │
   │ {parameters: [{     │                     │                   │
   │   lazy: true,       │                     │                   │
   │   parameterId: 7    │                     │                   │
   │ }]}                 │                     │                   │
   │<────────────────────┤                     │                   │
   │                     │                     │                   │
   │ GET /parameters/7   │                     │                   │
   ├────────────────────>│                     │                   │
   │                     │                     │                   │
   │                     │                     │ get(7)            │
   │                     │                     ├──────────────────>│
   │                     │                     │                   │
   │                     │                     │     param value   │
   │                     │                     │<──────────────────┤
   │                     │                     │                   │
   │                     │ buildParameterValue │                   │
   │                     │ (preferLazy=false)  │                   │
   │                     ├────────────────────>│                   │
   │                     │                     │                   │
   │                     │  TraceParameterValue│                   │
   │                     │  (full value)       │                   │
   │                     │<────────────────────┤                   │
   │                     │                     │                   │
   │ {lazy: false,       │                     │                   │
   │  value: {...}}      │                     │                   │
   │<────────────────────┤                     │                   │
```

---

### Flow 4: Export Trace

```
┌──────┐            ┌──────────┐           ┌──────────┐          ┌───────────┐
│Client│            │Controller│           │Export    │          │TraceHelper│
│      │            │          │           │Service   │          │           │
└──┬───┘            └────┬─────┘           └────┬─────┘          └─────┬─────┘
   │                     │                      │                      │
   │ GET /trace/export   │                      │                      │
   │ ?release=true       │                      │                      │
   ├────────────────────>│                      │                      │
   │                     │                      │                      │
   │                     │ getCompletedTraceHelper()                   │
   │                     ├─────────────────────────────────────────────>│
   │                     │                      │                      │
   │                     │      traceHelper     │                      │
   │                     │<─────────────────────────────────────────────┤
   │                     │                      │                      │
   │                     │ setContentDisposition│                      │
   │                     │ (trace.txt)          │                      │
   │                     │                      │                      │
   │                     │ exportTrace(writer)  │                      │
   │                     ├─────────────────────>│                      │
   │                     │                      │                      │
   │                     │                      │ getTableTracer(0)    │
   │                     │                      ├─────────────────────>│
   │                     │                      │                      │
   │                     │                      │      root node       │
   │                     │                      │<─────────────────────┤
   │                     │                      │                      │
   │                     │                      │ (recursive export    │
   │                     │                      │  with tree format)   │
   │                     │                      │                      │
   │                     │      (streams to     │                      │
   │                     │       response)      │                      │
   │                     │<─────────────────────┤                      │
   │                     │                      │                      │
   │                     │ if release:          │                      │
   │                     │   clear registries   │                      │
   │                     │                      │                      │
   │ text/plain          │                      │                      │
   │ (trace.txt)         │                      │                      │
   │<────────────────────┤                      │                      │
```

---

## Session Management

### Session Lifecycle

```
State: NO_TRACE
  │
  │ POST /trace (start new trace)
  ▼
State: TRACE_PENDING
  │
  │ (async execution in progress)
  │
  ├─── WebSocket: STARTED
  │
  │ (execution completes)
  │
  ├─── WebSocket: COMPLETED ──────────────────┐
  │                                           │
  ├─── WebSocket: ERROR ──────────────────────┤
  │                                           ▼
  │                                   State: TRACE_COMPLETE
  │                                           │
  │                                           ├─── GET /trace
  │                                           ├─── GET /nodes
  │                                           ├─── GET /nodes/{id}
  │                                           ├─── GET /parameters/{id}
  │                                           ├─── GET /nodes/{id}/table
  │                                           │
  │                                           ▼
  │                                   State: TRACE_COMPLETE
  │
  └─── DELETE /trace ───────────────────────> State: NO_TRACE
  │
  └─── POST /trace (new trace) ─────────────> State: TRACE_PENDING
                                              (previous cancelled)
```

### One Trace Per Session

**Design Decision**: Only one trace can be active per user session.

**Implementation**:
```java
public void setTask(String projectId, String tableId,
                    CompletableFuture<ITracerObject> task,
                    TraceHelper traceHelper) {
    // Cancel previous trace if exists
    cancelIfAny();

    // Register new trace
    this.currentProjectId = projectId;
    this.currentTableId = tableId;
    this.currentTask = task;
    this.currentTraceHelper = traceHelper;
}
```

**Why?**
- Prevents memory exhaustion from multiple concurrent traces
- Simplifies session state management
- Clear user expectation (one trace at a time)
- Automatic cleanup of stale traces

---

## Lazy Loading Strategy

### Lazy Nodes (ITracerObject)

**Problem**: Large execution traces can consume significant memory.

**Solution**: Lazy nodes defer child creation until accessed.

``` mermaid
sequenceDiagram
    participant E as Execution
    participant T as TreeBuildTracer
    participant L as LazyTracerNode

    E->>T: doBegin(method)
    T->>T: canBeLazyNode()?
    alt Can be lazy
        T->>L: new LazyTracerNodeObject(executor, params)
        L-->>T: lazyNode (no children yet)
    else Cannot be lazy
        T->>T: TracedObjectFactory.create()
    end
    T-->>E: continue

    Note over L: Children not created

    E->>L: getChildren()
    L->>L: materialize()
    L->>L: Execute and capture children
    L-->>E: children (now materialized)
```

**Implementation**:
```java
public class LazyTracerNodeObject implements ITracerObject {
    private final IOpenMethodExecutor executor;
    private final Object target;
    private final Object[] params;
    private final IRuntimeEnv env;

    private volatile ITracerObject realNode;
    private volatile boolean materialized = false;

    @Override
    public Iterable<ITracerObject> getChildren() {
        materialize();
        return realNode.getChildren();
    }

    private void materialize() {
        if (!materialized) {
            synchronized (this) {
                if (!materialized) {
                    // Preserve context classloader
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    try {
                        // Execute with fresh trace context
                        TreeBuildTracer.initialize(false);
                        realNode = TracedObjectFactory.getTracedObject(
                            executor, target, params, env);
                        executor.invoke(target, params, env);
                    } finally {
                        TreeBuildTracer.destroy();
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                    materialized = true;
                }
            }
        }
    }
}
```

---

### Lazy Parameters (TraceParameterValue)

**Problem**: Complex parameter values (large objects, arrays) bloat API responses.

**Solution**: Register complex parameters for on-demand retrieval.

**Decision Logic**:
```java
private boolean isComplexType(Class<?> type, Object value) {
    // Arrays are lazy
    if (type.isArray()) {
        return true;
    }

    // Collections are lazy
    if (Collection.class.isAssignableFrom(type)) {
        return true;
    }

    // Maps are lazy
    if (Map.class.isAssignableFrom(type)) {
        return true;
    }

    // Custom objects are lazy (not primitives/wrappers/strings)
    if (!type.isPrimitive()
        && !isWrapperType(type)
        && type != String.class
        && type != BigDecimal.class
        && type != Date.class) {
        return true;
    }

    return false;
}
```

**Benefits**:
- Initial response is fast and small
- Client loads values on-demand
- Memory efficient for large traces
- Works well with tree UI (expand on click)

---

## Integration with Rules Engine

### Execution Flow with Tracing

```
1. Client: POST /trace
   │
   ▼
2. TraceExecutorService.traceMethod()
   │
   ├─ TreeBuildTracer.initialize(true)
   │  └─ Sets ThreadLocal tracer state
   │
   ▼
3. ProjectModel.traceElement(testSuite)
   │
   ├─ Creates TestSuite with TestDescription
   │
   └─ TestSuiteMethod.invoke()
      │
      ├─ For each test case:
      │  ├─ Setup runtime context
      │  │
      │  ├─ Tracer.invoke()  ────────────────┐
      │  │                                   │
      │  │  ┌─────────────────────────────────┘
      │  │  │
      │  │  ▼
      │  │  TreeBuildTracer.doBegin()
      │  │  └─ Create ITracerObject
      │  │  └─ Push to trace stack
      │  │
      │  ├─ Execute method
      │  │  │
      │  │  ├─ Decision table: evaluate conditions
      │  │  │  └─ Tracer.put() for each condition
      │  │  │
      │  │  ├─ Spreadsheet: evaluate cells
      │  │  │  └─ Tracer.put() for each cell
      │  │  │
      │  │  └─ Nested method calls
      │  │     └─ Recursive Tracer.invoke()
      │  │
      │  ├─ TreeBuildTracer.doEnd()
      │  │  └─ Pop from trace stack
      │  │  └─ Store result
      │  │
      │  └─ Capture result
      │
      └─ Return trace root
   │
   ▼
4. TraceHelper.cacheTraceTree(root)
   │
   ├─ BFS traversal
   └─ Assign integer IDs
   │
   ▼
5. TreeBuildTracer.destroy()
   │
   └─ Clean up ThreadLocal state
   │
   ▼
6. CompletableFuture.complete(root)
   │
   └─ WebSocket: COMPLETED
```

---

## WebSocket Progress Notifications

### Architecture

```
┌──────────────┐          ┌─────────────────────┐          ┌─────────────┐
│ TraceExecutor│          │SocketListener       │          │ WebSocket   │
│ Service      │          │ Factory             │          │ Session     │
└──────┬───────┘          └──────────┬──────────┘          └──────┬──────┘
       │                             │                            │
       │ onStatusChanged(PENDING)    │                            │
       ├────────────────────────────>│                            │
       │                             │                            │
       │                             │ send({status: PENDING})    │
       │                             ├───────────────────────────>│
       │                             │                            │
       │ onStatusChanged(STARTED)    │                            │
       ├────────────────────────────>│                            │
       │                             │                            │
       │                             │ send({status: STARTED})    │
       │                             ├───────────────────────────>│
       │                             │                            │
       │ ... execution ...           │                            │
       │                             │                            │
       │ onStatusChanged(COMPLETED)  │                            │
       ├────────────────────────────>│                            │
       │                             │                            │
       │                             │ send({status: COMPLETED})  │
       │                             ├───────────────────────────>│
```

### Message Format

```json
{
  "type": "trace_progress",
  "projectId": "MyProject",
  "tableId": "TABLE_BankRating",
  "status": "COMPLETED",
  "timestamp": "2026-01-28T10:30:00Z"
}
```

### Status Values

| Status | Description | Next Actions |
|--------|-------------|--------------|
| `PENDING` | Task queued | Wait |
| `STARTED` | Execution in progress | Wait |
| `COMPLETED` | Success | Fetch results |
| `INTERRUPTED` | Cancelled | Start new trace |
| `ERROR` | Failed | View error, retry |

---

## Performance Considerations

### 1. Async Execution

**Challenge**: Trace execution can be slow for complex rules.

**Solution**: Spring `@Async` with dedicated thread pool.

```java
@Configuration
public class AsyncConfig {

    @Bean(name = "testSuiteExecutor")
    public Executor testSuiteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("trace-");
        executor.initialize();
        return executor;
    }
}
```

**Benefits**:
- Non-blocking request handling
- Dedicated thread pool for traces
- Configurable pool size
- Request returns immediately (202)

---

### 2. Memory Optimization

**Challenge**: Large trace trees consume memory.

**Optimizations**:

1. **Lazy Nodes**: Children created on-demand
2. **Lazy Parameters**: Large values not serialized until requested
3. **Session Scope**: One trace per session prevents accumulation
4. **Automatic Cancellation**: Previous trace cancelled when new one starts

**Memory Estimate** (typical trace):
```
Base trace: ~100 KB
Per node: ~500 bytes
100 nodes: ~50 KB
1000 nodes: ~500 KB
```

---

### 3. BidiMap for O(1) Lookups

**Challenge**: Need to look up nodes by ID and get IDs for nodes.

**Solution**: Apache Commons BidiMap (DualHashBidiMap).

```java
private final BidiMap<Integer, ITracerObject> traceTreeCache = new DualHashBidiMap<>();

// O(1) lookup by ID
ITracerObject node = traceTreeCache.get(5);

// O(1) reverse lookup
Integer id = traceTreeCache.getKey(node);
```

---

### 4. JSON Schema Caching

**Challenge**: JSON Schema generation is expensive.

**Solution**: Cache schemas by type.

```java
private final Map<Class<?>, ObjectNode> schemaCache = new ConcurrentHashMap<>();

private ObjectNode generateSchema(Class<?> type) {
    return schemaCache.computeIfAbsent(type, this::doGenerateSchema);
}
```

---

## Security Architecture

### 1. Authentication

All trace endpoints require authenticated user:
- Session-based authentication
- OAuth2/SAML integration supported
- Personal Access Tokens supported

### 2. Authorization

Project-level permissions checked:
```java
@ProjectId @PathVariable("projectId") RulesProject project
// @ProjectId annotation validates user access to project
```

### 3. Input Validation

- `tableId` validated against project tables
- `testRanges` parsed with bounds checking
- JSON parameters validated before execution

### 4. Resource Limits

- Session scope prevents unbounded trace accumulation
- Thread pool limits concurrent trace executions
- Lazy loading prevents memory exhaustion

---

## Design Decisions

### Decision 1: Async with CompletableFuture

**Problem**: Trace execution can be slow.

**Options**:
1. Synchronous (block until complete)
2. Async with polling
3. Async with WebSocket

**Decision**: Async with CompletableFuture + WebSocket

**Rationale**:
- Non-blocking client experience
- Real-time progress updates
- Scalable under load
- Standard Java async pattern

---

### Decision 2: One Trace Per Session

**Problem**: How many concurrent traces per user?

**Options**:
1. Unlimited
2. One per project
3. One per session

**Decision**: One per session

**Rationale**:
- Simple memory management
- Clear user expectation
- Automatic cleanup
- Sufficient for debugging use case

---

### Decision 3: Lazy Loading for Parameters

**Problem**: Complex parameters bloat responses.

**Options**:
1. Always include full values
2. Always lazy load
3. Smart lazy loading by type

**Decision**: Smart lazy loading by type

**Rationale**:
- Simple types inline (fast)
- Complex types lazy (memory efficient)
- Good UX (expand on demand)
- Configurable threshold

---

### Decision 4: BidiMap for Node Cache

**Problem**: Need efficient lookup by ID and reverse lookup.

**Options**:
1. Two separate maps
2. Linear search for reverse
3. BidiMap

**Decision**: BidiMap (DualHashBidiMap)

**Rationale**:
- O(1) both directions
- Single data structure
- Standard Apache Commons library
- Memory efficient

---

## Future Enhancements

### 1. Trace Comparison

Compare traces from different runs:
```json
GET /trace/compare?traceId1=abc&traceId2=def
```

### 2. Additional Export Formats

Extend export to support additional formats:
```json
GET /trace/export?format=json|xml|html
```
(Currently only text format is supported)

### 3. Performance Profiling

Add timing information to trace nodes:
```json
{
  "key": 5,
  "title": "Rule 1",
  "duration": 12,
  "durationUnit": "ms"
}
```

### 4. Breakpoint Support

Pause execution at specific rules:
```json
POST /trace?breakpoint=DT_RiskRating:Rule3
```

### 5. Variable Watches

Monitor specific variables during execution:
```json
POST /trace
{
  "watches": ["age", "score", "result"]
}
```

---

## Conclusion

The Projects Trace API provides a comprehensive debugging facility for OpenL rules with:

1. **7 REST Endpoints**: Complete trace lifecycle management
2. **Async Execution**: Non-blocking trace with WebSocket progress
3. **Hierarchical Tree**: Complete execution tree with lazy loading
4. **Memory Efficiency**: Lazy nodes and parameters
5. **Session Management**: One trace per session with auto-cleanup
6. **Integration**: Deep integration with rules engine via TreeBuildTracer
7. **Performance**: BidiMap cache, async execution, thread pool
8. **Export**: Text export with streaming and memory release

The API is currently in BETA and will evolve based on user feedback.

---

## References

- **API Documentation**: [projects-trace-api.md](projects-trace-api.md)
- **Source Code**: `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/rest/controller/ProjectsTraceController.java`
- **Tracer Core**: `/STUDIO/org.openl.rules.webstudio/src/org/openl/rules/webstudio/web/trace/TreeBuildTracer.java`
- **Export Service**: `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/service/trace/TraceExportService.java`
- **Input Parser**: `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/service/trace/TableInputParserService.java`
- **Integration Tests**: `/ITEST/itest.studio/`

---

**Last Updated**: 2026-01-29
**Version**: 6.0.0-SNAPSHOT
**Status**: BETA

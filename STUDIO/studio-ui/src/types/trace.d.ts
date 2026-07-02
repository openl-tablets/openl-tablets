/**
 * Interactive debug session status.
 *
 * - pending: created, worker not started yet
 * - running: executing, not suspended
 * - suspended: paused at a breakpoint or step point; the stack can be inspected
 * - completed: finished normally
 * - error: failed with an error
 * - terminated: cancelled before finishing
 */
export type DebugStatus = 'pending' | 'running' | 'suspended' | 'completed' | 'error' | 'terminated'

/** Step command issued to a suspended session. */
export type StepType = 'into' | 'over' | 'out'

/**
 * Kind of rule table a stack frame represents (mirrors the backend FrameKind codes). `stepRef` is not a
 * table: a reference to a step that already executed elsewhere in the same frame.
 */
export type FrameKind = 'decisionTable' | 'spreadsheet' | 'method' | 'cmatch' | 'tbasic' | 'tbasicMethod' | 'stepRef'

/** Location type of the current line inside a frame. */
export type LocationKind = 'cell' | 'dtrule' | 'operation'

/** A rule table that can be a breakpoint target (set a breakpoint by name before it runs). */
export interface BreakpointTableView {
    /** Table name — the breakpoint key (stops on any same-named table) and the search term. */
    name: string
}

/**
 * The current line being evaluated inside a stack frame.
 */
export interface DebugLocationView {
    /** Location type of the current line */
    kind: LocationKind
    /** Cell row index, when applicable */
    row?: number | null
    /** Cell column index, when applicable */
    column?: number | null
    /** Short cell reference such as 'R2C3' */
    ref?: string | null
    /** Human-readable description of the current line */
    label?: string | null
}

/**
 * One frame of the live execution stack.
 */
/** A table chosen at runtime from versions overloaded by dimension properties (a dispatcher). */
export interface DispatchInfo {
    /** The overloaded versions, each labelled by its dimension properties, with the chosen one flagged. */
    candidates: DispatchCandidate[]
}

/** One overloaded version of a dispatched table. */
export interface DispatchCandidate {
    /** The version's dimension properties (e.g. `effectiveDate: 01/01/2020`), or its name. */
    label: string
    /** Whether the dispatcher selected this version for the current runtime context. */
    chosen: boolean
}

export interface DebugFrameView {
    /** Position in the stack, 0 for the root call */
    index: number
    /** Frame depth, 1 for the root call */
    depth: number
    /** Source URI of the frame's table, used for breakpoints and table rendering */
    uri: string
    /** Stable id of the frame's table, used to fetch its raw grid from the Tables API */
    tableId: string
    /** Display name of the frame's table */
    name: string
    /** Kind of the frame's table */
    kind: FrameKind
    /** Current line inside the frame, or undefined at entry */
    location?: DebugLocationView | null
    /** Whether this is the current (top) frame */
    active: boolean
    /** Whether the frame has returned */
    completed: boolean
    /** Whether the frame failed */
    error: boolean
    /**
     * The frame's sub-steps (spreadsheet cells or decision-table rules) with their status, so the call
     * tree shows every level at once. Values are omitted here and fetched per frame on demand.
     */
    steps?: StepValueView[] | null
    /** Total execution time (ms) once the frame has returned, e.g. after a step out; absent while it runs. */
    durationMillis?: number | null
    /** Own execution time (ms) once the frame has returned (total minus called tables); absent while it runs. */
    selfMillis?: number | null
    /** Set when the table was chosen by a dispatcher (versioned by dimension properties); absent otherwise. */
    dispatch?: DispatchInfo | null
}

/** How a traced cell is highlighted, shared with the spreadsheet grid and the decision panel. */
export type HighlightState = 'current' | 'result' | 'conditionTrue' | 'conditionFalse'

/** One highlighted cell of a traced table, addressed in A1 notation (e.g. `C7`). */
export interface CellHighlight {
    cell: string
    state: HighlightState
}

/** Excel cell style read from the workbook; every field is optional and absent when it is the default. */
export interface RawTableCellStyle {
    /** Background colour as #rrggbb (absent when white) */
    background?: string
    /** Font colour as #rrggbb (absent when black) */
    color?: string
    /** Horizontal alignment */
    align?: string
    /** Vertical alignment */
    valign?: string
    bold?: boolean
    italic?: boolean
    underline?: boolean
    indent?: number
}

/** One cell of a raw table grid (Tables API `?raw=true`). */
export interface RawTableCell {
    /** Cell address in A1 notation; absent on covered cells */
    cell?: string
    /** Typed cell value (number, string, boolean), or absent when empty */
    value?: string | number | boolean | null
    /** Number of columns this cell spans (>= 2), when merged */
    colspan?: number
    /** Number of rows this cell spans (>= 2), when merged */
    rowspan?: number
    /** True for a cell masked by another cell's span */
    covered?: boolean
    /** Excel cell style, present only when the raw table was requested with `styles=true` */
    style?: RawTableCellStyle
}

/** A table in raw tabular form: a 2D matrix of cells with merge geometry. */
export interface RawTableView {
    id: string
    name: string
    /** The table body as a 2D matrix indexed source[row][col] */
    source: RawTableCell[][]
    /** Full row count when the response was truncated by maxRows; absent when the whole table is returned */
    totalRows?: number
}

/**
 * A debug session failure, shaped for non-technical users.
 *
 * `summary` is the cleaned message; `table`/`location` say where it failed; `type`/`detail` carry the
 * technical exception type and stack trace for an optional drill-down.
 */
export interface DebugError {
    summary: string
    table?: string | null
    location?: string | null
    type?: string | null
    detail?: string | null
}

/**
 * The live execution stack at the current suspension.
 */
export interface DebugStackView {
    status: DebugStatus
    frames: DebugFrameView[]
    error?: DebugError | null
    /** The whole executed call tree once the trace has finished (profiling mode); absent while it runs. */
    tree?: CallNodeView | null
}

/**
 * One sub-step of a frame: a spreadsheet cell or a decision-table rule.
 * `ref` is the breakpoint key suffix (`uri#ref`).
 */
export interface StepValueView {
    ref: string
    label?: string | null
    status: 'executed' | 'current' | 'pending'
    value?: TraceParameterValue | null
    /** Tables this step called, retained in profiling mode so a returned branch can be browsed. */
    children?: CallNodeView[] | null
    /** Total execution time (ms) of an executed step: its own work plus called tables; absent if not run. */
    durationMillis?: number | null
    /** Own execution time (ms) of an executed step: total minus called tables; absent if not run. */
    selfMillis?: number | null
}

/**
 * A node of the executed call tree: a table invocation that has already returned. Structure only — no
 * values — present only when the session runs in profiling mode.
 */
export interface CallNodeView {
    uri: string
    name: string
    kind: FrameKind
    /** Total execution time in milliseconds (this table and everything it called), excluding parked time. */
    durationMillis: number
    /** Own execution time in milliseconds: total minus the time spent in the tables it called. */
    selfMillis: number
    steps: StepValueView[]
    /** Set when the table was chosen by a dispatcher (versioned by dimension properties); absent otherwise. */
    dispatch?: DispatchInfo | null
    /** For a `stepRef` node, the reference of the already-executed step it points at; absent otherwise. */
    refStep?: string | null
}

/** One evaluated decision-table condition, for one rule. */
export interface DecisionConditionView {
    condition: string
    rule: string
    matched: boolean
}

/** Plain-language explanation of a decision table's outcome: which rule fired and how each condition turned out. */
export interface DecisionView {
    firedRules: string[]
    conditions: DecisionConditionView[]
}

/**
 * Frozen variables of a stack frame, captured while execution is suspended.
 */
export interface DebugFrameVariables {
    parameters: TraceParameterValue[]
    context?: TraceParameterValue | null
    result?: TraceParameterValue | null
    steps: StepValueView[]
    /** Spreadsheet column names, so steps can be laid out as a grid (only for spreadsheet frames). */
    gridColumns?: string[] | null
    /** Spreadsheet row names (only for spreadsheet frames). */
    gridRows?: string[] | null
    /** Decision-table outcome explanation (only for decision-table frames). */
    decision?: DecisionView | null
    /** All rule names of a decision-table frame, so a breakpoint can be set on any rule. */
    ruleNames?: string[] | null
    errors: MessageDescription[]
}

/**
 * Parameter value with optional lazy loading support.
 */
export interface TraceParameterValue {
    /** Parameter name */
    name: string
    /** Type description (e.g., 'int', 'String', 'Bank') */
    description: string
    /** Whether the value is lazy-loaded (not included in response) */
    lazy: boolean
    /** Parameter ID for lazy loading (null if value is included) */
    parameterId?: number | null
    /** Full JSON value (null if lazy=true) */
    value?: any
    /** JSON Schema for the type */
    schema?: object
}

/**
 * Error/warning message description.
 */
export interface MessageDescription {
    /** Severity level */
    severity: 'ERROR' | 'WARNING' | 'INFO'
    /** Short summary message */
    summary: string
    /** Detailed message (optional) */
    detail?: string
    /** Source code location (optional) */
    sourceLocation?: string
}

/**
 * WebSocket message for debug status updates.
 */
export interface TraceProgressMessage {
    status: DebugStatus
    message?: string
}

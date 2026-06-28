/**
 * Interactive debug session status.
 *
 * - PENDING: created, worker not started yet
 * - RUNNING: executing, not suspended
 * - SUSPENDED: paused at a breakpoint or step point; the stack can be inspected
 * - COMPLETED: finished normally
 * - ERROR: failed with an error
 * - TERMINATED: cancelled before finishing
 */
export type DebugStatus = 'PENDING' | 'RUNNING' | 'SUSPENDED' | 'COMPLETED' | 'ERROR' | 'TERMINATED'

/** Step command issued to a suspended session. */
export type StepType = 'into' | 'over' | 'out'

/** Kind of rule table a stack frame represents (mirrors the backend FrameKind codes). */
export type FrameKind = 'decisionTable' | 'spreadsheet' | 'method' | 'cmatch' | 'tbasic' | 'tbasicMethod'

/** Location type of the current line inside a frame. */
export type LocationKind = 'cell' | 'dtrule' | 'operation'

/** A rule table that can be a breakpoint target (set a breakpoint by name before it runs). */
export interface BreakpointTableView {
    /** Table source URI — the breakpoint key. */
    uri: string
    /** Table display name, used to search. */
    name: string
    /** Frame kind code. */
    kind: FrameKind
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
export interface DebugFrameView {
    /** Position in the stack, 0 for the root call */
    index: number
    /** Frame depth, 1 for the root call */
    depth: number
    /** Source URI of the frame's table, used for breakpoints and table rendering */
    uri: string
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
}

/** Lightweight debug session status, used for polling. */
export interface DebugStatusView {
    status: DebugStatus
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

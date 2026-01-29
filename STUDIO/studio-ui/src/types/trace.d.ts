/**
 * Trace execution status for WebSocket progress notifications.
 */
export type TraceExecutionStatus = 'PENDING' | 'STARTED' | 'COMPLETED' | 'INTERRUPTED' | 'ERROR'

/**
 * Trace node view representing a single node in the trace tree.
 * Short view includes only basic display fields.
 * Full view includes parameters, context, result, and errors.
 */
export interface TraceNodeView {
    /** Unique node identifier */
    key: number
    /** Display name for the node */
    title: string
    /** Hover tooltip text */
    tooltip: string
    /** Node type (e.g., 'method', 'rule', 'condition', 'spreadsheet') */
    type: string
    /** Whether this node has children that can be lazy-loaded */
    lazy: boolean
    /** CSS classes for styling (e.g., 'result', 'fail', 'no_result') */
    extraClasses: string
    /** Indicates if an error occurred during execution of this trace node */
    error?: boolean
    // Detail fields (only in full view from /nodes/{nodeId})
    /** Input parameters for this traced method */
    parameters?: TraceParameterValue[]
    /** Runtime context used during execution */
    context?: TraceParameterValue
    /** Return value of the method */
    result?: TraceParameterValue
    /** Error messages if execution failed */
    errors?: MessageDescription[]
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
 * Ant Design Tree data node structure for trace tree.
 * Uses path-based keys to handle duplicate nodes (same node under multiple parents).
 */
export interface TraceTreeDataNode {
    /** Unique tree key (path-based, e.g., "24-25" for node 25 under parent 24) */
    key: string
    /** Original node ID from backend (used for API calls) */
    nodeId: number
    title: string
    tooltip?: string
    type: string
    extraClasses: string
    isLeaf: boolean
    children?: TraceTreeDataNode[]
    /** Original node data for reference */
    nodeData: TraceNodeView
}

/**
 * WebSocket message for trace progress updates.
 */
export interface TraceProgressMessage {
    status: TraceExecutionStatus
    message?: string
}

import type { TraceExecutionStatus } from 'types/trace'

export const TRACE_EXECUTION_STATUS: Readonly<Record<TraceExecutionStatus, TraceExecutionStatus>> = {
    PENDING: 'PENDING',
    STARTED: 'STARTED',
    COMPLETED: 'COMPLETED',
    INTERRUPTED: 'INTERRUPTED',
    ERROR: 'ERROR',
}

export const IN_PROGRESS_TRACE_EXECUTION_STATUSES: readonly TraceExecutionStatus[] = [
    TRACE_EXECUTION_STATUS.PENDING,
    TRACE_EXECUTION_STATUS.STARTED,
]

export const TERMINAL_TRACE_EXECUTION_STATUSES: readonly TraceExecutionStatus[] = [
    TRACE_EXECUTION_STATUS.ERROR,
    TRACE_EXECUTION_STATUS.INTERRUPTED,
]

export const isTraceExecutionInProgress = (
    status: TraceExecutionStatus | null | undefined
): boolean => status ? IN_PROGRESS_TRACE_EXECUTION_STATUSES.includes(status) : false

export const isTraceExecutionTerminal = (
    status: TraceExecutionStatus | null | undefined
): boolean => status ? TERMINAL_TRACE_EXECUTION_STATUSES.includes(status) : false

export const isTraceExecutionError = (
    status: TraceExecutionStatus | null | undefined
): boolean => status === TRACE_EXECUTION_STATUS.ERROR

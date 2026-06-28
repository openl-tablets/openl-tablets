import type { DebugStatus } from 'types/trace'

export const DEBUG_STATUS: Readonly<Record<DebugStatus, DebugStatus>> = {
    PENDING: 'PENDING',
    RUNNING: 'RUNNING',
    SUSPENDED: 'SUSPENDED',
    COMPLETED: 'COMPLETED',
    ERROR: 'ERROR',
    TERMINATED: 'TERMINATED',
}

export const IN_PROGRESS_TRACE_EXECUTION_STATUSES: readonly DebugStatus[] = [
    DEBUG_STATUS.PENDING,
    DEBUG_STATUS.RUNNING,
]

export const TERMINAL_TRACE_EXECUTION_STATUSES: readonly DebugStatus[] = [
    DEBUG_STATUS.COMPLETED,
    DEBUG_STATUS.ERROR,
    DEBUG_STATUS.TERMINATED,
]

export const isTraceExecutionInProgress = (
    status: DebugStatus | null | undefined
): boolean => (status ? IN_PROGRESS_TRACE_EXECUTION_STATUSES.includes(status) : false)

export const isTraceExecutionTerminal = (
    status: DebugStatus | null | undefined
): boolean => (status ? TERMINAL_TRACE_EXECUTION_STATUSES.includes(status) : false)

export const isTraceExecutionError = (
    status: DebugStatus | null | undefined
): boolean => status === DEBUG_STATUS.ERROR

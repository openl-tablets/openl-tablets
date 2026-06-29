import type { DebugStatus } from 'types/trace'

export const IN_PROGRESS_TRACE_EXECUTION_STATUSES: readonly DebugStatus[] = ['PENDING', 'RUNNING']

export const TERMINAL_TRACE_EXECUTION_STATUSES: readonly DebugStatus[] = ['COMPLETED', 'ERROR', 'TERMINATED']

export const isTraceExecutionInProgress = (
    status: DebugStatus | null | undefined
): boolean => (status ? IN_PROGRESS_TRACE_EXECUTION_STATUSES.includes(status) : false)

export const isTraceExecutionTerminal = (
    status: DebugStatus | null | undefined
): boolean => (status ? TERMINAL_TRACE_EXECUTION_STATUSES.includes(status) : false)

export const isTraceExecutionError = (
    status: DebugStatus | null | undefined
): boolean => status === 'ERROR'

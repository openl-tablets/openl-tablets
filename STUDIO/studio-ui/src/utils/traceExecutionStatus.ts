import type { DebugStatus } from 'types/trace'

export const IN_PROGRESS_TRACE_EXECUTION_STATUSES: readonly DebugStatus[] = ['pending', 'running']

export const TERMINAL_TRACE_EXECUTION_STATUSES: readonly DebugStatus[] = ['completed', 'error', 'terminated']

export const isTraceExecutionInProgress = (
    status: DebugStatus | null | undefined
): boolean => (status ? IN_PROGRESS_TRACE_EXECUTION_STATUSES.includes(status) : false)

export const isTraceExecutionTerminal = (
    status: DebugStatus | null | undefined
): boolean => (status ? TERMINAL_TRACE_EXECUTION_STATUSES.includes(status) : false)

export const isTraceExecutionError = (
    status: DebugStatus | null | undefined
): boolean => status === 'error'

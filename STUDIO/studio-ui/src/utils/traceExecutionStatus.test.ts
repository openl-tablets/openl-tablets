import {
    IN_PROGRESS_TRACE_EXECUTION_STATUSES,
    TERMINAL_TRACE_EXECUTION_STATUSES,
    isTraceExecutionInProgress,
    isTraceExecutionTerminal,
    isTraceExecutionError,
} from 'utils/traceExecutionStatus'

describe('traceExecutionStatus utils', () => {
    it('defines in-progress and terminal groups correctly', () => {
        expect(IN_PROGRESS_TRACE_EXECUTION_STATUSES).toEqual(['PENDING', 'RUNNING'])
        expect(TERMINAL_TRACE_EXECUTION_STATUSES).toEqual(['COMPLETED', 'ERROR', 'TERMINATED'])
    })

    it('detects in-progress statuses', () => {
        expect(isTraceExecutionInProgress('PENDING')).toBe(true)
        expect(isTraceExecutionInProgress('RUNNING')).toBe(true)
        expect(isTraceExecutionInProgress('SUSPENDED')).toBe(false)
        expect(isTraceExecutionInProgress(undefined)).toBe(false)
        expect(isTraceExecutionInProgress(null)).toBe(false)
    })

    it('detects terminal statuses', () => {
        expect(isTraceExecutionTerminal('COMPLETED')).toBe(true)
        expect(isTraceExecutionTerminal('ERROR')).toBe(true)
        expect(isTraceExecutionTerminal('TERMINATED')).toBe(true)
        expect(isTraceExecutionTerminal('SUSPENDED')).toBe(false)
        expect(isTraceExecutionTerminal(undefined)).toBe(false)
        expect(isTraceExecutionTerminal(null)).toBe(false)
    })

    it('detects error status precisely', () => {
        expect(isTraceExecutionError('ERROR')).toBe(true)
        expect(isTraceExecutionError('TERMINATED')).toBe(false)
        expect(isTraceExecutionError('RUNNING')).toBe(false)
        expect(isTraceExecutionError(undefined)).toBe(false)
    })
})

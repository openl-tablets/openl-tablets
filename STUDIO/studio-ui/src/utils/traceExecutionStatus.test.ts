import {
    IN_PROGRESS_TRACE_EXECUTION_STATUSES,
    TERMINAL_TRACE_EXECUTION_STATUSES,
    isTraceExecutionInProgress,
    isTraceExecutionTerminal,
    isTraceExecutionError,
} from 'utils/traceExecutionStatus'

describe('traceExecutionStatus utils', () => {
    it('defines in-progress and terminal groups correctly', () => {
        expect(IN_PROGRESS_TRACE_EXECUTION_STATUSES).toEqual(['pending', 'running'])
        expect(TERMINAL_TRACE_EXECUTION_STATUSES).toEqual(['completed', 'error', 'terminated'])
    })

    it('detects in-progress statuses', () => {
        expect(isTraceExecutionInProgress('pending')).toBe(true)
        expect(isTraceExecutionInProgress('running')).toBe(true)
        expect(isTraceExecutionInProgress('suspended')).toBe(false)
        expect(isTraceExecutionInProgress(undefined)).toBe(false)
        expect(isTraceExecutionInProgress(null)).toBe(false)
    })

    it('detects terminal statuses', () => {
        expect(isTraceExecutionTerminal('completed')).toBe(true)
        expect(isTraceExecutionTerminal('error')).toBe(true)
        expect(isTraceExecutionTerminal('terminated')).toBe(true)
        expect(isTraceExecutionTerminal('suspended')).toBe(false)
        expect(isTraceExecutionTerminal(undefined)).toBe(false)
        expect(isTraceExecutionTerminal(null)).toBe(false)
    })

    it('detects error status precisely', () => {
        expect(isTraceExecutionError('error')).toBe(true)
        expect(isTraceExecutionError('terminated')).toBe(false)
        expect(isTraceExecutionError('running')).toBe(false)
        expect(isTraceExecutionError(undefined)).toBe(false)
    })
})

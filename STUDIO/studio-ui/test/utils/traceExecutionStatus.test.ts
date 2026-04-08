import {
    TRACE_EXECUTION_STATUS,
    IN_PROGRESS_TRACE_EXECUTION_STATUSES,
    TERMINAL_TRACE_EXECUTION_STATUSES,
    isTraceExecutionInProgress,
    isTraceExecutionTerminal,
    isTraceExecutionError,
} from 'utils/traceExecutionStatus'

describe('traceExecutionStatus utils', () => {
    it('exposes stable status constants', () => {
        expect(TRACE_EXECUTION_STATUS.PENDING).toBe('PENDING')
        expect(TRACE_EXECUTION_STATUS.STARTED).toBe('STARTED')
        expect(TRACE_EXECUTION_STATUS.COMPLETED).toBe('COMPLETED')
        expect(TRACE_EXECUTION_STATUS.INTERRUPTED).toBe('INTERRUPTED')
        expect(TRACE_EXECUTION_STATUS.ERROR).toBe('ERROR')
    })

    it('defines in-progress and terminal groups correctly', () => {
        expect(IN_PROGRESS_TRACE_EXECUTION_STATUSES).toEqual(['PENDING', 'STARTED'])
        expect(TERMINAL_TRACE_EXECUTION_STATUSES).toEqual(['ERROR', 'INTERRUPTED'])
    })

    it('detects in-progress statuses', () => {
        expect(isTraceExecutionInProgress('PENDING')).toBe(true)
        expect(isTraceExecutionInProgress('STARTED')).toBe(true)
        expect(isTraceExecutionInProgress('COMPLETED')).toBe(false)
        expect(isTraceExecutionInProgress(undefined)).toBe(false)
        expect(isTraceExecutionInProgress(null)).toBe(false)
    })

    it('detects terminal statuses', () => {
        expect(isTraceExecutionTerminal('ERROR')).toBe(true)
        expect(isTraceExecutionTerminal('INTERRUPTED')).toBe(true)
        expect(isTraceExecutionTerminal('COMPLETED')).toBe(false)
        expect(isTraceExecutionTerminal(undefined)).toBe(false)
        expect(isTraceExecutionTerminal(null)).toBe(false)
    })

    it('detects error status precisely', () => {
        expect(isTraceExecutionError('ERROR')).toBe(true)
        expect(isTraceExecutionError('INTERRUPTED')).toBe(false)
        expect(isTraceExecutionError('STARTED')).toBe(false)
        expect(isTraceExecutionError(undefined)).toBe(false)
    })
})

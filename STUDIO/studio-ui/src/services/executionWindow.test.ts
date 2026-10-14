import {
    currentExecution,
    EXECUTION_WINDOW_KEY,
    isMissingExecution,
    openExecutionWindow,
    publishExecution,
} from './executionWindow'

vi.mock('./config', () => ({ default: { CONTEXT: '/webstudio' } }))

describe('execution result window', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.restoreAllMocks()
    })

    afterEach(() => vi.unstubAllGlobals())

    it('reuses its named window and restores the latest result after refresh', () => {
        const open = vi.spyOn(window, 'open').mockReturnValue({} as Window)
        const first = { kind: 'tests' as const, projectId: 'p1', tableId: 't1', options: { failuresOnly: true } }
        const second = { kind: 'run' as const, projectId: 'p1', tableId: 't2', fileOptions: {} }

        expect(openExecutionWindow(first)).toBe(true)
        expect(openExecutionWindow(second)).toBe(true)
        expect(open).toHaveBeenCalledTimes(2)
        expect(open).toHaveBeenLastCalledWith(
            '/webstudio/execution-results', 'execution_results', 'width=1240,height=800,resizable=yes,scrollbars=yes'
        )
        expect(currentExecution()).toEqual(second)
    })

    it('signals a second identical run and handles an absent or invalid saved request', () => {
        vi.stubGlobal('crypto', {}) // HTTP does not expose randomUUID.
        const request = { kind: 'benchmark' as const, projectId: 'p1', tableId: 't1' }
        expect(currentExecution()).toBeNull()
        publishExecution(request)
        const first = localStorage.getItem(EXECUTION_WINDOW_KEY)
        publishExecution(request)
        expect(localStorage.getItem(EXECUTION_WINDOW_KEY)).not.toBe(first)
        expect(currentExecution()).toEqual(request)
        localStorage.setItem(EXECUTION_WINDOW_KEY, '{')
        expect(currentExecution()).toBeNull()
        localStorage.setItem(EXECUTION_WINDOW_KEY, JSON.stringify({ request: { kind: 'run', projectId: 'p1' } }))
        expect(currentExecution()).toBeNull()
    })

    it('recognizes results that the server has released', () => {
        expect(isMissingExecution({ status: 404 })).toBe(true)
        expect(isMissingExecution({ status: 500 })).toBe(false)
        expect(isMissingExecution(new Error('offline'))).toBe(false)
    })

    it('keeps the editor result usable when browser storage refuses writes', () => {
        const request = { kind: 'run' as const, projectId: 'p1', tableId: 't1', fileOptions: {} }
        vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Storage disabled') })

        expect(publishExecution(request)).toBe(false)
        expect(currentExecution()).toBeNull()
        expect(openExecutionWindow(request)).toBe(false)
    })
})

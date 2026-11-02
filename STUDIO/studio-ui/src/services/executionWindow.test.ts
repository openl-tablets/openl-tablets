import {
    currentExecution,
    EXECUTION_WINDOW_KEY,
    isMissingExecution,
    isOutdatedExecution,
    launchInWindow,
    openExecutionWindow,
    publishExecution,
} from './executionWindow'

vi.mock('./config', () => ({ default: { CONTEXT: '/webstudio' } }))

const REFUSALS = { blocked: 'Allow pop-ups', unsaved: 'Allow storage' }

/** A browser window as far as the results window is opened in it. */
const browserWindow = (pathname = '') => ({ location: { pathname, href: '' }, focus: vi.fn() })

describe('execution result window', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.restoreAllMocks()
    })

    afterEach(() => vi.unstubAllGlobals())

    it('reuses its named window and restores the latest result after refresh', () => {
        const opened = browserWindow()
        const open = vi.spyOn(window, 'open').mockReturnValue(opened as unknown as Window)
        const first = { kind: 'tests' as const, projectId: 'p1', tableId: 't1', options: { failuresOnly: true } }
        const second = { kind: 'run' as const, projectId: 'p1', tableId: 't2', fileOptions: {} }

        expect(openExecutionWindow(first)).toBe(true)
        expect(opened.location.href).toBe('/webstudio/execution-results')
        expect(openExecutionWindow(second)).toBe(true)
        expect(open).toHaveBeenCalledTimes(2)
        expect(open).toHaveBeenLastCalledWith(
            '', 'execution_results', 'width=1240,height=800,resizable=yes,scrollbars=yes'
        )
        expect(opened.focus).toHaveBeenCalledTimes(2)
        expect(currentExecution()).toEqual(second)
    })

    it('focuses a window that shows the results already, without loading the page again', () => {
        const opened = browserWindow('/webstudio/execution-results')
        vi.spyOn(window, 'open').mockReturnValue(opened as unknown as Window)

        expect(openExecutionWindow({ kind: 'starting', of: 'tests' })).toBe(true)

        expect(opened.location.href).toBe('')
        expect(opened.focus).toHaveBeenCalledOnce()
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

    it('opens the window before the execution starts, then shows what started', async () => {
        const open = vi.spyOn(window, 'open').mockReturnValue(browserWindow() as unknown as Window)
        const started = { kind: 'benchmark' as const, projectId: 'p1', tableId: 't1' }
        const start = vi.fn(() => {
            // The window already says the benchmark is starting when the start is asked for.
            expect(open).toHaveBeenCalledOnce()
            expect(currentExecution()).toEqual({ kind: 'starting', of: 'benchmark' })
            return Promise.resolve(started)
        })

        await launchInWindow('benchmark', start, REFUSALS)

        expect(start).toHaveBeenCalledOnce()
        expect(currentExecution()).toEqual(started)
    })

    it('starts nothing when the browser refuses the window, and leaves the results shown before', async () => {
        const before = { kind: 'benchmark' as const, projectId: 'p1', tableId: 't1' }
        publishExecution(before)
        vi.spyOn(window, 'open').mockReturnValue(null)
        const start = vi.fn()

        await expect(launchInWindow('tests', start, REFUSALS)).rejects.toThrow('Allow pop-ups')
        expect(start).not.toHaveBeenCalled()
        expect(currentExecution()).toEqual(before)
    })

    it('starts nothing, and says so, when the browser will not store the launch for the window', async () => {
        vi.spyOn(window, 'open').mockReturnValue(browserWindow() as unknown as Window)
        vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Quota exceeded') })
        const start = vi.fn()

        await expect(launchInWindow('benchmark', start, REFUSALS)).rejects.toThrow('Allow storage')
        expect(start).not.toHaveBeenCalled()
    })

    it('shows in the window why the execution could not start', async () => {
        vi.spyOn(window, 'open').mockReturnValue(browserWindow() as unknown as Window)

        await expect(launchInWindow('run', () => Promise.reject(new Error('Compiling')), REFUSALS))
            .rejects.toThrow('Compiling')
        expect(currentExecution()).toEqual({ kind: 'failed', of: 'run', message: 'Compiling' })
    })

    it('restores a starting launch and refuses one of an unknown kind', () => {
        publishExecution({ kind: 'starting', of: 'tests' })
        expect(currentExecution()).toEqual({ kind: 'starting', of: 'tests' })
        localStorage.setItem(EXECUTION_WINDOW_KEY, JSON.stringify({ id: 'x', request: { kind: 'starting', of: 'trace' } }))
        expect(currentExecution()).toBeNull()
        localStorage.setItem(EXECUTION_WINDOW_KEY, JSON.stringify({ id: 'x', request: { kind: 'failed', of: 'run' } }))
        expect(currentExecution()).toBeNull()
    })

    it('recognizes results that the server has released', () => {
        expect(isMissingExecution({ status: 404 })).toBe(true)
        expect(isMissingExecution({ status: 500 })).toBe(false)
        expect(isMissingExecution(new Error('offline'))).toBe(false)
    })

    it('recognizes results that the server dropped because the project changed', () => {
        expect(isOutdatedExecution({ status: 410 })).toBe(true)
        expect(isOutdatedExecution({ status: 404 })).toBe(false)
        expect(isOutdatedExecution(null)).toBe(false)
    })

    it('keeps the editor result usable when browser storage refuses writes', () => {
        const request = { kind: 'run' as const, projectId: 'p1', tableId: 't1', fileOptions: {} }
        vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Storage disabled') })
        vi.spyOn(window, 'open').mockReturnValue(browserWindow() as unknown as Window)

        expect(publishExecution(request)).toBe(false)
        expect(currentExecution()).toBeNull()
        expect(openExecutionWindow(request)).toBe(false)
    })
})

import { launchTrace, TRACE_WINDOW_BLOCKED } from 'services/traceLaunch'
import { traceService } from 'services/traceService'
import { retireTraceLaunch, stampTraceLaunch } from 'services/traceLaunchToken'

vi.mock('services/traceService', () => ({
    traceService: {
        startTrace: vi.fn().mockResolvedValue({}),
        exportTrace: vi.fn().mockResolvedValue('TRACE: SpreadSheet Double Rate() = 0.9\n'),
        cancelTrace: vi.fn().mockResolvedValue(undefined),
    },
}))

vi.mock('services/traceLaunchToken', () => ({
    stampTraceLaunch: vi.fn(() => '7'),
    retireTraceLaunch: vi.fn(),
}))

vi.mock('services/config', () => ({ default: { CONTEXT: '/webstudio' } }))

const startTrace = traceService.startTrace as ReturnType<typeof vi.fn>
const exportTrace = traceService.exportTrace as ReturnType<typeof vi.fn>
const stamp = stampTraceLaunch as ReturnType<typeof vi.fn>
const retire = retireTraceLaunch as ReturnType<typeof vi.fn>

const request = { projectId: 'p1', tableId: 't1', showRealNumbers: true }

describe('launchTrace', () => {
    let openSpy: ReturnType<typeof vi.spyOn>
    let clickSpy: ReturnType<typeof vi.spyOn>

    beforeEach(() => {
        vi.clearAllMocks()
        // The browser opens the window; a test that wants it refused says so for itself.
        openSpy = vi.spyOn(window, 'open').mockReturnValue({} as Window)
        clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
        // jsdom does not implement object URLs. The download helper only needs them to not throw.
        URL.createObjectURL = vi.fn(() => 'blob:trace')
        URL.revokeObjectURL = vi.fn()
    })

    afterEach(() => {
        openSpy.mockRestore()
        clickSpy.mockRestore()
    })

    it('starts the session with the input and opens the trace window on it', async () => {
        await launchTrace({ ...request, inputJson: '{}', fromModule: 'Main' })

        expect(startTrace).toHaveBeenCalledWith('p1', {
            tableId: 't1', inputJson: '{}', fromModule: 'Main', stopAtEntry: true,
        })
        expect(openSpy).toHaveBeenCalledTimes(1)
        const url = String(openSpy.mock.calls[0][0])
        expect(url).toContain('/webstudio/trace/p1?')
        expect(url).toContain('tableId=t1')
        expect(url).toContain('fromModule=Main')
        expect(url).not.toContain('advanced')
        expect(exportTrace).not.toHaveBeenCalled()
        // The launch is stamped before the session exists. A window closing during the request then cannot
        // delete the session being created. A successful launch keeps its token.
        expect(stamp.mock.invocationCallOrder[0]).toBeLessThan(Number(startTrace.mock.invocationCallOrder[0]))
        expect(retire).not.toHaveBeenCalled()
    })

    it('carries the test cases and the advanced mode into the trace window URL', async () => {
        await launchTrace({ ...request, testRanges: '3', advanced: true })

        expect(startTrace).toHaveBeenCalledWith('p1', { tableId: 't1', testRanges: '3', stopAtEntry: true })
        const url = String(openSpy.mock.calls[0][0])
        expect(url).toContain('testRanges=3')
        expect(url).toContain('advanced=true')
    })

    it('hands the launch token back and rejects when the session fails to start', async () => {
        startTrace.mockRejectedValueOnce(new Error('compilation in progress'))

        await expect(launchTrace({ ...request, inputJson: '{}' })).rejects.toThrow('compilation in progress')

        expect(retire).toHaveBeenCalledWith('7')
        expect(openSpy).not.toHaveBeenCalled()
    })

    it('downloads the trace as a file instead of opening the window', async () => {
        await launchTrace({ ...request, inputJson: '{}', download: true })

        expect(startTrace).toHaveBeenCalledTimes(1) // a session is still started first
        expect(exportTrace).toHaveBeenCalledWith('p1', true) // then the full trace is fetched
        expect(clickSpy).toHaveBeenCalledTimes(1) // and saved via a download link
        expect(openSpy).not.toHaveBeenCalled()
    })
    it('lets the session go when the browser will not open the trace window', async () => {
        // Creating the session waits for a project that may still be compiling, so the click that started
        // the launch may no longer count as one and the window is refused. Nothing is left registered.
        openSpy.mockReturnValue(null)

        await expect(launchTrace(request)).rejects.toThrow(TRACE_WINDOW_BLOCKED)
        // Nothing will ever attach to the session, so it does not stay holding a parked execution.
        expect(traceService.cancelTrace).toHaveBeenCalledWith(request.projectId)
        expect(retireTraceLaunch).toHaveBeenCalledWith('7')
    })
})

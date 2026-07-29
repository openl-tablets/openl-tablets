import React from 'react'
import { act, render } from '@testing-library/react'
import { traceService } from 'services/traceService'
import { retireTraceLaunch, stampTraceLaunch } from 'services/traceLaunchToken'
import TraceExecutionModal from 'containers/TraceExecutionModal/TraceExecutionModal'

vi.mock('services/traceService', () => ({
    traceService: {
        startTrace: vi.fn().mockResolvedValue({}),
        exportTrace: vi.fn().mockResolvedValue('TRACE: SpreadSheet Double Rate() = 0.9\n'),
    },
}))

vi.mock('services/traceLaunchToken', () => ({
    stampTraceLaunch: vi.fn(() => '7'),
    retireTraceLaunch: vi.fn(),
}))

vi.mock('services/config', () => ({ default: { CONTEXT: '/webstudio' } }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const startTrace = traceService.startTrace as ReturnType<typeof vi.fn>
const stamp = stampTraceLaunch as ReturnType<typeof vi.fn>
const retire = retireTraceLaunch as ReturnType<typeof vi.fn>
const exportTrace = traceService.exportTrace as ReturnType<typeof vi.fn>

const fire = (detail: Record<string, unknown>) =>
    window.dispatchEvent(new CustomEvent('openTraceExecutionModal', { detail }))

describe('TraceExecutionModal', () => {
    let openSpy: ReturnType<typeof vi.spyOn>
    let clickSpy: ReturnType<typeof vi.spyOn>

    beforeEach(() => {
        vi.clearAllMocks()
        openSpy = vi.spyOn(window, 'open').mockReturnValue(null)
        clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
        // jsdom does not implement object URLs; the download helper only needs them to not throw.
        URL.createObjectURL = vi.fn(() => 'blob:trace')
        URL.revokeObjectURL = vi.fn()
    })

    afterEach(() => {
        openSpy.mockRestore()
        clickSpy.mockRestore()
    })

    it('opens the debugger window for a normal trace', async () => {
        render(<TraceExecutionModal />)

        await act(async () => {
            fire({ projectId: 'p1', tableId: 't1', moduleName: 'm', showRealNumbers: true, inputJson: '{}' })
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        expect(startTrace).toHaveBeenCalledWith('p1', expect.objectContaining({ tableId: 't1', stopAtEntry: true }))
        expect(openSpy).toHaveBeenCalledTimes(1) // debugger window opened
        expect(exportTrace).not.toHaveBeenCalled()
        // The launch is stamped before the session exists, so a window closing during the request
        // cannot delete the session being created; a successful launch keeps its token.
        expect(stamp.mock.invocationCallOrder[0]).toBeLessThan(Number(startTrace.mock.invocationCallOrder[0]))
        expect(retire).not.toHaveBeenCalled()
    })

    it('carries the launch-time advanced flag into the trace window URL', async () => {
        render(<TraceExecutionModal />)

        await act(async () => {
            fire({ projectId: 'p1', tableId: 't1', moduleName: 'm', showRealNumbers: true, inputJson: '{}',
                advanced: true })
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        // The Advanced tracer checkbox on the JSF page decides the mode; the window opens straight into it.
        expect(String(openSpy.mock.calls[0][0])).toContain('advanced=true')
    })

    it('opens the business view — no advanced flag — when the checkbox is off', async () => {
        render(<TraceExecutionModal />)

        await act(async () => {
            fire({ projectId: 'p1', tableId: 't1', moduleName: 'm', showRealNumbers: true, inputJson: '{}' })
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        expect(String(openSpy.mock.calls[0][0])).not.toContain('advanced')
    })

    it('hands the launch token back when the session fails to start', async () => {
        startTrace.mockRejectedValueOnce(new Error('compilation in progress'))
        render(<TraceExecutionModal />)

        await act(async () => {
            fire({ projectId: 'p1', tableId: 't1', moduleName: 'm', showRealNumbers: true, inputJson: '{}' })
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        expect(retire).toHaveBeenCalledWith('7') // the token stamped for this failed launch
        expect(openSpy).not.toHaveBeenCalled()
    })

    it('exports and downloads the trace file in download mode, without opening the debugger', async () => {
        render(<TraceExecutionModal />)

        await act(async () => {
            fire({ projectId: 'p1', tableId: 't1', moduleName: 'm', showRealNumbers: true, inputJson: '{}', downloadMode: true })
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        expect(startTrace).toHaveBeenCalledTimes(1) // a session is still started first
        expect(exportTrace).toHaveBeenCalledWith('p1', true) // then the full trace is fetched
        expect(clickSpy).toHaveBeenCalledTimes(1) // and saved via a download link
        expect(openSpy).not.toHaveBeenCalled() // no debugger window in download mode
    })
})

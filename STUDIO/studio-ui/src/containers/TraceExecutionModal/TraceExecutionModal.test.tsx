import React from 'react'
import { act, render } from '@testing-library/react'
import { traceService } from 'services/traceService'
import TraceExecutionModal from 'containers/TraceExecutionModal/TraceExecutionModal'

vi.mock('services/traceService', () => ({
    traceService: {
        startTrace: vi.fn().mockResolvedValue({}),
        exportTrace: vi.fn().mockResolvedValue('TRACE: SpreadSheet Double Rate() = 0.9\n'),
    },
}))

vi.mock('services/config', () => ({ default: { CONTEXT: '/webstudio' } }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const startTrace = traceService.startTrace as ReturnType<typeof vi.fn>
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

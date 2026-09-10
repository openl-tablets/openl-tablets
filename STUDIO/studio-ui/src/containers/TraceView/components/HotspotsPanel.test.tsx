import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { ProfileSummaryView } from 'types/trace'
import HotspotsPanel from 'containers/TraceView/components/HotspotsPanel'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        setBreakpoints: vi.fn().mockResolvedValue(undefined),
        resume: vi.fn().mockResolvedValue(undefined),
        cancelTrace: vi.fn().mockResolvedValue(undefined),
        getStack: vi.fn().mockRejectedValue(new Error('no session')),
        startTrace: vi.fn().mockResolvedValue({ status: 'suspended', frames: []}),
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const setBreakpoints = traceService.setBreakpoints as ReturnType<typeof vi.fn>

const profile = (): ProfileSummaryView => ({
    distinctTables: 3,
    nodeCount: 6720,
    totalMillis: 273.8,
    truncated: false,
    hotspots: [
        { uri: 'uHot', name: 'ClaimCostPerBenefitPerAgeBand', kind: 'spreadsheet', selfMillis: 68.2, totalMillis: 112.4, count: 6400 },
        { uri: 'uMid', name: 'AgeBandRate', kind: 'spreadsheet', selfMillis: 12.1, totalMillis: 40.0, count: 160 },
    ],
})

describe('HotspotsPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1', tableId: 't1' })
    })

    it('shows an empty state until a profiling run has produced hot spots', () => {
        render(<HotspotsPanel />)
        expect(screen.getByText('hotspots.empty')).toBeInTheDocument()
        expect(screen.queryByTestId('hotspot-row')).toBeNull()
    })

    it('ranks the hottest tables with their run count and own/total time', () => {
        act(() => { useTraceStore.setState({ profile: profile() }) })
        render(<HotspotsPanel />)

        expect(screen.getAllByTestId('hotspot-row')).toHaveLength(2)
        expect(screen.getByText('ClaimCostPerBenefitPerAgeBand')).toBeInTheDocument()
        expect(screen.getByText('×6400')).toBeInTheDocument()
        expect(screen.getByText('68 ms')).toBeInTheDocument() // self time (rounded)
        expect(screen.getByText('112 ms')).toBeInTheDocument() // inclusive time
    })

    it('notes that only the slowest tables are shown while every call is still counted', () => {
        // Three tables ran but only two are listed, so the display-limit note appears.
        act(() => { useTraceStore.setState({ profile: profile() }) })
        render(<HotspotsPanel />)
        expect(screen.getByText('hotspots.more')).toBeInTheDocument()
    })

    it('omits the note when every table that ran is shown', () => {
        act(() => { useTraceStore.setState({ profile: { ...profile(), distinctTables: 2 } }) })
        render(<HotspotsPanel />)
        expect(screen.queryByText('hotspots.more')).toBeNull()
    })

    it('replays to the hot table on click, restarting and running to its uri', async () => {
        useTraceStore.setState({ profile: profile(), status: 'completed' })
        render(<HotspotsPanel />)

        await userEvent.click(screen.getAllByTestId('hotspot-replay')[0]!)
        // Replay adds a one-shot breakpoint on the table, then restarts.
        await waitFor(() => expect(setBreakpoints).toHaveBeenCalledWith('p1', ['uHot']))
    })
})

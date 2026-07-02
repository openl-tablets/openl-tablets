import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { WatchView } from 'types/trace'
import WatchPanel from 'containers/TraceView/components/WatchPanel'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        setWatches: vi.fn().mockResolvedValue(undefined),
        getWatch: vi.fn(),
        startTrace: vi.fn().mockResolvedValue({ status: 'suspended', frames: [{ index: 0, uri: 'uCov', name: 'CoveragePremium', active: true, depth: 1, kind: 'spreadsheet', completed: false, error: false, steps: []}]}),
        cancelTrace: vi.fn().mockResolvedValue(undefined),
        getStack: vi.fn().mockRejectedValue(new Error('no session')),
        setBreakpoints: vi.fn().mockResolvedValue(undefined),
        resume: vi.fn().mockResolvedValue(undefined),
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const setWatches = traceService.setWatches as ReturnType<typeof vi.fn>
const getWatch = traceService.getWatch as ReturnType<typeof vi.fn>
const cancelTrace = traceService.cancelTrace as ReturnType<typeof vi.fn>
const setBreakpoints = traceService.setBreakpoints as ReturnType<typeof vi.fn>

const scalar = (value: number) => ({ name: '$VehiclePriceFactor', description: 'Double', lazy: false, value })

const series: WatchView = {
    truncated: false,
    series: [{
        name: '$VehiclePriceFactor',
        table: 'CoveragePremium',
        tableUri: 'uCov',
        points: [
            { instance: 0, label: 'CoveragePremium #1', path: ['Root', 'CoveragePremium'], ref: 'uCov#R2C0', value: scalar(1.0) },
            { instance: 1, label: 'CoveragePremium #2', path: ['Root', 'CoveragePremium'], ref: 'uCov#R2C0', value: scalar(83.372) },
        ],
    }],
}

describe('WatchPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1', tableId: 't1' })
    })

    it('adds a watch cell and shows a factor series across executions', async () => {
        render(<WatchPanel />)

        await userEvent.type(screen.getByTestId('watch-add'), '$VehiclePriceFactor')
        await userEvent.click(screen.getByTestId('watch-add-button'))
        await waitFor(() => expect(setWatches).toHaveBeenCalledWith('p1', ['$VehiclePriceFactor']))
        expect(useTraceStore.getState().watches).toEqual(['$VehiclePriceFactor'])

        // Once a series is in the store, the panel shows the value on every execution — the outlier stands out.
        act(() => { useTraceStore.setState({ watch: series }) })
        expect(await screen.findByText('83.372')).toBeInTheDocument()
        expect(screen.getAllByTestId('watch-point')).toHaveLength(2)
        expect(screen.getByTestId('watch-series')).toHaveTextContent('$VehiclePriceFactor')
    })

    it('jumps to the watched cell (its ref), not its table, when replaying a point', async () => {
        const onePoint: WatchView = {
            truncated: false,
            series: [{
                name: '$VehiclePriceFactor', table: 'CoveragePremium', tableUri: 'uCov',
                points: [{ instance: 0, label: 'CoveragePremium #1', path: ['Root'], ref: 'uCov#R2C0', value: scalar(1.0) }],
            }],
        }
        useTraceStore.setState({ watch: onePoint, watches: ['$VehiclePriceFactor']})
        render(<WatchPanel />)

        await userEvent.click(screen.getByTestId('watch-replay'))

        // Replay restarts and runs to the cell on THIS exact instance (uri#cellRef@N) — so watching an
        // outlier on pass N jumps straight to pass N, not the first pass, and not the whole table.
        await waitFor(() => expect(cancelTrace).toHaveBeenCalledWith('p1'))
        await waitFor(() =>
            expect(setBreakpoints).toHaveBeenCalledWith('p1', expect.arrayContaining(['uCov#R2C0@0'])))
    })

    it('removing a watch also clears its already-collected series', async () => {
        useTraceStore.setState({ watches: ['$VehiclePriceFactor'], watch: series })

        await useTraceStore.getState().setWatchCells([])

        expect(setWatches).toHaveBeenCalledWith('p1', [])
        expect(useTraceStore.getState().watches).toEqual([])
        expect(useTraceStore.getState().watch?.series).toEqual([])
    })

    it('collects the series by running the trace to completion', async () => {
        getWatch.mockResolvedValue(series)
        useTraceStore.setState({ watches: ['$VehiclePriceFactor']})
        render(<WatchPanel />)

        await userEvent.click(screen.getByTestId('watch-collect'))

        // Collect runs to completion (stopAtEntry=false, no full tree), then fetches and renders the series.
        await waitFor(() => expect(traceService.startTrace).toHaveBeenCalledWith('p1',
            expect.objectContaining({ stopAtEntry: false, includeTree: false })))
        expect(await screen.findByText('83.372')).toBeInTheDocument()
        expect(getWatch).toHaveBeenCalledWith('p1')
    })
})

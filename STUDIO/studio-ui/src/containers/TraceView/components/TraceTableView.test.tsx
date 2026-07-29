import React from 'react'
import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import traceService from 'services/traceService'
import { NotFoundError } from 'services'
import { useTraceStore } from 'store/traceStore'
import type { DebugFrameView, RawTableView } from 'types/trace'
import TraceTableView from 'containers/TraceView/components/TraceTableView'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: { getFrameHighlights: vi.fn() },
}))

vi.mock('react-i18next', () => {
    const t = (key: string, opts?: Record<string, unknown>) => (opts ? `${key} ${JSON.stringify(opts)}` : key)
    return { useTranslation: () => ({ t }) }
})

const getFrameHighlights = traceService.getFrameHighlights as ReturnType<typeof vi.fn>

const frame = (tableId: string): DebugFrameView => ({
    index: 0,
    depth: 1,
    instance: 0,
    uri: 'u',
    tableId,
    name: 'T',
    kind: 'spreadsheet',
    active: true,
    completed: false,
    error: false,
})

const cacheTable = (tableId: string, table: RawTableView): void => {
    useTraceStore.setState({
        projectId: 'p1',
        stackVersion: 1,
        selectedFrameIndex: 0,
        frames: [frame(tableId)],
        rawTableCache: { [tableId]: table },
    })
}

// Captured before any test overrides it; reset() restores state but not overridden actions.
const pristineLoadRawTable = useTraceStore.getState().loadRawTable

describe('TraceTableView', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        // Put the real loader back before rendering, so a prior test's override never leaks in.
        useTraceStore.setState({ loadRawTable: pristineLoadRawTable })
    })

    // Point the frame at a table that is not cached and control how its structure loads.
    const uncachedFrame = (loadRawTable: () => Promise<RawTableView>): void => {
        useTraceStore.setState({
            projectId: 'p1',
            stackVersion: 1,
            selectedFrameIndex: 0,
            frames: [frame('tbl')],
            rawTableCache: {},
            loadRawTable,
        })
    }

    it('renders the raw grid with merges, Excel styles, and a highlight that overrides the cell background', async () => {
        getFrameHighlights.mockResolvedValue([{ cell: 'B2', state: 'current' }])
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            source: [
                [{ cell: 'A1', value: 'Header', colspan: 2, style: { bold: true, background: '#dbe5f1' } }, { covered: true }],
                [{ cell: 'A2', value: 'Step' }, { cell: 'B2', value: '= x', style: { background: '#ffffcc' } }],
            ],
        })

        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        const table = screen.getByTestId('trace-table')
        expect(screen.getByText('Header')).toBeInTheDocument()
        expect(screen.getByText('= x')).toBeInTheDocument()

        // The covered cell is skipped, so the merged first row renders a single <td> spanning two columns.
        const firstRow = table.querySelectorAll('tbody tr')[0] as HTMLElement
        expect(firstRow.querySelectorAll('td')).toHaveLength(1)
        const header = table.querySelector('[data-cell="A1"]') as HTMLElement
        expect(header.getAttribute('colspan')).toBe('2')
        expect(header.style.fontWeight).toBe('bold')
        expect(header.style.background).not.toBe('') // a non-highlighted styled cell keeps its Excel background

        // The highlighted current cell drops its Excel background so the highlight class wins.
        const current = table.querySelector('[data-cell="B2"]') as HTMLElement
        expect(current.style.background).toBe('')
        const plain = table.querySelector('[data-cell="A2"]') as HTMLElement
        expect(current.className).not.toBe(plain.className) // current carries the extra highlight class
        expect(getFrameHighlights).toHaveBeenCalledWith('p1', 0)
    })

    it('mutes non-highlighted cells while a meaningful highlight (a decision result) keeps its colour', async () => {
        getFrameHighlights.mockResolvedValue([{ cell: 'B1', state: 'result' }])
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            source: [[{ cell: 'A1', value: 'Step' }, { cell: 'B1', value: '= x' }, { cell: 'C1', value: 'note' }]],
        })

        render(<TraceTableView dimOthers frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        const table = screen.getByTestId('trace-table')
        const plainA = table.querySelector('[data-cell="A1"]') as HTMLElement
        const plainC = table.querySelector('[data-cell="C1"]') as HTMLElement
        const highlighted = table.querySelector('[data-cell="B1"]') as HTMLElement
        expect(plainA.className).toBe(plainC.className) // both muted the same way
        expect(plainA.className).not.toBe(highlighted.className) // the result highlight keeps its colour
    })

    it('drops the yellow current highlight when dimmed — the current cell keeps its original colour', async () => {
        getFrameHighlights.mockResolvedValue([{ cell: 'B1', state: 'current' }])
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            source: [[{ cell: 'A1', value: 'Step' }, { cell: 'B1', value: '= x' }]],
        })

        // Advanced view: the current cell carries the yellow highlight class.
        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())
        const yellow = (screen.getByTestId('trace-table').querySelector('[data-cell="B1"]') as HTMLElement).className
        cleanup()

        // Business view: the current cell is left in its original colour (no yellow) while the rest are muted.
        render(<TraceTableView dimOthers frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())
        const table = screen.getByTestId('trace-table')
        const original = (table.querySelector('[data-cell="B1"]') as HTMLElement).className
        const muted = (table.querySelector('[data-cell="A1"]') as HTMLElement).className
        expect(original).not.toBe(yellow) // the yellow highlight is gone
        expect(original).not.toBe(muted) // and it is not muted like the rest — it keeps its colour
    })

    it('keeps the table fully readable without dimOthers even when a cell is highlighted', async () => {
        getFrameHighlights.mockResolvedValue([{ cell: 'B1', state: 'current' }])
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            source: [[{ cell: 'A1', value: 'Step' }, { cell: 'B1', value: '= x' }]],
        })
        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())
        const plain = screen.getByTestId('trace-table').querySelector('[data-cell="A1"]') as HTMLElement

        cleanup()
        render(<TraceTableView dimOthers frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())
        const muted = screen.getByTestId('trace-table').querySelector('[data-cell="A1"]') as HTMLElement

        expect(muted.className).not.toBe(plain.className) // dimOthers adds the muting class
    })

    it('paints the given cell directly and skips the highlights fetch when highlightCell is set', async () => {
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            source: [[{ cell: 'A1', value: 'Step' }, { cell: 'B1', value: '= x' }]],
        })

        render(<TraceTableView frameIndex={0} highlightCell="B1" />)

        const table = screen.getByTestId('trace-table')
        const pointed = table.querySelector('[data-cell="B1"]') as HTMLElement
        const plain = table.querySelector('[data-cell="A1"]') as HTMLElement
        expect(pointed.className).not.toBe(plain.className) // the pointed cell carries the highlight class
        expect(getFrameHighlights).not.toHaveBeenCalled() // the caller already knows the cell
    })

    it('shows a truncation notice when the backend sliced the table', async () => {
        getFrameHighlights.mockResolvedValue([])
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            totalRows: 900,
            source: [[{ cell: 'A1', value: 'only' }]],
        })

        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        screen.getByTestId('trace-table')
        // The notice reports the rendered count and the full total.
        expect(screen.getByText(/table\.truncated.*900/)).toBeInTheDocument()
    })

    it('shows a spinner while the table structure loads', async () => {
        getFrameHighlights.mockResolvedValue([])
        uncachedFrame(() => new Promise<RawTableView>(() => undefined)) // never resolves: stays loading

        await act(async () => {
            render(<TraceTableView frameIndex={0} />)
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        expect(screen.getByText('loadingTable')).toBeInTheDocument()
    })

    it('shows an error when the table structure fails to load', async () => {
        getFrameHighlights.mockResolvedValue([])
        uncachedFrame(() => Promise.reject(new Error('boom')))

        await act(async () => {
            render(<TraceTableView frameIndex={0} />)
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        expect(screen.getByText('boom')).toBeInTheDocument()
        expect(screen.queryByTestId('trace-table')).toBeNull()
    })

    it('renders nothing when the frame has no table view (404)', async () => {
        getFrameHighlights.mockResolvedValue([])
        uncachedFrame(() => Promise.reject(new NotFoundError('no table')))

        await act(async () => {
            render(<TraceTableView frameIndex={0} />)
            await new Promise(resolve => setTimeout(resolve, 50))
        })

        // A 404 is not an error: no table, no message, nothing rendered.
        expect(screen.queryByTestId('trace-table')).toBeNull()
        expect(screen.queryByText('no table')).toBeNull()
    })

    it('still renders the table when the highlights fetch fails', async () => {
        getFrameHighlights.mockRejectedValue(new Error('no highlights'))
        cacheTable('tbl', { id: 'tbl', name: 'T', source: [[{ cell: 'A1', value: 'v' }]]})

        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        screen.getByTestId('trace-table') // the grid renders; the highlight overlay is just empty
    })

    it('skips the highlights fetch when there is no open project', async () => {
        cacheTable('tbl', { id: 'tbl', name: 'T', source: [[{ cell: 'A1', value: 'v' }]]})
        useTraceStore.setState({ projectId: null })

        render(<TraceTableView frameIndex={0} />)

        screen.getByTestId('trace-table')
        expect(getFrameHighlights).not.toHaveBeenCalled()
    })

    it('keys the legend to only the states the table actually paints, not all four every time', async () => {
        getFrameHighlights.mockResolvedValue([
            { cell: 'A1', state: 'conditionTrue' },
            { cell: 'A2', state: 'conditionFalse' },
            { cell: 'B1', state: 'result' },
        ])
        cacheTable('tbl', {
            id: 'tbl', name: 'T',
            source: [[{ cell: 'A1', value: 'c1' }, { cell: 'B1', value: 'r' }], [{ cell: 'A2', value: 'c2' }]],
        })

        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        expect(screen.getByTestId('trace-legend')).toBeInTheDocument()
        expect(screen.getByText('legend.conditionMet')).toBeInTheDocument()
        expect(screen.getByText('legend.conditionNotMet')).toBeInTheDocument()
        expect(screen.getByText('legend.result')).toBeInTheDocument()
        expect(screen.queryByText('legend.current')).toBeNull() // no current cell → no such legend entry
    })

    it('shows no legend at all when the table paints no highlights', async () => {
        getFrameHighlights.mockResolvedValue([])
        cacheTable('tbl', { id: 'tbl', name: 'T', source: [[{ cell: 'A1', value: 'v' }]]})

        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        expect(screen.queryByTestId('trace-legend')).toBeNull()
    })

    it('hides the legend in the business view when only the current cell is highlighted — it is suppressed there', async () => {
        getFrameHighlights.mockResolvedValue([{ cell: 'B1', state: 'current' }])
        cacheTable('tbl', { id: 'tbl', name: 'T', source: [[{ cell: 'A1', value: 'Step' }, { cell: 'B1', value: '= x' }]]})

        render(<TraceTableView dimOthers frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        // The current highlight is suppressed in the dimmed view, so neither its entry nor the legend appears.
        expect(screen.queryByTestId('trace-legend')).toBeNull()
    })

    it('keys the current-step entry in the advanced view, where the current cell is painted', async () => {
        getFrameHighlights.mockResolvedValue([{ cell: 'B1', state: 'current' }])
        cacheTable('tbl', { id: 'tbl', name: 'T', source: [[{ cell: 'A1', value: 'Step' }, { cell: 'B1', value: '= x' }]]})

        render(<TraceTableView frameIndex={0} />)
        await waitFor(() => expect(getFrameHighlights).toHaveBeenCalled())

        expect(screen.getByTestId('trace-legend')).toBeInTheDocument()
        expect(screen.getByText('legend.current')).toBeInTheDocument()
    })
})

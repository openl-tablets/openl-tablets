import React from 'react'
import { act, render, screen } from '@testing-library/react'
import traceService from 'services/traceService'
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

describe('TraceTableView', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
    })

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

        await act(async () => {
            render(<TraceTableView frameIndex={0} />)
        })

        const table = screen.getByTestId('trace-table')
        expect(screen.getByText('Header')).toBeInTheDocument()
        expect(screen.getByText('= x')).toBeInTheDocument()

        // The covered cell is skipped, so the merged first row renders a single <td> spanning two columns.
        expect(table.querySelectorAll('tbody tr')[0].querySelectorAll('td')).toHaveLength(1)
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

    it('shows a truncation notice when the backend sliced the table', async () => {
        getFrameHighlights.mockResolvedValue([])
        cacheTable('tbl', {
            id: 'tbl',
            name: 'T',
            totalRows: 900,
            source: [[{ cell: 'A1', value: 'only' }]],
        })

        await act(async () => {
            render(<TraceTableView frameIndex={0} />)
        })

        screen.getByTestId('trace-table')
        // The notice reports the rendered count and the full total.
        expect(screen.getByText(/table\.truncated.*900/)).toBeInTheDocument()
    })
})

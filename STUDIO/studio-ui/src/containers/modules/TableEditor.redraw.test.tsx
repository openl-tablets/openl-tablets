import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { getTableEditors, NO_EDITORS } from '../../services/modules'
import { applyTableActions } from '../../services/tables'
import { TableEditor } from './TableEditor'

vi.mock('../../services/tables', () => ({ applyTableActions: vi.fn() }))
vi.mock('../../services/modules', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/modules')>(),
    getTableEditors: vi.fn(),
}))
vi.mock('react-router-dom', () => ({
    useBlocker: () => ({ state: 'unblocked', proceed: vi.fn(), reset: vi.fn() }),
}))
vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

// The table stands in for the grid, which is what a keystroke must not set drawing again. It draws the one
// cell the screen has taken over, so the field the reader writes in is there to write in.
const grid = vi.hoisted(() => ({ drawn: 0 }))
vi.mock('../../components/RawTableGrid', () => ({
    RawTableGrid: ({ rows, decorate, onOpenCell }: {
        rows: RawTableCell[][]
        decorate?: (cell: RawTableCell, row: number, column: number) => { content?: React.ReactNode } | undefined
        onOpenCell?: (row: number, column: number) => void
    }) => {
        grid.drawn += 1
        return (
            <>
                <button data-testid="open-cell" onClick={() => onOpenCell?.(1, 0)} type="button" />
                <div data-testid="drawn">{String(rows[1]?.[0]?.value ?? '')}</div>
                {decorate?.(rows[1]?.[0] ?? {}, 1, 0)?.content}
            </>
        )
    },
}))

const ROWS: RawTableCell[][] = [
    [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
    [{ cell: 'B5', value: 'Good Morning' }, { cell: 'C5', value: 0 }],
]

describe('TableEditor redrawing', () => {
    beforeEach(() => {
        grid.drawn = 0
        vi.mocked(applyTableActions).mockResolvedValue('table-1')
        vi.mocked(getTableEditors).mockResolvedValue(NO_EDITORS)
    })

    const draw = () => render(
        <TableEditor
            canWrite
            editing
            moduleName="Claims"
            onEditingChange={vi.fn()}
            onSaved={vi.fn()}
            projectId="repo:Rating"
            rows={ROWS}
            tableId="table-1"
        />
    )

    it('draws the table again for none of the keys typed into an open cell', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
        await userEvent.click(screen.getByTestId('open-cell'))
        await userEvent.clear(screen.getByTestId('table-cell-input'))
        const drawnBy = grid.drawn

        await userEvent.type(screen.getByTestId('table-cell-input'), 'Buenos Dias')

        // A table drawn again for every key is a table of thousands of cells built eleven times over, and
        // the tab stops answering. What is being written belongs to the cell it is written in.
        expect(grid.drawn).toBe(drawnBy)
        expect(screen.getByTestId('table-cell-input')).toHaveValue('Buenos Dias')
    })

    it('draws the table again once the cell is written, which is what changed it', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
        await userEvent.click(screen.getByTestId('open-cell'))
        await userEvent.clear(screen.getByTestId('table-cell-input'))
        await userEvent.type(screen.getByTestId('table-cell-input'), 'Buenos Dias')
        const drawnBy = grid.drawn

        await userEvent.keyboard('{Enter}')

        expect(grid.drawn).toBeGreaterThan(drawnBy)
        expect(screen.getByTestId('drawn')).toHaveTextContent('Buenos Dias')
    })
})

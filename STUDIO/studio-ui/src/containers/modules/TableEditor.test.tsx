import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { applyTableActions } from '../../services/tables'
import { TableEditor } from './TableEditor'

vi.mock('../../services/tables', () => ({ applyTableActions: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const ROWS: RawTableCell[][] = [
    [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
    [{ cell: 'B5', value: 0 }, { cell: 'C5', value: 'Good Morning' }],
]

const draw = (over: Partial<Parameters<typeof TableEditor>[0]> = {}) => {
    const onEditingChange = vi.fn()
    const onSaved = vi.fn()
    render(
        <TableEditor
            canWrite
            editing
            onEditingChange={onEditingChange}
            onSaved={onSaved}
            projectId="repo:Rating"
            rows={ROWS}
            tableId="table-1"
            testId="module-table"
            {...over}
        />
    )
    return { onEditingChange, onSaved }
}

/** Opens the cell holding the given text and writes something else into it. */
const write = async (was: string, becomes: string) => {
    await userEvent.dblClick(screen.getByText(was))
    const input = screen.getByTestId('table-cell-input')
    await userEvent.clear(input)
    await userEvent.type(input, `${becomes}{Enter}`)
}

describe('TableEditor', () => {
    beforeEach(() => {
        vi.mocked(applyTableActions).mockResolvedValue('table-1')
    })

    it('opens a cell on a double click and starts editing', async () => {
        const { onEditingChange } = draw({ editing: false })

        await userEvent.dblClick(screen.getByText('Good Morning'))

        expect(screen.getByTestId('table-cell-input')).toHaveValue('Good Morning')
        expect(onEditingChange).toHaveBeenCalledWith(true)
    })

    it('leaves the table alone for a reader who may not write it', async () => {
        draw({ canWrite: false })

        await userEvent.dblClick(screen.getByText('Good Morning'))

        expect(screen.queryByTestId('table-cell-input')).toBeNull()
    })

    it('shows what was written without asking the server for it', async () => {
        draw()

        await write('Good Morning', 'Buenos Dias')

        expect(screen.getByText('Buenos Dias')).toBeInTheDocument()
        expect(applyTableActions).not.toHaveBeenCalled()
    })

    it('sends everything that was written in one request when the reader saves', async () => {
        const { onSaved } = draw()

        await write('Good Morning', 'Buenos Dias')
        await write('0', '6')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledTimes(1))
        expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: 'Buenos Dias' } },
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '6' } },
        ])
        await waitFor(() => expect(onSaved).toHaveBeenCalledWith('table-1'))
    })

    it('takes an edit back and puts it again without asking the server', async () => {
        draw()

        await write('Good Morning', 'Buenos Dias')
        await userEvent.click(screen.getByTestId('table-edit-undo'))
        expect(screen.getByText('Good Morning')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('table-edit-redo'))
        expect(screen.getByText('Buenos Dias')).toBeInTheDocument()
        expect(applyTableActions).not.toHaveBeenCalled()
    })

    it('has nothing to save on a table nobody has written to', () => {
        draw()

        expect(screen.getByTestId('table-edit-save')).toBeDisabled()
        expect(screen.getByTestId('table-edit-undo')).toBeDisabled()
        expect(screen.getByTestId('table-edit-redo')).toBeDisabled()
    })

    it('keeps no band of actions over a table that is only being read', () => {
        draw({ editing: false })

        expect(screen.queryByTestId('table-edit-toolbar')).toBeNull()
    })
})

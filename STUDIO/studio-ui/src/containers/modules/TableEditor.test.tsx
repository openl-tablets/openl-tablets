import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { getTableEditors } from '../../services/modules'
import { applyTableActions } from '../../services/tables'
import { TableEditor } from './TableEditor'

vi.mock('../../services/tables', () => ({ applyTableActions: vi.fn() }))
vi.mock('../../services/modules', () => ({ getTableEditors: vi.fn() }))

const blocker = vi.hoisted(() => ({ state: 'unblocked', proceed: vi.fn(), reset: vi.fn() }))
vi.mock('react-router-dom', () => ({ useBlocker: () => blocker }))

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
            moduleName="Claims"
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
        vi.mocked(getTableEditors).mockResolvedValue({ editors: [], cells: []})
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
        // What is sent is the table as the reader left it, so the cells come in the order they stand in.
        expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '6' } },
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: 'Buenos Dias' } },
        ], 'Claims')
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

    it('has nothing to save on a table nobody has written to', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        expect(screen.getByTestId('table-edit-save')).toBeDisabled()
        expect(screen.getByTestId('table-edit-undo')).toBeDisabled()
        expect(screen.getByTestId('table-edit-redo')).toBeDisabled()
    })

    it('offers the values a cell is chosen from, as the table said when editing started', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            editors: [{ editor: 'combo', choices: ['R1', 'R2'], displayValues: ['Rating 1', 'Rating 2']}],
            cells: [{ row: 1, column: 1, editor: 0 }],
        })
        draw()

        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))
        await userEvent.dblClick(screen.getByText('Good Morning'))

        // The cell holds one of a known set of values, so it is chosen rather than typed.
        expect(await screen.findByText('Rating 1')).toBeInTheDocument()
        expect(screen.getByText('Rating 2')).toBeInTheDocument()
    })

    it('asks how the cells take a value once, however many are opened', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('Good Morning'))
        await userEvent.keyboard('{Escape}')
        await userEvent.dblClick(screen.getByText('0'))

        expect(getTableEditors).toHaveBeenCalledTimes(1)
    })

    it('enters a range in a dialog of its own, in the wording OpenL prints', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            editors: [{ editor: 'range', entryEditor: 'double' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        const to = await screen.findByTestId('range-to')
        await userEvent.type(to, '200')
        await userEvent.click(screen.getByTestId('range-write'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '[0..200]' } },
        ], 'Claims'))
    })

    it('writes a cell over several lines when the reader switches to it', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-cell-switch'))
        // Picking the way of writing takes the pointer out of the field, which must not close the cell.
        await userEvent.click(await screen.findByText('browser.module.editor_switch_multiline'))

        const input = screen.getByTestId('table-cell-input')
        expect(input.tagName).toBe('TEXTAREA')
        expect(input).toHaveValue('Good Morning')
    })

    it('takes the ways out of the cells while the table is being edited', async () => {
        const leading: RawTableCell[][] = [[{
            cell: 'B4',
            value: 'Bank bank',
            metaInfo: {
                usages: [{ start: 0, end: 4, description: 'Bank', tableId: 't2', module: 'm', kind: 'datatype' }],
            },
        }]]
        const { rerender } = render(
            <TableEditor
                canWrite
                editing={false}
                onEditingChange={vi.fn()}
                onOpenUsage={vi.fn()}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={leading}
                tableId="table-1"
            />
        )
        expect(screen.getByTestId('cell-usage-0').tagName).toBe('BUTTON')

        rerender(
            <TableEditor
                canWrite
                editing
                onEditingChange={vi.fn()}
                onOpenUsage={vi.fn()}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={leading}
                tableId="table-1"
            />
        )

        // The same piece of text is still marked, but it no longer leads anywhere a misclick could follow.
        await waitFor(() => expect(screen.getByTestId('cell-usage-0').tagName).toBe('SPAN'))
    })

    it('opens a cell that takes more than one line over several lines at once', async () => {
        const long = 'A value long enough that the cell it sits in wraps it onto more than one line.'
        draw({ rows: [[{ cell: 'B4', value: long }]]})
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText(long))

        expect(screen.getByTestId('table-cell-input').tagName).toBe('TEXTAREA')
    })

    it('opens the cell a message was raised against when the reader asks for it', async () => {
        const { rerender } = render(
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
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        rerender(
            <TableEditor
                canWrite
                editing
                moduleName="Claims"
                onEditingChange={vi.fn()}
                onSaved={vi.fn()}
                openAt="C5"
                projectId="repo:Rating"
                rows={ROWS}
                tableId="table-1"
            />
        )

        // The message names the cell as the workbook names it, and that is the cell that opens.
        expect(await screen.findByTestId('table-cell-input')).toHaveValue('Good Morning')
    })

    it('holds the screen while the table is being written', async () => {
        let finish: (id: string | null) => void = () => {}
        vi.mocked(applyTableActions).mockReturnValue(new Promise(resolve => { finish = resolve }))
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
        await write('Good Morning', 'Buenos Dias')

        await userEvent.click(screen.getByTestId('table-edit-save'))

        // Nothing else can be asked for while the workbook is rewritten and the module built from it again.
        expect(await screen.findByTestId('table-edit-saving')).toBeInTheDocument()

        finish('table-1')
        await waitFor(() => expect(screen.queryByTestId('table-edit-saving')).toBeNull())
    })

    it('asks before the reader leaves with cells they have not saved', async () => {
        blocker.state = 'blocked'
        try {
            draw()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
            await write('Good Morning', 'Buenos Dias')

            await userEvent.click(await screen.findByTestId('table-edit-discard'))

            expect(blocker.proceed).toHaveBeenCalled()
        } finally {
            blocker.state = 'unblocked'
        }
    })

    it('asks the same question when the editor is closed with cells unsaved', async () => {
        const { onEditingChange } = draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
        await write('Good Morning', 'Buenos Dias')

        await userEvent.click(screen.getByTestId('table-edit-cancel'))

        // The same dialog as leaving the page: the reader loses the same work either way.
        expect(await screen.findByText('browser.module.edit_leaving')).toBeInTheDocument()
        expect(onEditingChange).not.toHaveBeenCalledWith(false)

        await userEvent.click(screen.getByTestId('table-edit-discard'))
        expect(onEditingChange).toHaveBeenCalledWith(false)
    })

    it('closes without a question when nothing was written', async () => {
        const { onEditingChange } = draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('table-edit-cancel'))

        expect(onEditingChange).toHaveBeenCalledWith(false)
    })

    it('keeps no band of actions over a table that is only being read', () => {
        draw({ editing: false })

        expect(screen.queryByTestId('table-edit-toolbar')).toBeNull()
    })
})

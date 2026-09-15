import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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

    /** The classes one cell of the drawn table carries. */
    const classesOf = (cell: string): string =>
        document.querySelector(`[data-cell="${cell}"]`)?.className ?? ''

    it('marks the cell a message was raised against, and no other', async () => {
        draw({ markCell: 'C5', editing: false })
        await waitFor(() => expect(screen.getByText('Good Morning')).toBeInTheDocument())

        // The reader lands on a table of any size; the mark is what tells them which cell the message was about.
        expect(classesOf('C5')).not.toEqual(classesOf('B5'))
    })

    it('marks nothing when no message sent the reader here', async () => {
        draw({ editing: false })
        await waitFor(() => expect(screen.getByText('Good Morning')).toBeInTheDocument())

        expect(classesOf('C5')).toEqual(classesOf('B5'))
    })

    it('asks the server for nothing when what the reader did comes to nothing', async () => {
        const { onEditingChange } = draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        // A row added and taken away again leaves the table as it was read, so there is nothing to write.
        await userEvent.click(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-edit-insert_row'))
        await userEvent.click(screen.getByTestId('table-edit-remove_row'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        expect(applyTableActions).not.toHaveBeenCalled()
        await waitFor(() => expect(onEditingChange).toHaveBeenLastCalledWith(false))
    })

    it('opens a cell written with a formula as that formula, and keeps it', async () => {
        const withFormula: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 12, formula: '=6*2' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        draw({ rows: withFormula })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        // Opening it as the number it computed would write that number back over the formula on save.
        await userEvent.dblClick(screen.getByText('12'))

        expect(screen.getByTestId('table-cell-input')).toHaveValue('=6*2')
    })

    it('writes a cell as a formula when the reader asks for the formula editor', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_switch_formula'))

        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '=B5*2{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: '=B5*2' } },
        ], 'Claims'))
    })

    it('writes several numbers into an array cell, and lets nothing else in', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            editors: [{ editor: 'array', separator: ',', entryEditor: 'integer', intOnly: true }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        // The separator stands in the field; a letter does not.
        await userEvent.type(input, '1,2,x3{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '1,2,3' } },
        ], 'Claims'))
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

    /** A cell the module says holds a range. */
    const rangeCell = () => vi.mocked(getTableEditors).mockResolvedValue({
        editors: [{ editor: 'range', entryEditor: 'double' }],
        cells: [{ row: 1, column: 0, editor: 0 }],
    })

    it('enters a range in the panel under the cell, in the wording OpenL prints', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        await userEvent.click(await screen.findByTestId('range-shape-between'))
        const to = screen.getByTestId('range-to')
        await userEvent.clear(to)
        await userEvent.type(to, '200')
        await userEvent.click(screen.getByTestId('range-write'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '[0..200]' } },
        ], 'Claims'))
    })

    it('closes the range panel when the reader clicks away from it', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        expect(await screen.findByTestId('range-editor')).toBeInTheDocument()

        // A click inside the panel leaves it standing; one outside closes it, writing nothing.
        fireEvent.mouseDown(screen.getByTestId('range-from'))
        expect(screen.getByTestId('range-editor')).toBeInTheDocument()
        fireEvent.mouseDown(document.body)

        await waitFor(() => expect(screen.queryByTestId('range-editor')).toBeNull())
        expect(applyTableActions).not.toHaveBeenCalled()
    })

    it('lets the reader write a range cell as text instead', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        expect(await screen.findByTestId('range-editor')).toBeInTheDocument()
        // The way out of the panel is the same one every other cell has.
        await userEvent.click(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_switch_text'))

        expect(screen.queryByTestId('range-editor')).toBeNull()
        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '5+{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '5+' } },
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

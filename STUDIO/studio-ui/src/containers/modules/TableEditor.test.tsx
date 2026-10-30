import { createRef } from 'react'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { SystemContext } from '../../contexts'
import { ApiHttpError, notifyLoadFailure } from '../../services/apiCall'
import { getTableEditors, NO_EDITORS } from '../../services/modules'
import { applyTableActions } from '../../services/tables'
import { rolesOf } from '../../components/tableRoles'
import { selectIn } from './MarkedTextField'
import { TableEditor, type TableEditorHandle } from './TableEditor'
import { paperToken } from '../../styles/paper'
import { hex, renderInTheme, tokenFor } from '../../testing/theme'
import { DEFAULT_USER_SETTINGS, useSettingsStore } from '../../store/settingsStore'

vi.mock('../../services/tables', () => ({ applyTableActions: vi.fn() }))
vi.mock('../../services/apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/apiCall')>(),
    notifyLoadFailure: vi.fn(),
}))
vi.mock('../../services/modules', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/modules')>(),
    getTableEditors: vi.fn(),
}))

const blocker = vi.hoisted(() => ({ state: 'unblocked', proceed: vi.fn(), reset: vi.fn() }))
// What the editor asks the router to hold a leaving reader back by.
const holdBack = vi.hoisted((): { when: unknown } => ({ when: undefined }))
vi.mock('react-router', () => ({
    useBlocker: (when: unknown) => {
        holdBack.when = when
        return blocker
    },
}))

/** Whether the router would hold the reader back if they left now, asked the way the router asks it. */
const holdsBack = (): boolean => (typeof holdBack.when === 'function'
    ? Boolean((holdBack.when as (leaving: unknown) => boolean)({}))
    : Boolean(holdBack.when))

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
    const editor = (props: Partial<Parameters<typeof TableEditor>[0]>) => (
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
            {...props}
        />
    )
    const { rerender } = render(editor(over))
    /** Draws the editor again with more of its props changed, as the screen holding it does. */
    const redraw = (more: Partial<Parameters<typeof TableEditor>[0]>) => rerender(editor({ ...over, ...more }))
    return { onEditingChange, onSaved, redraw }
}

/** The settings of a server that does not format tables on save. */
const NOT_FORMATTING_ON_SAVE = {
    isExternalAuthSystem: false,
    isUserManagementEnabled: false,
    isGroupsManagementEnabled: false,
    isPersonalAccessTokenEnabled: false,
    isAuditLogEnabled: false,
    systemSettings: { formatTablesOnSave: false },
}

/**
 * Draws the editor where the look of a cell is the reader's to set: tables are not formatted on save, and the
 * formatting of the workbook is shown, unless asked otherwise.
 */
const drawFormatting = (over: Partial<Parameters<typeof TableEditor>[0]> = {}, excelFormatting = true) =>
    renderInTheme(
        <SystemContext.Provider value={NOT_FORMATTING_ON_SAVE}>
            <TableEditor
                canWrite
                editing
                moduleName="Claims"
                onEditingChange={vi.fn()}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={ROWS}
                tableId="table-1"
                testId="module-table"
                {...over}
            />
        </SystemContext.Provider>,
        { excelFormatting })

/** The cell drawn at the given place, for a place that holds nothing to search for by its text. */
const cellOf = (row: number, column: number): HTMLElement => {
    const drawn = screen.getByTestId('module-table').querySelectorAll('tr')[row]?.querySelectorAll('td')[column]
    if (drawn === undefined) {
        throw new Error(`No cell at row ${row}, column ${column}`)
    }
    return drawn
}

/** Opens the cell holding the given text and writes something else into it. */
const write = async (was: string, becomes: string) => {
    await userEvent.dblClick(screen.getByText(was))
    const input = screen.getByTestId('table-cell-input')
    await userEvent.clear(input)
    await userEvent.type(input, `${becomes}{Enter}`)
}

/** Gives the cell measured text fragments, then restores the browser's measurement. */
const withMeasuredText = async (tops: number[], check: () => Promise<void>,
    whole: { top: number, height: number }[] = tops.map(top => ({ top, height: 16 }))) => {
    const original = Object.getOwnPropertyDescriptor(Range.prototype, 'getClientRects')
    Object.defineProperty(Range.prototype, 'getClientRects', {
        configurable: true,
        value(this: Range) {
            return this.startContainer.nodeType === Node.TEXT_NODE
                ? tops.map(top => ({ top, height: 16 }))
                : whole
        },
    })
    try {
        await check()
    } finally {
        if (original === undefined) {
            Reflect.deleteProperty(Range.prototype, 'getClientRects')
        } else {
            Object.defineProperty(Range.prototype, 'getClientRects', original)
        }
    }
}

describe('TableEditor', () => {
    beforeEach(() => {
        vi.mocked(applyTableActions).mockResolvedValue('table-1')
        vi.mocked(getTableEditors).mockResolvedValue(NO_EDITORS)
        useSettingsStore.setState({ settings: DEFAULT_USER_SETTINGS })
    })

    it('asks about the table now on screen even while the one before it is still answering', async () => {
        // The screen is kept across tables, so an answer still on its way belongs to a table nobody is looking
        // at any more — and it must not stand in the way of asking about the one that is.
        let answerFirst: ((answer: typeof NO_EDITORS) => void) | undefined
        vi.mocked(getTableEditors)
            .mockImplementationOnce(() => new Promise(resolve => { answerFirst = resolve }))
            .mockResolvedValue(NO_EDITORS)
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
                testId="module-table"
            />
        )
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        rerender(
            <TableEditor
                canWrite
                editing
                moduleName="Claims"
                onEditingChange={vi.fn()}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={ROWS}
                tableId="table-2"
                testId="module-table"
            />
        )
        act(() => answerFirst?.(NO_EDITORS))

        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(2))
        expect(vi.mocked(getTableEditors).mock.calls[1]?.[1]).toBe('table-2')
    })

    describe('in the two looks of a table', () => {
        // A Datatype field the workbook fills in yellow, which the Studio look draws by its role instead.
        const named: RawTableCell[][] = [[{ cell: 'B4', value: 'name', style: { background: '#ffff00' } }]]
        const roles = rolesOf({
            pos: 'B4:B4',
            regions: [{ role: 'key', top: 0, left: 0, bottom: 0, right: 0 }],
        })
        const drawIn = (excelFormatting: boolean, over: Partial<Parameters<typeof TableEditor>[0]> = {}) =>
            renderInTheme(
                <TableEditor
                    canWrite
                    editing={false}
                    moduleName="Claims"
                    onEditingChange={vi.fn()}
                    onSaved={vi.fn()}
                    projectId="repo:Rating"
                    roles={roles}
                    rows={named}
                    tableId="table-1"
                    testId="module-table"
                    {...over}
                />,
                { theme: 'standard', mode: 'dark', excelFormatting }
            )
        const dark = tokenFor('standard', true)

        it('draws a table by the roles of its cells in the colours of the Studio theme', () => {
            drawIn(false)

            expect(screen.getByText('name')).not.toHaveStyle({ backgroundColor: '#ffff00' })
            expect(screen.getByText('name')).toHaveAttribute('data-role', 'key')
            expect(screen.getByTestId('module-table')).toHaveStyle({ backgroundColor: dark.colorBgContainer })
        })

        it('writes a cell in the colours of the Studio theme', async () => {
            drawIn(false, { editing: true })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.dblClick(screen.getByText('name'))

            expect(screen.getByTestId('table-cell-input')).toHaveStyle({ backgroundColor: dark.colorBgContainer })
        })

        it('draws the formatting of the workbook on its paper with Excel Formatting on', () => {
            drawIn(true)

            expect(screen.getByText('name')).toHaveStyle({ backgroundColor: '#ffff00' })
            expect(screen.getByTestId('module-table')).toHaveStyle({ backgroundColor: paperToken().colorBgContainer })
        })

        it('writes a cell on the paper of the workbook with Excel Formatting on', async () => {
            drawIn(true, { editing: true })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.dblClick(screen.getByText('name'))

            expect(screen.getByTestId('table-cell-input')).toHaveStyle({ backgroundColor: paperToken().colorBgContainer })
        })
    })

    it('asks again when the table is taken up a second time, so the project is held again', async () => {
        // Putting the table down lets the project go. Taking it up again without asking would let the reader
        // fill in cells of a project somebody else may have taken meanwhile, and say so only on the save.
        const of = (editing: boolean) => (
            <TableEditor
                canWrite
                editing={editing}
                moduleName="Claims"
                onEditingChange={vi.fn()}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={ROWS}
                tableId="table-1"
                testId="module-table"
            />
        )
        const { rerender } = render(of(true))
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        rerender(of(false))
        rerender(of(true))

        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(2))
    })

    it('keeps editing when a window it cannot read is asked for over cells already written', async () => {
        // Reading more rows asks about the wider window. Refused, the cells the reader has written are still
        // theirs to save, and Save is on the editing toolbar — so editing stays where it is.
        const onEditingChange = vi.fn()
        const of = (maxRows: number) => (
            <TableEditor
                canWrite
                editing
                maxRows={maxRows}
                moduleName="Claims"
                onEditingChange={onEditingChange}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={ROWS}
                tableId="table-1"
                testId="module-table"
            />
        )
        const { rerender } = render(of(2))
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))
        await write('Good Morning', 'Good Day')

        vi.mocked(getTableEditors).mockRejectedValue(new ApiHttpError(404, 'The table is not found.'))
        rerender(of(4))

        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(2))
        expect(onEditingChange).not.toHaveBeenCalledWith(false)
        expect(screen.getByTestId('table-edit-save')).toBeInTheDocument()
    })

    it('closes again when the table cannot be taken up, rather than editing what cannot be saved', async () => {
        // Asking how the cells are written is what takes the table up to write it, and the server reserves the
        // project then. Another user holding it answers 409, and there is nothing to edit.
        vi.mocked(getTableEditors).mockRejectedValue(
            new ApiHttpError(409, 'The project is locked by user \'user2\'.')
        )

        const { onEditingChange } = draw()

        await waitFor(() => expect(onEditingChange).toHaveBeenCalledWith(false))
        expect(notifyLoadFailure).toHaveBeenCalledWith(
            'browser.module.edit_refused',
            expect.objectContaining({ status: 409 })
        )
    })

    it('opens a date cell on the date it holds, whichever of OpenL\'s formats it is written in', async () => {
        const dated: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Date on)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: '2024-03-07' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'date' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw({ rows: dated })

        await userEvent.dblClick(screen.getByText('2024-03-07'))

        // The cell holds an ISO date, which OpenL reads and the calendar must show rather than open blank: in the
        // pattern of the reader's locale, where they picked no Date Format.
        await waitFor(() => expect(screen.getByTestId('table-cell-input')).toHaveValue('03/07/2024'))
    })

    it('writes a picked date in ISO 8601, whatever format the cell was written in and the reader reads it in', async () => {
        const dated: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Date on)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: '3/7/2024' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'date' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        useSettingsStore.setState({ settings: { ...DEFAULT_USER_SETTINGS, useSystemDateFormat: false, dateFormat: 'DD.MM.YYYY' } })
        draw({ rows: dated })
        await userEvent.dblClick(screen.getByText('3/7/2024'))
        // shown in the Date Format the reader picked
        await waitFor(() => expect(screen.getByTestId('table-cell-input')).toHaveValue('07.03.2024'))

        // The calendar is the field's to drop when the reader goes to it, not the cell's to open unasked.
        await userEvent.click(screen.getByTestId('table-cell-input'))
        await userEvent.click(await screen.findByTitle('2024-03-14'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        // The server is sent a date in ISO 8601, and stores it as a date.
        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '2024-03-14' } },
        ], 'Claims', undefined))
    })

    it('keeps a date the reader emptied when they go on to another cell', async () => {
        const dated: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Date on)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: '2024-03-07' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'date' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw({ rows: dated })
        await userEvent.dblClick(screen.getByText('2024-03-07'))
        await waitFor(() => expect(screen.getByTestId('table-cell-input')).toHaveValue('03/07/2024'))

        // Backspace empties the cell, as it did in the Editor. The calendar was never opened, so what keeps
        // the empty cell is the reader going elsewhere — which is what keeps every other cell too.
        await userEvent.keyboard('{Backspace}')
        await userEvent.click(cellOf(1, 1))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '' } },
        ], 'Claims', undefined))
    })

    it('writes nothing for a formula cell the reader only looked into', async () => {
        const computed: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 42, formula: '=B2*C2' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        draw({ rows: computed })

        // The cell opens on the formula it was written with, and closing it again leaves that formula alone:
        // what it holds is the formula, not the 42 it computed.
        await userEvent.dblClick(screen.getByText('42'))
        expect(screen.getByTestId('table-cell-input')).toHaveValue('=B2*C2')
        await userEvent.keyboard('{Enter}')

        expect(screen.getByTestId('table-edit-save')).toBeDisabled()
    })

    it('writes a cell of a table read with no roles on the paper of the table, white whatever the theme', async () => {
        renderInTheme(
            <TableEditor
                canWrite
                editing
                moduleName="Claims"
                onEditingChange={vi.fn()}
                onSaved={vi.fn()}
                projectId="repo:Rating"
                rows={ROWS}
                tableId="table-1"
                testId="module-table"
            />,
            { theme: 'monokai', mode: 'dark' }
        )

        await userEvent.dblClick(await screen.findByText('Good Morning'))

        expect(screen.getByTestId('table-cell-input')).toHaveStyle({ backgroundColor: paperToken().colorBgContainer })
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
        // The row is laid down under the one the reader is on, so it is that row they take away again.
        await userEvent.click(cellOf(2, 0))
        await userEvent.click(screen.getByTestId('table-edit-remove_row'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        expect(applyTableActions).not.toHaveBeenCalled()
        await waitFor(() => expect(onEditingChange).toHaveBeenLastCalledWith(false))
    })

    it('lays an added row under the one the reader is on, as the Editor did', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.click(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-edit-insert_row'))
        // A row left blank would split the table, so it is filled in before the table is written.
        await userEvent.dblClick(cellOf(2, 0))
        await userEvent.type(screen.getByTestId('table-cell-input'), '12{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalled())
        const [, , actions] = vi.mocked(applyTableActions).mock.calls[0] ?? []
        expect(actions).toEqual([{
            operation: 'insert',
            target: { type: 'rows', position: 2, cells: [[{ value: '12' }, { value: '' }]]},
        }])
    })

    describe('where the reader stands once a line is taken away', () => {
        const rules: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 3 },
                { covered: true }, { covered: true }],
            [{ cell: 'B5', value: 'R1' }, { cell: 'C5', value: 0 }, { cell: 'D5', value: 'Good Morning' }],
            [{ cell: 'B6', value: 'R2' }, { cell: 'C6', value: 12 }, { cell: 'D6', value: 'Good Afternoon' }],
            [{ cell: 'B7', value: 'R3' }, { cell: 'C7', value: 18 }, { cell: 'D7', value: 'Good Evening' }],
        ]

        /** Opens the cell the reader is on, with the key, so the keys have to reach the table. */
        const openPicked = async () => {
            await userEvent.keyboard('{Enter}')
            return screen.getByTestId('table-cell-input')
        }

        /** Draws the table of rules, and waits for what its cells ask to be written with. */
        const drawRules = async (over: Partial<Parameters<typeof TableEditor>[0]> = {}) => {
            draw({ rows: rules, ...over })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
        }

        it.each([
            ['the row that moves up into the place of the one taken away', 'Good Morning', 'remove_row',
                'Good Afternoon'],
            ['the new last row when the last one is taken away', 'Good Evening', 'remove_row', 'Good Afternoon'],
            ['the column that moves along into the place of the one taken away', 'R2', 'remove_column', '12'],
            ['the new last column when the last one is taken away', 'Good Evening', 'remove_column', '18'],
        ])('lands on %s', async (_place, picked, action, landed) => {
            await drawRules()

            await userEvent.click(screen.getByText(picked))
            await userEvent.click(screen.getByTestId(`table-edit-${action}`))

            expect(await openPicked()).toHaveValue(landed)
        })

        it('takes several rows away one after another without picking a cell again', async () => {
            await drawRules()

            await userEvent.click(screen.getByText('R1'))
            await userEvent.click(screen.getByTestId('table-edit-remove_row'))
            await userEvent.click(screen.getByTestId('table-edit-remove_row'))

            expect(screen.queryByText('Good Afternoon')).not.toBeInTheDocument()
            expect(await openPicked()).toHaveValue('R3')
        })

        it('lands on the merged cell the place belongs to', async () => {
            // Every rule is taken away but the title, which reaches over all three columns.
            await drawRules({ rows: rules.slice(0, 2) })

            await userEvent.click(screen.getByText('Good Morning'))
            await userEvent.click(screen.getByTestId('table-edit-remove_row'))

            expect(await openPicked()).toHaveValue('Rules String Greeting(Integer hour)')
        })

        it('picks nothing once only the rows kept out of sight are left', async () => {
            await drawRules({ rows: rules.slice(0, 2), hiddenRows: 1 })

            await userEvent.click(screen.getByText('Good Morning'))
            await userEvent.click(screen.getByTestId('table-edit-remove_row'))

            // The header left is drawn nowhere, so nothing on it is offered — not only the row it is on.
            expect(screen.getByTestId('table-edit-remove_row')).toBeDisabled()
            expect(screen.getByTestId('table-edit-remove_column')).toBeDisabled()
        })

        it('stays on the row it landed on when a row is laid down next', async () => {
            await drawRules()

            await userEvent.click(screen.getByText('Good Evening'))
            await userEvent.click(screen.getByTestId('table-edit-remove_row'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))

            // The row is laid down under the one the reader landed on, and the reader stays on that one.
            expect(cellOf(3, 2)).toHaveTextContent('')
            // The reader's cell is marked as picked; its neighbour of the same row is not.
            expect(classesOf('D6')).not.toEqual(classesOf('B6'))
            expect(classesOf('D5')).toEqual(classesOf('B5'))
        })

        it('stands nowhere in another table until a cell of it is picked', async () => {
            const of = (tableId: string, rows: RawTableCell[][]) => (
                <TableEditor
                    canWrite
                    editing
                    moduleName="Claims"
                    onEditingChange={vi.fn()}
                    onSaved={vi.fn()}
                    projectId="repo:Rating"
                    rows={rows}
                    tableId={tableId}
                    testId="module-table"
                />
            )
            const { rerender } = render(of('table-1', rules))
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
            await userEvent.click(screen.getByText('Good Evening'))

            rerender(of('table-2', ROWS))
            await waitFor(() => expect(getTableEditors).toHaveBeenLastCalledWith('repo:Rating', 'table-2',
                expect.anything()))

            expect(screen.getByTestId('table-edit-remove_row')).toBeDisabled()
            expect(screen.getByTestId('table-edit-remove_column')).toBeDisabled()
        })
    })

    describe('what the screen beside it is told and may ask', () => {
        it('says while it holds cells the reader has not saved, and says when it holds none again', async () => {
            const onDirtyChange = vi.fn()
            draw({ onDirtyChange })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
            expect(onDirtyChange).toHaveBeenLastCalledWith(false)

            await write('Good Morning', 'Buenos Dias')

            // Anything that reads the table again would lose this, so the screen around it has to know.
            await waitFor(() => expect(onDirtyChange).toHaveBeenLastCalledWith(true))

            await userEvent.click(screen.getByTestId('table-edit-save'))

            await waitFor(() => expect(onDirtyChange).toHaveBeenLastCalledWith(false))
        })

        it('writes what it holds when asked, and answers the table that write leaves', async () => {
            vi.mocked(applyTableActions).mockResolvedValue('table-2')
            const held = createRef<TableEditorHandle>()
            draw({ ref: held })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
            await write('Good Morning', 'Buenos Dias')

            const written = await act(() => held.current?.write() ?? Promise.resolve(null))

            // The write may rename the table, and what writes its properties next has to address it by the id
            // it has afterwards rather than the one it started with.
            expect(applyTableActions).toHaveBeenCalled()
            expect(written).toEqual({ tableId: 'table-2', changed: true })
        })

        it('lets the screen open the table a save has renamed, without holding the reader back', async () => {
            vi.mocked(applyTableActions).mockResolvedValue('table-2')
            // The screen opens the table under its new id as soon as the save answers, before the editor is
            // drawn again, and the router asks then whether leaving would lose anything.
            const heldBackOnSave: boolean[] = []
            draw({ onSaved: () => heldBackOnSave.push(holdsBack()) })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
            await write('Good Morning', 'Buenos Dias')
            expect(holdsBack()).toBe(true)

            await userEvent.click(screen.getByTestId('table-edit-save'))

            await waitFor(() => expect(heldBackOnSave).toEqual([false]))
        })

        it('answers the table unchanged when it holds nothing to write', async () => {
            const held = createRef<TableEditorHandle>()
            draw({ ref: held })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            const written = await act(() => held.current?.write() ?? Promise.resolve(null))

            expect(applyTableActions).not.toHaveBeenCalled()
            expect(written).toEqual({ tableId: 'table-1', changed: false })
        })

        it('refuses to write while a row the reader added is empty', async () => {
            const held = createRef<TableEditorHandle>()
            draw({ ref: held })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())
            await userEvent.click(screen.getByText('Good Morning'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))

            const written = await act(() => held.current?.write() ?? Promise.resolve(null))

            // A blank row splits the table. Nothing of the table is written, so nothing else of it is either.
            expect(written).toBeNull()
            expect(applyTableActions).not.toHaveBeenCalled()
        })
    })

    describe('what a cell asks to be written with once a line has been laid down', () => {
        // A datatype as the ticket has it: a decimal default on one row, a whole number on the next.
        const policy: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Datatype Policy', colspan: 3 }, { covered: true }, { covered: true }],
            [{ cell: 'B5', value: 'String' }, { cell: 'C5', value: 'name' }, { cell: 'D5', value: 'x' }],
            [{ cell: 'B6', value: 'Double' }, { cell: 'C6', value: 'rate' }, { cell: 'D6', value: '0.5' }],
            [{ cell: 'B7', value: 'Integer' }, { cell: 'C7', value: 'count' }, { cell: 'D7', value: '3' }],
        ]

        const typed = () => {
            vi.mocked(getTableEditors).mockResolvedValue({
                ...NO_EDITORS,
                editors: [{ editor: 'numeric' }, { editor: 'numeric', intOnly: true }],
                cells: [{ row: 2, column: 2, editor: 0 }, { row: 3, column: 2, editor: 1 }],
            })
            return draw({ rows: policy })
        }

        it('keeps a decimal a decimal after a row is laid down above it', async () => {
            typed()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            // The row laid down under `name` moves the decimal into the place the whole number held.
            await userEvent.click(screen.getByText('name'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            await userEvent.dblClick(screen.getByText('0.5'))
            const box = screen.getByTestId('table-cell-input')
            await userEvent.clear(box)
            await userEvent.type(box, '0.75{Enter}')
            // A row left blank would split the table, so it is filled in before the table is written.
            await userEvent.dblClick(cellOf(2, 0))
            await userEvent.type(screen.getByTestId('table-cell-input'), 'String{Enter}')
            await userEvent.click(screen.getByTestId('table-edit-save'))

            // Asked for by where the cell now sits, the decimal would open in the whole number's box, which
            // takes no point at all: `0.75` would reach the workbook as `75`.
            await waitFor(() => expect(applyTableActions).toHaveBeenCalled())
            const [, , actions] = vi.mocked(applyTableActions).mock.calls[0] ?? []
            expect(actions).toContainEqual({
                operation: 'update', target: { type: 'cell', row: 3, column: 2, value: '0.75' },
            })
        })

        it('writes a cell of the row it laid down as plain text, which nothing is known about yet', async () => {
            typed()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('name'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            await userEvent.dblClick(cellOf(2, 2))
            await userEvent.type(screen.getByTestId('table-cell-input'), 'a note{Enter}')

            // The place it stands in was the decimal's; a box that takes only numbers would refuse the words.
            expect(cellOf(2, 2)).toHaveTextContent('a note')
        })
    })

    describe('what a cell of a table whose headers declare its columns asks to be written with', () => {
        // A Data table as the ticket has it: one column declared to hold whole numbers, and one row written.
        const hours: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Data Integer Hours' }],
            [{ cell: 'B5', value: 'this' }],
            [{ cell: 'B6', value: 'Hour' }],
            [{ cell: 'B7', value: '12' }],
        ]

        const declared = () => {
            vi.mocked(getTableEditors).mockResolvedValue({
                areas: [{ row: 3, column: 0, rows: null, columns: 1, editor: 0 }],
                cells: [],
                editors: [{ editor: 'numeric', intOnly: true }],
                kind: 'declared',
            })
            return draw({ rows: hours })
        }

        it('writes a cell of the row it laid down the way the column it stands in is declared', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('12'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            await userEvent.dblClick(cellOf(3, 0))

            // The column holds whole numbers however many rows the table has, the new one included, and a
            // field that took words would let one reach a column the workbook reads as numbers.
            expect(screen.getByTestId('table-cell-input')).toHaveAttribute('role', 'spinbutton')
        })

        it('writes a cell the column was already declared for the same way', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.dblClick(screen.getByText('12'))

            expect(screen.getByTestId('table-cell-input')).toHaveAttribute('role', 'spinbutton')
        })

        it('writes the first cell of a table whose columns are declared and which holds no rows', async () => {
            // The table as a reader meets it once every row of it has been taken away.
            vi.mocked(getTableEditors).mockResolvedValue({
                areas: [{ row: 3, column: 0, rows: null, columns: 1, editor: 0 }],
                cells: [],
                editors: [{ editor: 'numeric', intOnly: true }],
                kind: 'declared',
            })
            draw({ rows: hours.slice(0, 3) })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('Hour'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            await userEvent.dblClick(cellOf(3, 0))

            // The column begins under the title, past the last row the table was read with, and the row just
            // laid down is the first of it.
            expect(screen.getByTestId('table-cell-input')).toHaveAttribute('role', 'spinbutton')
        })

        it('leaves the headings the column is declared in as they are', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            // `this` names the field the column stands for; it is not one of the numbers the column holds.
            await userEvent.dblClick(screen.getByText('this'))

            expect(screen.getByTestId('table-cell-input')).not.toHaveAttribute('role', 'spinbutton')
        })
    })

    describe('what a cell of a lookup asks to be written with', () => {
        // A lookup as Tutorial 1 has it: the rules run down one condition and across another, and what they
        // meet in is the return.
        const lookup: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules Double Premium(String age, String status)', colspan: 3 },
                { covered: true }, { covered: true }],
            [{ cell: 'B5', value: 'C1' }, { cell: 'C5', value: 'HC1' }, { cell: 'D5', value: 'RET1' }],
            [{ cell: 'B6', value: 'age' }, { cell: 'C6', value: 'status' }, { cell: 'D6', value: '' }],
            [{ cell: 'B7', value: 'String' }, { cell: 'C7', value: 'String' }, { cell: 'D7', value: '' }],
            [{ cell: 'B8', value: 'Driver Age' }, { cell: 'C8', value: 'Married' }, { cell: 'D8', value: 'Single' }],
            [{ cell: 'B9', value: 'Young' }, { cell: 'C9', value: '700' }, { cell: 'D9', value: '720' }],
        ]

        const declared = () => {
            vi.mocked(getTableEditors).mockResolvedValue({
                areas: [
                    // The condition the rules run across, and what they meet the vertical one in. The vertical
                    // condition holds text, which a screen writes without being told and the table never names.
                    { row: 4, column: 1, rows: 1, columns: null, editor: 0 },
                    { row: 5, column: 1, rows: null, columns: null, editor: 1 },
                ],
                cells: [],
                editors: [{ editor: 'combo', choices: ['Married', 'Single']}, { editor: 'numeric' }],
                kind: 'declared',
            })
            return draw({ rows: lookup })
        }

        it('writes a cell of the row it laid down the way the rules that meet there are written', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('Young'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            await userEvent.dblClick(cellOf(6, 1))

            // The new rule meets the horizontal one where the returns are, and a return is a number.
            expect(screen.getByTestId('table-cell-input')).toHaveAttribute('role', 'spinbutton')
        })

        it('leaves the condition the rules run across where it is when a row is laid down under it', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('Young'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            await userEvent.dblClick(cellOf(6, 0))

            // The row laid down is a rule of the vertical condition, which holds the driver's age as text.
            expect(screen.getByTestId('table-cell-input')).not.toHaveAttribute('role', 'spinbutton')
            expect(screen.queryByRole('combobox')).toBeNull()
        })

        it('writes a cell of the column it laid down the way the rules that meet there are written', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            // A column laid down beside the last one is another value of the condition the rules run across.
            await userEvent.click(screen.getByText('720'))
            await userEvent.click(screen.getByTestId('table-edit-insert_column'))
            await userEvent.dblClick(cellOf(5, 2))

            expect(screen.getByTestId('table-cell-input')).toHaveAttribute('role', 'spinbutton')
        })

        it('offers the same choices in the column it laid down as the condition it runs across allows', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('720'))
            await userEvent.click(screen.getByTestId('table-edit-insert_column'))
            await userEvent.dblClick(cellOf(4, 2))

            expect(screen.getByRole('combobox')).toBeInTheDocument()
        })

        it('leaves a heading of the table as it is when a row is laid down under the rules', async () => {
            declared()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('Young'))
            await userEvent.click(screen.getByTestId('table-edit-insert_row'))
            // `status` names the condition the rules run across; it is no value of the rules themselves.
            await userEvent.dblClick(screen.getByText('status'))

            expect(screen.getByTestId('table-cell-input')).not.toHaveAttribute('role', 'spinbutton')
        })
    })

    describe('with the header rows kept out of sight', () => {
        // A decision table as the reader meets it with Show Header off: the four header rows are drawn
        // nowhere, and what they see is the title row and the rules under it.
        const withHeader: RawTableCell[][] = [
            [{ cell: 'D57', value: 'Rules Double Premium(String age)', colspan: 2 }, { covered: true }],
            [{ cell: 'D58', value: 'C1' }, { cell: 'E58', value: 'RET1' }],
            [{ cell: 'D59', value: 'age' }, { cell: 'E59', value: '' }],
            [{ cell: 'D60', value: 'String' }, { cell: 'E60', value: '' }],
            [{ cell: 'D61', value: 'Driver Age' }, { cell: 'E61', value: 'Premium Increase' }],
            [{ cell: 'D62', value: 'Young Driver' }, { cell: 'E62', value: 700 }],
            [{ cell: 'D63', value: 'Standard Driver' }, { cell: 'E63', value: 500 }],
        ]
        const HIDDEN = 4

        it('draws the table from the first row it shows, and none of the ones put away', async () => {
            draw({ rows: withHeader, hiddenRows: HIDDEN })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            expect(screen.getByTestId('module-table').querySelectorAll('tr')).toHaveLength(3)
            expect(screen.queryByText('Rules Double Premium(String age)')).not.toBeInTheDocument()
            expect(screen.getByText('Driver Age')).toBeInTheDocument()
        })

        it('writes a cell to the row it stands on in the table, not the row it is drawn at', async () => {
            draw({ rows: withHeader, hiddenRows: HIDDEN })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            // Opened where it is drawn — the last cell of the third row on screen, which the reader sees as
            // the premium of Standard Driver.
            await userEvent.dblClick(cellOf(2, 1))
            const input = screen.getByTestId('table-cell-input')
            await userEvent.clear(input)
            await userEvent.type(input, '555{Enter}')
            await userEvent.click(screen.getByTestId('table-edit-save'))

            // That cell stands on the seventh row of the table. Addressed as it is drawn it would be written
            // four rows higher — over another rule, or over the header, which stops the table compiling.
            await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
                { operation: 'update', target: { type: 'cell', row: 6, column: 1, value: '555' } },
            ], 'Claims', undefined))
        })

        it('offers to take away the first row it draws, which is no header of the table', async () => {
            draw({ rows: withHeader, hiddenRows: HIDDEN })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(cellOf(0, 0))

            // The row that is kept is the table's own first one, and that one is on screen nowhere.
            expect(screen.getByTestId('table-edit-remove_row')).toBeEnabled()
        })

        it('keeps the reader among the rows it draws', async () => {
            draw({ rows: withHeader, hiddenRows: HIDDEN })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(cellOf(0, 0))
            await userEvent.keyboard('{ArrowUp}')
            await userEvent.keyboard('{Enter}')

            // Up from the first drawn row leads into the header, which is drawn nowhere: the reader stays.
            expect(screen.getByTestId('table-cell-input')).toHaveValue('Driver Age')
        })

        it('draws the line under the rows put away over the first row it shows', async () => {
            // The workbook draws a line under the titles, which the reader gives the last row of the header.
            const line = { style: 'solid' as const, width: 1, color: '#ff0000' }
            const lined = withHeader.map((row, at) => row.map(cell => (cell.covered || at !== HIDDEN - 1 ? cell : {
                ...cell,
                style: { border: { bottom: line } },
            })))
            draw({ editing: false, rows: lined, hiddenRows: HIDDEN })
            await waitFor(() => expect(screen.getByText('Driver Age')).toBeInTheDocument())

            expect(cellOf(0, 0).style.borderTop).toBe('1px solid rgb(255, 0, 0)')
            expect(cellOf(0, 1).style.borderTop).toBe('1px solid rgb(255, 0, 0)')
        })

        it('counts the rows put away out of the line numbers', async () => {
            draw({ rows: withHeader, hiddenRows: HIDDEN, layout: { firstDataLine: 5 } })
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            // The data begins on the table's sixth row, which is the second of the three drawn — so the
            // first drawn row is numbered not at all and the rules under it are the first and the second.
            expect(screen.getAllByTestId('table-line-number').map(cell => cell.textContent)).toEqual(['1', '2'])
            expect(cellOf(1, 0)).toHaveTextContent('1')
        })
    })

    it('lays a row down inside a merged group without pushing its cells out of their columns', async () => {
        // The group `Young Driver` is one cell over the two rules under it, the way a rules table is written.
        const grouped: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Premium(String age, String status)', colspan: 3 },
                { covered: true }, { covered: true }],
            [{ cell: 'B5', value: 'R1' }, { cell: 'C5', value: 'Young Driver', rowspan: 2 },
                { cell: 'D5', value: 'Married' }],
            [{ cell: 'B6', value: 'R2' }, { covered: true }, { cell: 'D6', value: 'Single' }],
        ]
        draw({ rows: grouped })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.click(screen.getByText('Married'))
        await userEvent.click(screen.getByTestId('table-edit-insert_row'))

        // The group grows over the new row, so the row has two cells and they stand under their own
        // headings; drawn with a third the group leaves no room for, they would all be one column out.
        const laid = screen.getByTestId('module-table').querySelectorAll('tr')[2]
        expect(laid?.querySelectorAll('td')).toHaveLength(2)

        await userEvent.dblClick(cellOf(2, 0))
        await userEvent.type(screen.getByTestId('table-cell-input'), 'R1b{Enter}')
        await userEvent.dblClick(cellOf(2, 1))
        await userEvent.type(screen.getByTestId('table-cell-input'), 'Widowed{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalled())
        const [, , actions] = vi.mocked(applyTableActions).mock.calls[0] ?? []
        expect(actions).toEqual([{
            operation: 'insert',
            target: {
                type: 'rows',
                position: 2,
                cells: [[{ value: 'R1b' }, { value: '', covered: true }, { value: 'Widowed' }]],
            },
        }])
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

    it('puts nothing beside the field of an open cell, so the column keeps its width', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))

        // A button beside the field would widen the column and shift the whole table as a cell is opened.
        const inCell = screen.getByTestId('table-cell-switch')
        expect(inCell.querySelector('button')).toBeNull()
        expect(screen.getByTestId('table-cell-input')).toBeInTheDocument()
    })

    it('writes a cell as a formula when the reader asks for the formula editor', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        // Another way of writing the value is asked for with the right button, as the Editor asked for it.
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_kind_formula'))

        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '=B5*2{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: '=B5*2' } },
        ], 'Claims', undefined))
    })

    it('writes a cell as an Excel formula when the reader asks for the Excel formula editor', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_kind_excelFormula'))

        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '=B5*2{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, formula: '=B5*2' } },
        ], 'Claims', undefined))
    })

    it('writes an Excel formula without the spaces typed around it', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_kind_excelFormula'))
        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '  =B5*2 {Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, formula: '=B5*2' } },
        ], 'Claims', undefined))
    })

    it('opens a cell written with an Excel formula in the Excel formula editor, and writes the formula changed', async () => {
        const withFormula: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 12, formula: '=6*2' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        draw({ rows: withFormula })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('12'))
        // The editor the cell is open in stays among the others, and cannot be picked again.
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        const formula = await screen.findByText('browser.module.editor_kind_formula')
        expect(formula.closest('[role="menuitem"]')).not.toHaveAttribute('aria-disabled', 'true')
        const excel = screen.getByText('browser.module.editor_kind_excelFormula')
        expect(excel.closest('[role="menuitem"]')).toHaveAttribute('aria-disabled', 'true')
        await userEvent.keyboard('{Escape}')

        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '=6*3{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, formula: '=6*3' } },
        ], 'Claims', undefined))
    })

    it('writes a number typed over an Excel formula as a value, as Excel does', async () => {
        const withFormula: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 12, formula: '=6*2' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        draw({ rows: withFormula })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('12'))
        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '42{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '42' } },
        ], 'Claims', undefined))
    })

    it('writes the text of an Excel formula as a value when the reader switches the cell to the text editor', async () => {
        const withFormula: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Integer hour)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 12, formula: '=6*2' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        draw({ rows: withFormula })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('12'))
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_kind_text'))
        // The same text, written as a value now, replaces the Excel formula.
        await userEvent.type(screen.getByTestId('table-cell-input'), '{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '=6*2' } },
        ], 'Claims', undefined))
    })

    it('opens a yes-or-no cell ticked on any of the words OpenL reads as true', async () => {
        const said: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Boolean on)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 'yes' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'boolean' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw({ rows: said })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('yes'))

        // 'yes' is the word the workbook was written with, and the Editor read it as ticked.
        expect(screen.getByTestId('table-cell-input')).toBeChecked()
    })

    it('writes a yes-or-no cell as the word OpenL writes, whatever word it held', async () => {
        const said: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Boolean on)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: 'yes' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'boolean' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw({ rows: said })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('yes'))
        await userEvent.click(screen.getByTestId('table-cell-input'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: 'false' } },
        ], 'Claims', undefined))
    })

    it('takes the whole list of choices at once and says so, as the Editor did', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'multiselect', choices: ['a', 'b'], displayValues: ['Alpha', 'Beta'], separator: ',' }],
            cells: [{ row: 1, column: 1, editor: 0 }],
        })
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-cell-input'))
        await userEvent.click(await screen.findByText('browser.module.edit_select_all'))

        // Everything is chosen now, so the same button offers to let it all go again.
        expect(await screen.findByText('browser.module.edit_deselect_all')).toBeInTheDocument()

        await userEvent.click(screen.getByText('browser.module.edit_done'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: 'a,b' } },
        ], 'Claims', undefined))
    })

    it('takes no date typed into the field, and empties it on Backspace', async () => {
        const dated: RawTableCell[][] = [
            [{ cell: 'B4', value: 'Rules String Greeting(Date on)', colspan: 2 }, { covered: true }],
            [{ cell: 'B5', value: '2024-03-07' }, { cell: 'C5', value: 'Good Morning' }],
        ]
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'date' }],
            cells: [{ row: 1, column: 0, editor: 0 }],
        })
        draw({ rows: dated })
        await userEvent.dblClick(screen.getByText('2024-03-07'))
        const field = await screen.findByTestId('table-cell-input')

        await userEvent.type(field, '12/25/2024')
        expect(field).toHaveValue('03/07/2024') // the date is the calendar's to give

        await userEvent.type(field, '{Backspace}')
        expect(field).toHaveValue('')
    })

    it('paints the picked cell while the pointer rests on a colour, and puts it back', async () => {
        drawFormatting()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.click(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))
        const swatch = (await screen.findAllByTestId('table-edit-swatch'))[1] as HTMLElement

        /** The cell the colour is meant for, read afresh: the screen draws it again on every change. */
        const painted = () => (screen.getByText('Good Morning').closest('td') as HTMLElement).style.background

        await userEvent.hover(swatch)
        expect(painted()).not.toBe('')

        await userEvent.unhover(swatch)
        expect(painted()).toBe('')
    })

    it('sends a theme colour as the theme colour, and shows it in the colours of Office until it is saved',
        async () => {
            drawFormatting()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

            await userEvent.click(screen.getByText('Good Morning'))
            await userEvent.click(screen.getByTestId('table-edit-fill_colour'))
            // The first shade under Text 1: Text 1, Lighter 50%.
            await userEvent.click((await screen.findAllByTestId('table-edit-swatch'))[11] as HTMLElement)

            expect(hex((screen.getByText('Good Morning').closest('td') as HTMLElement).style.backgroundColor))
                .toBe('#808080')
            await userEvent.click(screen.getByTestId('table-edit-save'))

            await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [{
                operation: 'style',
                target: { type: 'cells', row: 1, column: 1, rowspan: 1, colspan: 1,
                    style: { background: { theme: 'dk1', tint: 0.5 } } },
            }], 'Claims', undefined))
        })

    it('offers the look of a cell only with tables not formatted on save and the Excel formatting shown', async () => {
        const offered = async () => {
            await userEvent.click(screen.getByText('Good Morning'))
            return ['bold', 'fill_colour', 'indent']
                .filter(action => screen.queryByTestId(`table-edit-${action}`) !== null)
        }

        drawFormatting()
        expect(await offered()).toEqual(['bold', 'fill_colour', 'indent'])
        cleanup()

        // The Studio look does not show the formatting of the workbook.
        drawFormatting({}, false)
        expect(await offered()).toEqual([])
        cleanup()

        // Tables are formatted on save, as they are until the settings say otherwise.
        draw()
        expect(await offered()).toEqual([])
        cleanup()

        // A TBasic table reads the levels of its steps from the indent, so the indent stays.
        draw({ kind: 'TBasic' })
        expect(await offered()).toEqual(['indent'])
    })

    it('writes several numbers into an array cell, and lets nothing else in', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
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
        ], 'Claims', undefined))
    })

    /** The value of the cell that is open for writing, which says where the keyboard landed. */
    const openValue = () => screen.getByTestId<HTMLInputElement>('table-cell-input').value

    it.each([
        ['moves between cells with the arrows, and opens one with Enter', '0', '{ArrowRight}{Enter}', 'Good Morning'],
        // The header spans both columns, so moving up from either of them lands on the header itself.
        ['lands on the cell that owns the place a move reached',
            'Good Morning', '{ArrowUp}{Enter}', 'Rules String Greeting(Integer hour)'],
        // Up from the second column lands on the header, which spans both; coming back must return to the
        // cell that was left, not to the first column the header begins in.
        ['turns back to the cell it came from rather than to the neighbour',
            'Good Morning', '{ArrowUp}{ArrowDown}{Enter}', 'Good Morning'],
        ['opens a cell on the character the reader types, and takes it as the value', 'Good Morning', 'N', 'N'],
    ])('%s', async (_behaviour, from, keys, opened) => {
        draw({ editing: false })
        await waitFor(() => expect(screen.getByText('Good Morning')).toBeInTheDocument())

        await userEvent.click(screen.getByText(from))
        await userEvent.keyboard(keys)

        expect(openValue()).toBe(opened)
    })

    it('puts the caret at either end of what the cell holds', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        const input = screen.getByTestId<HTMLInputElement>('table-cell-input')
        await userEvent.keyboard('{F2}')
        expect(input.selectionStart).toBe(0)

        await userEvent.keyboard('{F3}')
        expect(input.selectionStart).toBe('Good Morning'.length)
    })

    it('gives a one-line field the room for several, and keeps what is written there with Ctrl+Enter', async () => {
        const { onSaved } = draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        await userEvent.keyboard('{Alt>}{Enter}{/Alt}')

        // Enter writes a line of its own once there is room for several, so Ctrl+Enter is what keeps the value.
        const field = screen.getByTestId('table-cell-input')
        await userEvent.clear(field)
        await userEvent.type(field, 'first{Enter}second')
        await userEvent.keyboard('{Control>}{Enter}{/Control}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: 'first\nsecond' } },
        ], 'Claims', undefined))
        expect(onSaved).toHaveBeenCalled()
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
        ], 'Claims', undefined)
        await waitFor(() => expect(onSaved).toHaveBeenCalledWith('table-1'))
    })

    it('names the version of the table it read, so a save over a table changed since is refused', async () => {
        draw({ version: 'v1' })

        await write('Good Morning', 'Buenos Dias')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith(
            'repo:Rating', 'table-1', expect.any(Array), 'Claims', 'v1'))
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

    it('opens a cell on its field alone, with nothing dropped under it unasked', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'combo', choices: ['R1', 'R2'], displayValues: ['Rating 1', 'Rating 2']}],
            cells: [{ row: 1, column: 1, editor: 0 }],
        })
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('Good Morning'))

        // A reader who opened the cell to read it, or to move on from it, has no panel to dismiss.
        expect(screen.getByTestId('table-cell-input')).toBeInTheDocument()
        expect(screen.queryByText('Rating 1')).toBeNull()
    })

    it('drops no panel under a range cell until the reader goes to its field', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        expect(screen.queryByTestId('range-editor')).toBeNull()

        await userEvent.click(screen.getByTestId('table-cell-input'))

        expect(await screen.findByTestId('range-editor')).toBeInTheDocument()
    })

    it('opens the panel of a range cell from the keyboard, since its field cannot be typed into', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        expect(screen.queryByTestId('range-editor')).toBeNull()

        // Another key leaves the panel where it was; only the arrow opens it.
        await userEvent.keyboard('{ArrowUp}')
        expect(screen.queryByTestId('range-editor')).toBeNull()

        await userEvent.keyboard('{ArrowDown}')

        expect(await screen.findByTestId('range-editor')).toBeInTheDocument()
    })

    it('offers a choice standing for none of them, which is how a cell is emptied', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'combo', choices: ['R1', 'R2'], displayValues: ['Rating 1', 'Rating 2']}],
            cells: [{ row: 1, column: 1, editor: 0 }],
        })
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('Good Morning'))
        await userEvent.click(screen.getByTestId('table-cell-input'))
        await screen.findByText('Rating 1')

        // The list heads with the empty choice, so nothing has to be crossed out over the arrow beside it.
        const options = document.querySelectorAll('.ant-select-item-option')
        expect(options).toHaveLength(3)
        expect(options[0]?.textContent?.trim()).toBe('')
    })

    it('offers the values a cell is chosen from, as the table said when editing started', async () => {
        vi.mocked(getTableEditors).mockResolvedValue({
            ...NO_EDITORS,
            editors: [{ editor: 'combo', choices: ['R1', 'R2'], displayValues: ['Rating 1', 'Rating 2']}],
            cells: [{ row: 1, column: 1, editor: 0 }],
        })
        draw()

        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))
        await userEvent.dblClick(screen.getByText('Good Morning'))
        // The list is the field's to drop when the reader goes to it, not the cell's to open unasked.
        await userEvent.click(screen.getByTestId('table-cell-input'))

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
        ...NO_EDITORS,
        editors: [{ editor: 'range', entryEditor: 'double' }],
        cells: [{ row: 1, column: 0, editor: 0 }],
    })

    /** A Smart Lookup condition in a header row, marked with the compiler's usage tooltip. */
    const horizontalRange = (linked = false, over: Partial<Parameters<typeof TableEditor>[0]> = {}) => {
        vi.mocked(getTableEditors).mockResolvedValue({
            kind: 'declared',
            editors: [{ editor: 'range', entryEditor: 'integer' }],
            cells: [],
            areas: [{ row: 1, column: 1, rows: 1, columns: null, editor: 0 }],
        })
        return draw({ rows: [
            [{ cell: 'C3', value: 'SmartLookup Integer quote(Integer units)', colspan: 2 }, { covered: true }],
            [{ cell: 'C4', value: 'units' }, { cell: 'D4', value: '1…50', metaInfo: {
                type: 'IntRange', editor: 'range',
                usages: [{ start: 0, end: 4, description: 'Condition: HC1', kind: 'other',
                    ...(linked ? { tableId: 'condition', module: 'Ranges' } : {}) }],
            } }],
        ], ...over })
    }

    it('opens the range editor for a horizontal condition whose usage spans its text on one line', async () => {
        await withMeasuredText([10, 10], async () => {
            horizontalRange()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

            await userEvent.dblClick(screen.getByTestId('cell-usage-0'))
            const input = screen.getByTestId('table-cell-input')
            expect(input.tagName).not.toBe('TEXTAREA')
            await userEvent.click(input)

            expect(await screen.findByTestId('range-editor')).toBeInTheDocument()
        })
    })

    it('opens an actually wrapped horizontal range over several lines', async () => {
        await withMeasuredText([10, 30], async () => {
            horizontalRange()
            await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

            await userEvent.dblClick(screen.getByTestId('cell-usage-0'))

            expect(screen.getByTestId('table-cell-input').tagName).toBe('TEXTAREA')
        })
    })

    it('detects wrapped text inside a linked usage when editing starts', async () => {
        await withMeasuredText([10, 30], async () => {
            horizontalRange(true, { editing: false, onOpenUsage: vi.fn() })
            const usage = screen.getByTestId('cell-usage-0')
            expect(usage.tagName).toBe('BUTTON')

            await userEvent.dblClick(usage)

            expect(screen.getByTestId('table-cell-input').tagName).toBe('TEXTAREA')
        }, [
            { top: 10, height: 40 },
            { top: 10, height: 16 },
            { top: 30, height: 16 },
        ])
    })

    it('enters a range in the panel under the cell, in the wording the Editor wrote', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        // The panel is the field's to drop when the reader goes to it, not the cell's to open unasked.
        await userEvent.click(screen.getByTestId('table-cell-input'))
        await userEvent.click(await screen.findByTestId('range-shape-between'))
        const to = screen.getByTestId('range-to')
        await userEvent.clear(to)
        await userEvent.type(to, '200')
        await userEvent.click(screen.getByTestId('range-write'))
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '0 .. 200' } },
        ], 'Claims', undefined))
    })

    it('closes the range panel when the reader clicks away from it', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        await userEvent.click(screen.getByTestId('table-cell-input'))
        expect(await screen.findByTestId('range-editor')).toBeInTheDocument()

        // A click inside the panel leaves it standing; one outside closes it, writing nothing.
        fireEvent.mouseDown(screen.getByTestId('range-from'))
        expect(screen.getByTestId('range-editor')).toBeInTheDocument()
        fireEvent.mouseDown(document.body)

        await waitFor(() => expect(screen.queryByTestId('range-editor')).toBeNull())
        expect(applyTableActions).not.toHaveBeenCalled()
    })

    it('closes a range cell the reader leaves without ever asking for its panel', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        expect(screen.getByTestId('table-cell-input')).toBeInTheDocument()

        // The field takes no typing and has no panel to close, so the click that leaves it closes the cell.
        // A cell left open holds on to the keys the table moves between cells with.
        await userEvent.click(cellOf(1, 1))

        await waitFor(() => expect(screen.queryByTestId('table-cell-input')).toBeNull())
        expect(applyTableActions).not.toHaveBeenCalled()
    })

    it('lets the reader write a range cell as text instead', async () => {
        rangeCell()
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalledTimes(1))

        await userEvent.dblClick(screen.getByText('0'))
        await userEvent.click(screen.getByTestId('table-cell-input'))
        expect(await screen.findByTestId('range-editor')).toBeInTheDocument()
        // The way out of the panel is the same one every other cell has.
        // Another way of writing the value is asked for with the right button, as the Editor asked for it.
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        await userEvent.click(await screen.findByText('browser.module.editor_kind_text'))

        expect(screen.queryByTestId('range-editor')).toBeNull()
        const input = screen.getByTestId('table-cell-input')
        await userEvent.clear(input)
        await userEvent.type(input, '5+{Enter}')
        await userEvent.click(screen.getByTestId('table-edit-save'))

        await waitFor(() => expect(applyTableActions).toHaveBeenCalledWith('repo:Rating', 'table-1', [
            { operation: 'update', target: { type: 'cell', row: 1, column: 0, value: '5+' } },
        ], 'Claims', undefined))
    })

    it('writes a cell over several lines when the reader switches to it', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        // Another way of writing the value is asked for with the right button, as the Editor asked for it.
        fireEvent.contextMenu(screen.getByTestId('table-cell-switch'))
        // Picking the way of writing takes the pointer out of the field, which must not close the cell.
        await userEvent.click(await screen.findByText('browser.module.editor_kind_multiline'))

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

describe('TableEditor marks', () => {
    beforeEach(() => {
        vi.mocked(applyTableActions).mockClear()
        vi.mocked(applyTableActions).mockResolvedValue('table-1')
        vi.mocked(getTableEditors).mockResolvedValue(NO_EDITORS)
    })

    const MARKED: RawTableCell[][] = [
        ROWS[0] ?? [],
        [{ cell: 'B5', value: 0 }, {
            cell: 'C5',
            value: 'Good Morning',
            runs: [{ text: 'Good ' }, { text: 'Morning', style: { bold: true, color: '#FF0000' } }],
        }],
    ]

    /** The actions the editor saved the table with. */
    const saved = async () => {
        await userEvent.click(screen.getByTestId('table-edit-save'))
        await waitFor(() => expect(applyTableActions).toHaveBeenCalled())
        return vi.mocked(applyTableActions).mock.calls[0]?.[2]
    }

    it('marks the selected piece of a text and writes the text with its mark', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))
        const input = screen.getByTestId<HTMLInputElement>('table-cell-input')
        input.setSelectionRange(5, 12)
        // The marks are on the band of actions over the table, and pressing one leaves the cell open.
        await userEvent.click(screen.getByTestId('table-edit-mark_error'))

        const field = await screen.findByTestId('table-cell-marked-input')
        expect(field.querySelector('[data-mark="error"]')).toHaveTextContent('Morning')
        fireEvent.keyDown(field, { key: 'Enter' })

        expect(await saved()).toEqual([{
            operation: 'update',
            target: { type: 'cell', row: 1, column: 1, value: 'Good Morning', marks: [{ from: 5, to: 12, color: 'error' }]},
        }])
    })

    it('opens a marked text with its marks and takes them off', async () => {
        draw({ rows: MARKED })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Morning'))
        const field = screen.getByTestId('table-cell-marked-input')
        expect(field.querySelector('[data-mark="error"]')).toHaveTextContent('Morning')
        selectIn(field, { from: 0, to: 12 })
        fireEvent.click(screen.getByTestId('table-edit-mark_clear'))

        const input = await screen.findByTestId('table-cell-input')
        fireEvent.keyDown(input, { key: 'Enter' })

        expect(await saved()).toEqual([{
            operation: 'update',
            target: { type: 'cell', row: 1, column: 1, value: 'Good Morning', marks: []},
        }])
    })

    it('writes a text it holds no marks on without them, as plain text', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await write('Good Morning', 'Good Evening')

        expect(await saved()).toEqual([{
            operation: 'update',
            target: { type: 'cell', row: 1, column: 1, value: 'Good Evening' },
        }])
    })

    it('offers no marks in a workbook that cannot hold them', async () => {
        draw({ canMark: false })
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Good Morning'))

        expect(screen.getByTestId('table-cell-input')).toBeInTheDocument()
        expect(screen.queryByTestId('table-edit-mark_error')).not.toBeInTheDocument()
    })

    it('offers no marks for the header of the table', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.dblClick(screen.getByText('Rules String Greeting(Integer hour)'))

        expect(screen.getByTestId('table-cell-input')).toBeInTheDocument()
        expect(screen.getByTestId('table-edit-mark_error')).toBeDisabled()
    })

    it('offers the marks while a cell holding a text is open, and takes them back once it closes', async () => {
        draw()
        await waitFor(() => expect(getTableEditors).toHaveBeenCalled())

        await userEvent.click(screen.getByText('Good Morning'))
        expect(screen.getByTestId('table-edit-mark_error')).toBeDisabled()

        await userEvent.dblClick(screen.getByText('Good Morning'))
        expect(screen.getByTestId('table-edit-mark_error')).toBeEnabled()
        expect(screen.getByTestId('table-edit-mark_clear')).toBeEnabled()

        fireEvent.keyDown(screen.getByTestId('table-cell-input'), { key: 'Escape' })
        await waitFor(() => expect(screen.getByTestId('table-edit-mark_error')).toBeDisabled())
    })
})

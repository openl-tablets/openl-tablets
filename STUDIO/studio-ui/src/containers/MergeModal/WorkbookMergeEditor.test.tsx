import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getConflictWorkbook, mergeConflictWorkbook } from '../../services/compare'
import { openConflictCompareWindow } from '../projects/compare'
import { WorkbookMergeEditor } from './WorkbookMergeEditor'
import type { ConflictCell, ConflictTableMerge, ConflictTableVersion, ConflictWorkbook } from './types'

vi.mock('../../services/compare', () => ({
    getConflictWorkbook: vi.fn(),
    mergeConflictWorkbook: vi.fn(),
    PALETTE_CONFLICT: '/palette',
}))

vi.mock('../projects/compare', () => ({ openConflictCompareWindow: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, unknown>) =>
        options ? `${key} ${Object.values(options).join('/')}` : key
    return { useTranslation: () => ({ t }) }
})

/** A rates table of two rows, the rate of US as a version has it. */
const rates = (us: number, changedCells: string[] = [], conflictCells: string[] = []): ConflictTableVersion => ({
    range: 'A1:B2',
    cells: [
        [{ cell: 'A1', value: 'Rules rate' }, { cell: 'B1', value: null }],
        [{ cell: 'A2', value: 'US' }, { cell: 'B2', value: us }],
    ],
    rows: 2,
    changedCells,
    formattingChanged: false,
    conflictCells,
})

/** A tall table of rates, the rate of the tenth country set by a version and conflicting when asked. */
const tallRates = (rate: number, conflict: boolean): ConflictTableVersion => ({
    range: 'A1:B20',
    cells: Array.from({ length: 20 }, (_, row) => [
        { cell: `A${row + 1}`, value: `R${row + 1}` },
        { cell: `B${row + 1}`, value: row === 9 ? rate : row },
    ]),
    rows: 20,
    changedCells: conflict ? ['B10'] : [],
    formattingChanged: false,
    conflictCells: conflict ? ['B10'] : [],
})

const WORKBOOK: ConflictWorkbook = {
    sheets: [
        { name: 'Rates', status: 'theirs', ours: true, base: true, theirs: true, tables: []},
        {
            name: 'Rules',
            status: 'conflict',
            ours: true,
            base: true,
            theirs: true,
            tables: [
                {
                    id: 'Rules/A1:B2',
                    name: 'Rules rate',
                    reason: 'cells',
                    ours: rates(10, ['B2']),
                    base: rates(1),
                    theirs: rates(20, ['B2']),
                },
                {
                    id: 'Rules/A9:C12',
                    name: 'Data Driver drivers',
                    reason: 'removedAndChanged',
                    ours: rates(30, ['B2']),
                    base: rates(3),
                },
            ],
        },
        { name: 'Extra', status: 'conflict', reason: 'removedAndChanged', ours: true, base: true, theirs: false,
            tables: []},
    ],
    palette: false,
}

const FILE = new Blob(['merged'])

const renderEditor = (props: Partial<React.ComponentProps<typeof WorkbookMergeEditor>> = {}) => {
    const onApply = vi.fn()
    const onCancel = vi.fn()
    const { unmount } = render(
        <WorkbookMergeEditor
            filePath="Project/rules/Rates.xlsx"
            onApply={onApply}
            onCancel={onCancel}
            projectId="p1"
            {...props}
        />
    )
    return { onApply, onCancel, unmount }
}

/** The cells each column shows of a table, without the headings of the sheet: your version, the result and theirs. */
const columnsOf = (number: number) => within(screen.getByTestId(`workbook-merge-table-${number}`))
    .getAllByRole('table')
    .map(grid => [...grid.querySelectorAll('td[data-cell]')].map(cell => cell.textContent))

/** The headings of the sheet a grid is drawn under and beside. */
const headingsOf = (grid: HTMLElement) => [...grid.querySelectorAll('td[data-heading]')].map(cell => cell.textContent)

/** Goes to the next table of the sheet shown, by the arrow of the way through its tables. */
const next = async (testId = 'workbook-merge-steps') => userEvent.click(
    screen.getByTestId(testId).querySelector<HTMLElement>('.ant-pagination-next button')!)

describe('WorkbookMergeEditor', () => {
    beforeEach(() => {
        vi.mocked(getConflictWorkbook).mockResolvedValue(WORKBOOK)
        vi.mocked(mergeConflictWorkbook).mockResolvedValue(FILE)
    })

    it('opens on the first conflicting sheet, its tables one at a time, and applies the versions taken', async () => {
        const { onApply } = renderEditor({ versionLabels: { ours: 'master', theirs: 'feature' } })

        await screen.findByTestId('workbook-merge-table-1')
        expect(getConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx')
        expect(screen.getByText('master')).toBeInTheDocument()
        expect(screen.getByText('feature')).toBeInTheDocument()
        expect(screen.getByText('merge:workbook.table_reason.cells')).toBeInTheDocument()
        // Until the table is resolved the result shows the base version.
        expect(columnsOf(1)).toEqual([
            ['Rules rate', '', 'US', '10'],
            ['Rules rate', '', 'US', '1'],
            ['Rules rate', '', 'US', '20'],
        ])
        expect(screen.queryByTestId('workbook-merge-table-2')).toBeNull()
        expect(screen.getByTestId('workbook-merge-apply')).toBeDisabled()

        await userEvent.click(screen.getByTestId('workbook-merge-table-1-take-theirs'))
        expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'US', '20'])
        await next()
        await userEvent.click(await screen.findByTestId('workbook-merge-table-2-take-ours'))
        await userEvent.click(screen.getByRole('tab', { name: /Extra/ }))
        await userEvent.click(await screen.findByTestId('workbook-merge-sheet-discard'))

        expect(screen.getByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 3/3')
        await userEvent.click(screen.getByTestId('workbook-merge-apply'))

        const decisions = { 'Rules/A1:B2': 'theirs', 'Rules/A9:C12': 'ours', 'Extra': 'base' }
        await waitFor(() => expect(onApply).toHaveBeenCalledWith({
            file: FILE,
            decisions,
            cellDecisions: {},
            workbook: WORKBOOK,
        }))
        expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', decisions, {})
    })

    it('leaves a version without the table empty, and names taking it the removal of the table', async () => {
        renderEditor()

        await screen.findByTestId('workbook-merge-table-1')
        await next()
        const table = await screen.findByTestId('workbook-merge-table-2')
        // Your version and the result show the table; their version, which has none, shows nothing at all.
        expect(within(table).getAllByRole('table')).toHaveLength(2)
        expect(within(table).getByTestId('workbook-merge-table-2-take-ours')).toHaveTextContent('merge:editor.take_yours')
        expect(within(table).getByTestId('workbook-merge-table-2-take-theirs'))
            .toHaveTextContent('merge:workbook.remove_table')

        await userEvent.click(within(table).getByTestId('workbook-merge-table-2-take-theirs'))
        expect(within(table).getByText('merge:workbook.table_left_out')).toBeInTheDocument()
    })

    it('decides a sheet resolved only as a whole for the whole sheet, its removal named so', async () => {
        renderEditor()

        await userEvent.click(await screen.findByRole('tab', { name: /Extra/ }))
        const sheet = await screen.findByTestId('workbook-merge-sheet')
        expect(within(sheet).getByText('merge:workbook.sheet_reason.removedAndChanged')).toBeInTheDocument()
        expect(within(sheet).queryByRole('table')).toBeNull()
        expect(within(sheet).getByTestId('workbook-merge-sheet-take-ours')).toHaveTextContent('merge:editor.take_yours')
        expect(within(sheet).getByTestId('workbook-merge-sheet-take-theirs'))
            .toHaveTextContent('merge:workbook.remove_sheet')
        await userEvent.click(within(sheet).getByTestId('workbook-merge-sheet-take-theirs'))
        expect(within(sheet).getByText('merge:editor.status_theirs')).toBeInTheDocument()

        await userEvent.click(within(sheet).getByTestId('workbook-merge-sheet-compare'))
        expect(openConflictCompareWindow).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx')
    })

    it('goes through the tables of a sheet one at a time and keeps the table shown on each sheet', async () => {
        const tables = Array.from({ length: 7 }, (_, index) => ({
            id: `Rules/A${index * 4 + 1}:B${index * 4 + 2}`,
            name: `Rules rate${index + 1}`,
            reason: 'cells' as const,
            ours: rates(10, ['B2']),
            base: rates(1),
            theirs: rates(20, ['B2']),
        }))
        renderEditor({
            merged: {
                file: FILE,
                decisions: {},
                cellDecisions: {},
                workbook: { sheets: [{ ...WORKBOOK.sheets[1]!, tables }, WORKBOOK.sheets[2]!], palette: false },
            },
        })

        expect(await screen.findByTestId('workbook-merge-table-1')).toHaveTextContent('Rules rate1')
        expect(screen.getByTestId('workbook-merge-steps')).toHaveTextContent('1/7')
        await next()
        expect(await screen.findByTestId('workbook-merge-table-2')).toHaveTextContent('Rules rate2')
        expect(screen.queryByTestId('workbook-merge-table-1')).toBeNull()

        await userEvent.click(screen.getByRole('tab', { name: /Extra/ }))
        await userEvent.click(screen.getByRole('tab', { name: /Rules/ }))
        expect(await screen.findByTestId('workbook-merge-table-2')).toHaveTextContent('Rules rate2')
        expect(screen.getByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 0/8')
    })

    it('repeats the way to the other tables under a table too long to scroll back from', async () => {
        const tall = {
            id: 'Rules/A1:B20',
            name: 'R1',
            reason: 'cells' as const,
            ours: tallRates(100, true),
            base: { ...tallRates(9, true), changedCells: []},
            theirs: tallRates(200, true),
        }
        renderEditor({
            merged: {
                file: FILE,
                decisions: {},
                cellDecisions: {},
                workbook: { sheets: [{ ...WORKBOOK.sheets[1]!, tables: [tall, WORKBOOK.sheets[1]!.tables[0]!]}],
                    palette: false },
            },
        })

        // The rows around the conflicting cell are few, so the way is drawn above the table alone.
        await screen.findByTestId('workbook-merge-table-1')
        expect(screen.queryByTestId('workbook-merge-steps-bottom')).toBeNull()
        await userEvent.click(screen.getByTestId('workbook-merge-table-1-whole'))
        await next('workbook-merge-steps-bottom')
        expect(await screen.findByTestId('workbook-merge-table-2')).toBeInTheDocument()
    })

    it('drops a merge still under way when the window closes', async () => {
        let answer: (file: Blob) => void = () => undefined
        vi.mocked(mergeConflictWorkbook).mockReturnValueOnce(new Promise(resolve => {
            answer = resolve
        }))
        const { onApply, unmount } = renderEditor({
            merged: {
                file: FILE,
                decisions: { 'Rules/A1:B2': 'base', 'Rules/A9:C12': 'ours', 'Extra': 'ours' },
                cellDecisions: {},
                workbook: WORKBOOK,
            },
        })

        await userEvent.click(await screen.findByTestId('workbook-merge-apply'))
        expect(mergeConflictWorkbook).toHaveBeenCalled()
        unmount()
        await act(async () => answer(FILE))

        expect(onApply).not.toHaveBeenCalled()
    })

    it('names the cells both branches changed differently and shows the rows around them', async () => {
        const table = {
            id: 'Rules/A1:B20',
            name: 'R1',
            reason: 'cells' as const,
            ours: tallRates(100, true),
            base: { ...tallRates(9, true), changedCells: []},
            theirs: tallRates(200, true),
        }
        renderEditor({
            merged: {
                file: FILE,
                decisions: {},
                cellDecisions: {},
                workbook: { sheets: [{ ...WORKBOOK.sheets[1]!, tables: [table]}], palette: false },
            },
        })

        const conflicts = await screen.findByTestId('workbook-merge-conflict-cells')
        expect(conflicts).toHaveTextContent('merge:workbook.conflict_cell B10/100/200')
        expect(screen.getByTestId('workbook-merge-table-1-rows'))
            .toHaveTextContent('merge:workbook.rows_window 8/12/20')
        // Each version shows the rows 8 to 12 alone, numbered as on the sheet.
        expect(columnsOf(1)[0]).toEqual(['R8', '7', 'R9', '8', 'R10', '100', 'R11', '10', 'R12', '11'])
        const ours = within(screen.getByTestId('workbook-merge-table-1')).getAllByRole('table')[0]!
        expect(headingsOf(ours)).toEqual(['A', 'B', '8', '9', '10', '11', '12'])
        // A sheet of one table needs no way to other tables.
        expect(screen.queryByTestId('workbook-merge-steps')).toBeNull()

        await userEvent.click(screen.getByTestId('workbook-merge-table-1-whole'))
        expect(columnsOf(1)[0]).toHaveLength(40)
        expect(screen.getByTestId('workbook-merge-table-1-rows')).toHaveTextContent('merge:workbook.rows_all')
    })

    it('frames a table whose formatting a branch changed, tells when both branches did, and keys the frames', async () => {
        const table = WORKBOOK.sheets[1]!.tables[0]!
        vi.mocked(getConflictWorkbook).mockResolvedValue({
            sheets: [{
                ...WORKBOOK.sheets[1]!,
                tables: [{
                    ...table,
                    ours: { ...table.ours!, formattingChanged: true },
                    theirs: { ...table.theirs!, formattingChanged: true },
                }],
            }],
            palette: false,
        })
        renderEditor()

        const first = await screen.findByTestId('workbook-merge-table-1')
        expect(within(first).getAllByTestId('workbook-merge-formatting-changed')).toHaveLength(2)
        expect(within(first).getByTestId('workbook-merge-table-1-formatting'))
            .toHaveTextContent('merge:workbook.formatting_both')
        // What the frames mean is told behind the icon of the title, each frame shown as the tables show it.
        expect(screen.queryByTestId('workbook-merge-legend')).toBeNull()
        await userEvent.click(screen.getByTestId('workbook-merge-legend-open'))
        const legend = await screen.findByTestId('workbook-merge-legend')
        expect(legend).toHaveTextContent('merge:workbook.legend_restyled')
        expect(legend).toHaveTextContent('merge:workbook.legend_merged')
        expect(legend.querySelectorAll('span[aria-hidden="true"]')).toHaveLength(6)
    })

    it('shows no sheet the merge takes by itself', async () => {
        renderEditor()

        await screen.findByTestId('workbook-merge-table-1')
        expect(screen.getAllByRole('tab').map(tab => tab.textContent)).toEqual([
            expect.stringMatching(/^Rules/),
            expect.stringMatching(/^Extra/),
        ])
    })

    it('shows why the merge refused the decisions and lets them be changed', async () => {
        vi.mocked(mergeConflictWorkbook).mockRejectedValueOnce(new Error('The tables would touch each other.'))
        const { onApply } = renderEditor()

        await userEvent.click(await screen.findByTestId('workbook-merge-table-1-take-ours'))
        await next()
        await userEvent.click(await screen.findByTestId('workbook-merge-table-2-take-ours'))
        await userEvent.click(screen.getByRole('tab', { name: /Extra/ }))
        await userEvent.click(await screen.findByTestId('workbook-merge-sheet-take-ours'))
        await userEvent.click(screen.getByTestId('workbook-merge-apply'))

        expect(await screen.findByTestId('workbook-merge-apply-error'))
            .toHaveTextContent('The tables would touch each other.')
        expect(onApply).not.toHaveBeenCalled()
        await userEvent.click(screen.getByTestId('workbook-merge-sheet-take-theirs'))
        expect(screen.queryByTestId('workbook-merge-apply-error')).toBeNull()
    })

    it('opens on the decisions made before without reading the workbook again', async () => {
        renderEditor({
            merged: {
                file: FILE,
                decisions: { 'Rules/A1:B2': 'ours', 'Rules/A9:C12': 'theirs', 'Extra': 'ours' },
                cellDecisions: {},
                workbook: WORKBOOK,
            },
        })

        expect(await screen.findByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 3/3')
        expect(getConflictWorkbook).not.toHaveBeenCalled()
        expect(screen.getByTestId('workbook-merge-apply')).toBeEnabled()
        expect(screen.getByTestId('workbook-merge-table-1-take-ours')).toBeDisabled()
    })

    it('resolves the palette of an .xls workbook with the version taken', async () => {
        const { onApply } = renderEditor({
            merged: {
                file: FILE,
                decisions: {},
                cellDecisions: {},
                workbook: { sheets: [], palette: true },
            },
        })

        const palette = await screen.findByTestId('workbook-merge-palette')
        await userEvent.click(within(palette).getByTestId('workbook-merge-palette-take-theirs'))
        await userEvent.click(screen.getByTestId('workbook-merge-apply'))

        await waitFor(() => expect(onApply).toHaveBeenCalled())
        expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', { '/palette': 'theirs' },
            {})
    })

    it('tells that the workbook could not be read', async () => {
        vi.mocked(getConflictWorkbook).mockRejectedValueOnce(new Error('No base version.'))
        renderEditor()

        expect(await screen.findByTestId('workbook-merge-error')).toHaveTextContent('No base version.')
    })

    describe('a table merged cell by cell', () => {
        /** The merged table of the rates, US renamed by your branch: its cells, and the conflicts given. */
        const mergedRates = (conflicts: ConflictTableMerge['conflicts'], rows = 2): ConflictTableMerge => ({
            range: 'A1:B2',
            cells: [
                [{ cell: 'A1', value: 'Rules rate' }, { cell: 'B1', value: null }],
                [{ cell: 'A2', value: 'USA' }, { cell: 'B2', value: 1 }],
            ].slice(0, rows),
            rows: 2,
            changedCells: ['A2'],
            conflicts,
        })
        /** A conflicting cell of the merged table, where every version has it. */
        const conflictAt = (
            row: number,
            column: number,
            cell: string,
            ours: number | string,
            theirs: number | string
        ): ConflictCell => ({
            row, column, cell, ourCell: cell, theirCell: cell, ours: { cell, value: ours }, theirs: { cell, value: theirs },
        })
        const RATE = conflictAt(1, 1, 'B2', 10, 20)
        const workbookOf = (merged: ConflictTableMerge): ConflictWorkbook => ({
            sheets: [{
                name: 'Rules',
                status: 'conflict',
                ours: true,
                base: true,
                theirs: true,
                tables: [{
                    id: 'Rules/A1:B2',
                    name: 'Rules rate',
                    reason: 'cells',
                    ours: rates(10, ['A2', 'B2'], ['B2']),
                    base: rates(1, [], ['B2']),
                    theirs: rates(20, ['B2'], ['B2']),
                    merged,
                }],
            }],
            palette: false,
        })
        /** Your branch renamed US and set its rate to 10, their branch set it to 20: the rate is the one conflict. */
        const BY_CELLS = workbookOf(mergedRates([RATE]))

        /** The grid of a version of the first table: your version, the result or their version. */
        const gridOf = (column: number) => within(screen.getByTestId('workbook-merge-table-1')).getAllByRole('table')[column]!

        /** The grid of the result of the first table. */
        const resultGrid = () => within(screen.getByTestId('workbook-merge-table-1-merged')).getByRole('table')

        /** A cell of the result by what it shows, not a heading of the sheet. */
        const cellOf = (text: string, grid = resultGrid()) => within(grid).getAllByText(text)
            .find(element => element.hasAttribute('data-cell'))!

        /** Picks a cell of the result by what it shows. */
        const pick = async (text: string) => userEvent.click(cellOf(text))

        /** Picks a row or a column of the result by its heading. */
        const pickHeading = async (label: string) => userEvent.click(
            resultGrid().querySelector<HTMLElement>(`td[data-heading="${label}"]`)!)

        beforeEach(() => {
            vi.mocked(getConflictWorkbook).mockResolvedValue(BY_CELLS)
        })

        it('shows the merged table as the result and takes a conflicting cell by the arrow of a version', async () => {
            const { onApply } = renderEditor()

            await screen.findByTestId('workbook-merge-table-1-merged')
            // The cells only one branch changed are merged; the conflicting cell shows the base version.
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '1'])
            // Your new name of US is framed alike in your version and in the result, as merged by itself.
            expect(cellOf('USA').className).toBe(cellOf('US', gridOf(0)).className)
            expect(cellOf('Rules rate').className).not.toBe(cellOf('USA').className)
            expect(screen.getByTestId('workbook-merge-table-1')).toHaveTextContent('merge:workbook.conflicts_left 1')
            expect(screen.getByText('merge:workbook.edit_hint')).toBeInTheDocument()
            // One conflicting cell needs no choice for all of them, and the reason the arrows tell is left out.
            expect(screen.queryByTestId('workbook-merge-table-1-all-take-ours')).toBeNull()
            expect(screen.queryByText('merge:workbook.table_reason.cells')).toBeNull()
            expect(screen.getByTestId('workbook-merge-apply')).toBeDisabled()

            await userEvent.click(screen.getByTestId('workbook-merge-table-1-B2-take-theirs'))

            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '20'])
            // The version chosen is framed as the result is and keeps no arrow; the other one keeps its arrow.
            expect(screen.queryByTestId('workbook-merge-table-1-B2-take-theirs')).toBeNull()
            expect(screen.getByTestId('workbook-merge-table-1-B2-take-ours')).toBeInTheDocument()
            expect(cellOf('20', gridOf(2)).className).toBe(cellOf('20').className)
            expect(cellOf('10', gridOf(0)).className).not.toBe(cellOf('20').className)
            expect(screen.getByTestId('workbook-merge-table-1')).toHaveTextContent('merge:workbook.status_by_cells')
            await userEvent.click(screen.getByTestId('workbook-merge-apply'))

            await waitFor(() => expect(onApply).toHaveBeenCalledWith({
                file: FILE,
                decisions: {},
                cellDecisions: { 'Rules/A1:B2': { cells: { '1:1': 'theirs' }, values: {} } },
                workbook: BY_CELLS,
            }))
            expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', {}, {
                'Rules/A1:B2': { cells: [{ row: 1, column: 1, side: 'theirs' }]},
            })
        })

        it('offers the versions of a conflicting cell clicked in the result beside it', async () => {
            renderEditor()

            await screen.findByTestId('workbook-merge-table-1-merged')
            await pick('1')
            // A conflicting cell offers its two versions alone, and opens no field to type into.
            await screen.findByTestId('workbook-merge-table-1-cell')
            expect(within(resultGrid()).queryByTestId('table-cell-input')).toBeNull()
            await userEvent.click(screen.getByTestId('workbook-merge-table-1-cell-ours'))

            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '10'])
            expect(screen.queryByTestId('workbook-merge-table-1-cell')).toBeNull()
        })

        it('takes a version for every conflicting cell of the table over what each of them took', async () => {
            const name = conflictAt(1, 0, 'A2', 'USA', 'United States')
            vi.mocked(getConflictWorkbook).mockResolvedValue(workbookOf(mergedRates([name, RATE])))
            renderEditor()

            await userEvent.click(await screen.findByTestId('workbook-merge-table-1-B2-take-theirs'))
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '20'])
            await userEvent.click(screen.getByTestId('workbook-merge-table-1-all-take-ours'))
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '10'])
            expect(screen.getByTestId('workbook-merge-table-1-all-take-ours')).toBeDisabled()
            await userEvent.click(screen.getByTestId('workbook-merge-apply'))

            expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', {}, {
                'Rules/A1:B2': { side: 'ours', cells: []},
            })
        })

        it('types a value on a double click into any cell, and gives the value up again', async () => {
            renderEditor()

            await screen.findByTestId('workbook-merge-table-1-merged')
            // A single click on a cell that is no conflict does nothing.
            await pick('USA')
            expect(within(resultGrid()).queryByTestId('table-cell-input')).toBeNull()
            await userEvent.dblClick(cellOf('USA'))
            const value = within(resultGrid()).getByTestId('table-cell-input')
            expect(value).toHaveValue('USA')
            await userEvent.clear(value)
            await userEvent.type(value, 'United States{Enter}')
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'United States', '1'])

            await userEvent.dblClick(cellOf('1'))
            await userEvent.type(within(resultGrid()).getByTestId('table-cell-input'), '5{Enter}')
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'United States', '15'])
            // A value typed for a conflicting cell is framed in a colour of its own, and leaves both arrows.
            expect(cellOf('15').className).not.toBe(cellOf('10', gridOf(0)).className)
            expect(screen.getByTestId('workbook-merge-table-1-B2-take-ours')).toBeInTheDocument()
            expect(screen.getByTestId('workbook-merge-table-1-B2-take-theirs')).toBeInTheDocument()
            expect(screen.getByTestId('workbook-merge-apply')).toBeEnabled()

            await userEvent.dblClick(cellOf('15'))
            await userEvent.click(screen.getByTestId('workbook-merge-untype'))
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'United States', '1'])
            expect(within(resultGrid()).queryByTestId('table-cell-input')).toBeNull()
            expect(screen.getByTestId('workbook-merge-apply')).toBeDisabled()
        })

        it('leaves a cell as it was when it is closed unchanged or with Escape', async () => {
            renderEditor()

            await screen.findByTestId('workbook-merge-table-1-merged')
            await userEvent.dblClick(cellOf('USA'))
            await userEvent.type(within(resultGrid()).getByTestId('table-cell-input'), '{Enter}')
            await userEvent.dblClick(cellOf('USA'))
            await userEvent.type(within(resultGrid()).getByTestId('table-cell-input'), 'X{Escape}')

            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '1'])
            expect(screen.getByTestId('workbook-merge-apply')).toBeDisabled()
        })

        it('names a conflicting cell past the rows shown and offers its versions by its name', async () => {
            vi.mocked(getConflictWorkbook).mockResolvedValue(workbookOf(mergedRates([RATE], 1)))
            renderEditor()

            await userEvent.click(within(await screen.findByTestId('workbook-merge-table-1-conflict-cells'))
                .getByText('B2'))
            await userEvent.click(await screen.findByTestId('workbook-merge-table-1-B2-ours'))

            expect(screen.getByTestId('workbook-merge-table-1-conflict-cells'))
                .toHaveTextContent('merge:editor.status_yours')
        })

        it('names every conflicting cell past the rows shown, and none of the cells in sight', async () => {
            const far = Array.from({ length: 6 }, (_, place) => conflictAt(place + 5, 1, `B${place + 6}`, 3, 4))
            vi.mocked(getConflictWorkbook).mockResolvedValue(workbookOf(mergedRates([RATE, ...far])))
            renderEditor()

            const named = await screen.findByTestId('workbook-merge-table-1-conflict-cells')
            expect(within(named).getAllByRole('button').map(button => button.textContent))
                .toEqual(['B6', 'B7', 'B8', 'B9', 'B10', 'B11'])
            expect(named).not.toHaveTextContent('merge:workbook.more_conflicts')
        })

        it('frames and takes a conflicting cell where a version has it, after rows its branch inserted', async () => {
            // Their branch inserted MX above US, so their rate of US is a row lower than in the merged table.
            const theirs: ConflictTableVersion = {
                ...rates(20, ['A2', 'B2', 'B3'], ['B3']),
                range: 'A1:B3',
                cells: [
                    [{ cell: 'A1', value: 'Rules rate' }, { cell: 'B1', value: null }],
                    [{ cell: 'A2', value: 'MX' }, { cell: 'B2', value: 3 }],
                    [{ cell: 'A3', value: 'US' }, { cell: 'B3', value: 20 }],
                ],
                rows: 3,
            }
            const workbook = workbookOf(mergedRates([{ ...RATE, theirCell: 'B3' }]))
            workbook.sheets[0]!.tables[0]!.theirs = theirs
            vi.mocked(getConflictWorkbook).mockResolvedValue(workbook)
            renderEditor()

            await screen.findByTestId('workbook-merge-table-1-merged')
            const arrow = screen.getByTestId('workbook-merge-table-1-B2-take-theirs')
            expect(cellOf('20', gridOf(2))).toContainElement(arrow)
            expect(cellOf('3', gridOf(2))).not.toContainElement(arrow)
            await userEvent.click(arrow)

            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '20'])
        })

        it('keeps the base value typed for a conflicting cell, which decides it', async () => {
            renderEditor()

            await screen.findByTestId('workbook-merge-table-1-merged')
            await userEvent.dblClick(cellOf('1'))
            await userEvent.type(within(resultGrid()).getByTestId('table-cell-input'), '{Enter}')

            expect(screen.getByTestId('workbook-merge-table-1')).toHaveTextContent('merge:workbook.status_by_cells')
            await userEvent.click(screen.getByTestId('workbook-merge-apply'))
            expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', {}, {
                'Rules/A1:B2': { cells: [{ row: 1, column: 1, value: '1' }]},
            })
        })

        it('takes the table whole from its menu, then merges it cell by cell again', async () => {
            renderEditor()

            await userEvent.click(await screen.findByTestId('workbook-merge-table-1-whole-table'))
            await userEvent.click(await screen.findByText('merge:editor.take_theirs'))
            expect(screen.queryByTestId('workbook-merge-table-1-merged')).toBeNull()
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'US', '20'])

            await userEvent.click(screen.getByTestId('workbook-merge-table-1-by-cells'))
            expect(screen.getByTestId('workbook-merge-table-1-merged')).toBeInTheDocument()
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '1'])
        })

        it('numbers the rows and letters the columns of the result, and decides a row or a column by its heading',
            async () => {
                renderEditor()

                await screen.findByTestId('workbook-merge-table-1-merged')
                expect(headingsOf(resultGrid())).toEqual(['A', 'B', '1', '2'])
                // A heading with no conflicting cell under it or beside it picks nothing.
                await pickHeading('A')
                expect(screen.queryByTestId('workbook-merge-table-1-column')).toBeNull()

                await pickHeading('2')
                await userEvent.click(await screen.findByTestId('workbook-merge-table-1-row-theirs'))
                expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '20'])
                expect(screen.queryByTestId('workbook-merge-table-1-row')).toBeNull()

                await pickHeading('B')
                expect(await screen.findByTestId('workbook-merge-table-1-column-theirs')).toHaveClass('ant-btn-primary')
                await userEvent.click(screen.getByTestId('workbook-merge-table-1-column-ours'))
                expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '10'])
            })

        it('opens on the choices made before', async () => {
            renderEditor({
                merged: {
                    file: FILE,
                    decisions: {},
                    cellDecisions: { 'Rules/A1:B2': { side: 'theirs', cells: {}, values: {} } },
                    workbook: BY_CELLS,
                },
            })

            expect(await screen.findByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 1/1')
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'USA', '20'])
        })
    })
})

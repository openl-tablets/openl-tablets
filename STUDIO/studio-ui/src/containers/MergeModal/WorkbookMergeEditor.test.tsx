import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getConflictWorkbook, mergeConflictWorkbook } from '../../services/compare'
import { openConflictCompareWindow } from '../projects/compare'
import { WorkbookMergeEditor } from './WorkbookMergeEditor'
import type { ConflictTableVersion, ConflictWorkbook } from './types'

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

/** The cells each column shows of a table: your version, the result and their version. */
const columnsOf = (number: number) => within(screen.getByTestId(`workbook-merge-table-${number}`))
    .getAllByRole('table')
    .map(grid => within(grid).getAllByRole('cell').map(cell => cell.textContent))

describe('WorkbookMergeEditor', () => {
    beforeEach(() => {
        vi.mocked(getConflictWorkbook).mockResolvedValue(WORKBOOK)
        vi.mocked(mergeConflictWorkbook).mockResolvedValue(FILE)
    })

    it('opens on the first conflicting sheet, with each table beside the result, and applies the versions taken',
        async () => {
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
            expect(screen.getByTestId('workbook-merge-apply')).toBeDisabled()

            await userEvent.click(screen.getByTestId('workbook-merge-table-1-take-theirs'))
            expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'US', '20'])
            await userEvent.click(screen.getByTestId('workbook-merge-table-2-take-ours'))
            await userEvent.click(screen.getByRole('tab', { name: /Extra/ }))
            await userEvent.click(await screen.findByTestId('workbook-merge-sheet-discard'))

            expect(screen.getByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 3/3')
            await userEvent.click(screen.getByTestId('workbook-merge-apply'))

            const decisions = { 'Rules/A1:B2': 'theirs', 'Rules/A9:C12': 'ours', 'Extra': 'base' }
            await waitFor(() => expect(onApply).toHaveBeenCalledWith({ file: FILE, decisions, workbook: WORKBOOK }))
            expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', decisions)
        })

    it('tells a table a version has not got, and a sheet taken from it', async () => {
        renderEditor()

        await screen.findByTestId('workbook-merge-table-2')
        expect(within(screen.getByTestId('workbook-merge-table-2')).getByText('merge:workbook.table_absent'))
            .toBeInTheDocument()
        await userEvent.click(screen.getByTestId('workbook-merge-table-2-take-theirs'))
        expect(within(screen.getByTestId('workbook-merge-table-2')).getByText('merge:workbook.table_left_out'))
            .toBeInTheDocument()

        await userEvent.click(screen.getByRole('tab', { name: /Extra/ }))
        const sheet = await screen.findByTestId('workbook-merge-sheet')
        expect(within(sheet).getByText('merge:workbook.sheet_reason.removedAndChanged')).toBeInTheDocument()
        expect(within(sheet).getAllByText('merge:workbook.sheet_present')).toHaveLength(2)
        await userEvent.click(within(sheet).getByTestId('workbook-merge-sheet-take-theirs'))
        expect(within(sheet).getAllByText('merge:workbook.sheet_absent')).toHaveLength(2)

        await userEvent.click(within(sheet).getByTestId('workbook-merge-sheet-compare'))
        expect(openConflictCompareWindow).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx')
    })

    it('takes a whole sheet at once, which resolves its tables and sends the sheet alone', async () => {
        const { onApply } = renderEditor()

        await screen.findByTestId('workbook-merge-table-1')
        await userEvent.click(screen.getByTestId('workbook-merge-table-1-take-ours'))
        await userEvent.click(within(screen.getByTestId('workbook-merge-sheet-choice'))
            .getByText('merge:editor.take_theirs'))

        // The buttons of the tables wait while the sheet is taken whole.
        expect(screen.getByTestId('workbook-merge-table-1-take-ours')).toBeDisabled()
        expect(columnsOf(1)[1]).toEqual(['Rules rate', '', 'US', '20'])
        expect(screen.getByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 2/3')
        await userEvent.click(screen.getByRole('tab', { name: /Extra/ }))
        await userEvent.click(await screen.findByTestId('workbook-merge-sheet-take-ours'))
        await userEvent.click(screen.getByTestId('workbook-merge-apply'))

        // The merge takes the sheet alone, and the decision made for its table is kept for Edit.
        expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx',
            { Rules: 'theirs', Extra: 'ours' })
        await waitFor(() => expect(onApply).toHaveBeenCalledWith({
            file: FILE,
            decisions: { 'Rules/A1:B2': 'ours', 'Rules': 'theirs', 'Extra': 'ours' },
            workbook: WORKBOOK,
        }))
    })

    it('shows again the decisions a whole sheet stood for once its tables are resolved one by one', async () => {
        renderEditor({
            merged: {
                file: FILE,
                decisions: { 'Rules/A1:B2': 'ours', 'Rules/A9:C12': 'theirs', 'Rules': 'theirs', 'Extra': 'ours' },
                workbook: WORKBOOK,
            },
        })

        await userEvent.click(within(await screen.findByTestId('workbook-merge-sheet-choice'))
            .getByText('merge:workbook.table_by_table'))

        expect(screen.getByTestId('workbook-merge-table-1-take-ours')).toBeDisabled()
        expect(screen.getByTestId('workbook-merge-table-1-take-theirs')).toBeEnabled()
        expect(screen.getByTestId('workbook-merge-table-2-take-theirs')).toBeDisabled()
        expect(screen.getByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 3/3')
    })

    it('drops a merge still under way when the window closes', async () => {
        let answer: (file: Blob) => void = () => undefined
        vi.mocked(mergeConflictWorkbook).mockReturnValueOnce(new Promise(resolve => {
            answer = resolve
        }))
        const { onApply, unmount } = renderEditor({
            merged: { file: FILE, decisions: { Rules: 'base', Extra: 'ours' }, workbook: WORKBOOK },
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
                workbook: { sheets: [{ ...WORKBOOK.sheets[1]!, tables: [table]}], palette: false },
            },
        })

        const conflicts = await screen.findByTestId('workbook-merge-conflict-cells')
        expect(conflicts).toHaveTextContent('merge:workbook.conflict_cell B10/100/200')
        expect(screen.getByTestId('workbook-merge-table-1-rows'))
            .toHaveTextContent('merge:workbook.rows_window 8/12/20')
        // Each version shows the rows 8 to 12 alone.
        expect(columnsOf(1)[0]).toEqual(['R8', '7', 'R9', '8', 'R10', '100', 'R11', '10', 'R12', '11'])

        await userEvent.click(screen.getByTestId('workbook-merge-table-1-whole'))
        expect(columnsOf(1)[0]).toHaveLength(40)
        expect(screen.getByTestId('workbook-merge-table-1-rows')).toHaveTextContent('merge:workbook.rows_all')
    })

    it('shows the conflicting tables of a sheet a page at a time', async () => {
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
                workbook: { sheets: [{ ...WORKBOOK.sheets[1]!, tables }], palette: false },
            },
        })

        expect(await screen.findByTestId('workbook-merge-table-5')).toBeInTheDocument()
        expect(screen.queryByTestId('workbook-merge-table-6')).toBeNull()
        await userEvent.click(within(screen.getByTestId('workbook-merge-pages')).getByTitle('2'))

        expect(await screen.findByTestId('workbook-merge-table-7')).toHaveTextContent('Rules rate7')
        expect(screen.queryByTestId('workbook-merge-table-1')).toBeNull()
        expect(screen.getByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 0/7')
    })

    it('frames a table whose formatting a branch changed and tells when both branches did', async () => {
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
        expect(screen.getByTestId('workbook-merge-legend')).toHaveTextContent('merge:workbook.legend')
        expect(within(first).getAllByTestId('workbook-merge-formatting-changed')).toHaveLength(2)
        expect(within(first).getByTestId('workbook-merge-table-1-formatting'))
            .toHaveTextContent('merge:workbook.formatting_both')
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

        await screen.findByTestId('workbook-merge-table-1')
        await userEvent.click(within(screen.getByTestId('workbook-merge-sheet-choice'))
            .getByText('merge:editor.take_yours'))
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
        renderEditor({ merged: { file: FILE, decisions: { Rules: 'base', Extra: 'ours' }, workbook: WORKBOOK } })

        expect(await screen.findByTestId('workbook-merge-progress')).toHaveTextContent('merge:editor.progress 3/3')
        expect(getConflictWorkbook).not.toHaveBeenCalled()
        expect(screen.getByTestId('workbook-merge-apply')).toBeEnabled()
    })

    it('resolves the palette of an .xls workbook with the version taken', async () => {
        const { onApply } = renderEditor({
            merged: {
                file: FILE,
                decisions: {},
                workbook: { sheets: [], palette: true },
            },
        })

        const palette = await screen.findByTestId('workbook-merge-palette')
        await userEvent.click(within(palette).getByTestId('workbook-merge-palette-take-theirs'))
        await userEvent.click(screen.getByTestId('workbook-merge-apply'))

        await waitFor(() => expect(onApply).toHaveBeenCalled())
        expect(mergeConflictWorkbook).toHaveBeenCalledWith('p1', 'Project/rules/Rates.xlsx', { '/palette': 'theirs' })
    })

    it('tells that the workbook could not be read', async () => {
        vi.mocked(getConflictWorkbook).mockRejectedValueOnce(new Error('No base version.'))
        renderEditor()

        expect(await screen.findByTestId('workbook-merge-error')).toHaveTextContent('No base version.')
    })
})

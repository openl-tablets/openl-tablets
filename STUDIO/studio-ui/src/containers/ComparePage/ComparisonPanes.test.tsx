import { render, screen } from '@testing-library/react'
import { ComparisonPanes } from './ComparisonPanes'
import { paperToken } from 'styles/paper'
import { renderInTheme } from 'testing/theme'
import type { ComparisonSide, ComparisonTable } from 'types/compare'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const TABLE: ComparisonTable = {
    id: '0-0',
    name: 'Datatype Person',
    status: 'changed',
    first: {
        source: [
            [{ cell: 'A1', value: 'Datatype Person' }],
            [{ cell: 'A2', value: 'int' }],
            [{ cell: 'A3', value: 'name' }],
        ],
        changedCells: ['A2'],
    },
    second: {
        source: [
            [{ cell: 'A1', value: 'Datatype Person' }],
            [{ cell: 'A2', value: 'double' }],
            [{ cell: 'A3', value: 'name' }],
        ],
        changedCells: ['A2'],
    },
}

/** The table with a row the second file adds at its end, lined up as the comparison reports it. */
const ADDED_ROW: ComparisonTable = {
    ...TABLE,
    second: {
        source: [...TABLE.second!.source, [{ cell: 'A4', value: 'age' }]],
        changedCells: ['A2'],
    },
    rows: [
        { first: 0, second: 0, status: 'equal' },
        { first: 1, second: 1, status: 'changed' },
        { first: 2, second: 2, status: 'equal' },
        { second: 3, status: 'added' },
    ],
    columns: [{ first: 0, second: 0, status: 'changed' }],
}

describe('ComparisonPanes', () => {
    it('shows the table of each file, with only the rows that differ', () => {
        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={TABLE} />)

        const first = screen.getByTestId('compare-pane-first')
        expect(first).toHaveTextContent('int')
        expect(first).not.toHaveTextContent('name')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('double')
    })

    it('frames a changed cell in the colours of the paper, whatever the theme', () => {
        renderInTheme(
            <ComparisonPanes error={null} loading={false} showEqualRows={false} table={TABLE} />,
            { theme: 'nord', mode: 'dark' }
        )

        const changed = screen.getByTestId('compare-pane-first').querySelector('[data-cell="A2"]') as HTMLElement
        expect(getComputedStyle(changed).boxShadow).toContain(paperToken().colorWarning)
        expect(changed).toHaveStyle({ color: paperToken().colorText })
    })

    it('keeps the colours the workbook gives the cells, the changed ones included', () => {
        const coloured: ComparisonTable = {
            ...TABLE,
            first: {
                source: [
                    [{ cell: 'A1', value: 'Datatype Person', style: { background: '#ffff00' } }],
                    [{ cell: 'A2', value: 'int', style: { background: '#ffff00' } }],
                ],
                changedCells: ['A2'],
            },
        }

        render(<ComparisonPanes showEqualRows error={null} loading={false} table={coloured} />)

        const cells = screen.getByTestId('compare-pane-first').querySelectorAll('td')
        // A changed cell is framed rather than filled, so nothing of the workbook is drawn over.
        expect((cells[0] as HTMLElement).style.background).toContain('rgb(255, 255, 0)')
        expect((cells[1] as HTMLElement).style.background).toContain('rgb(255, 255, 0)')
    })

    it('keeps the colours of a table that differs in nothing', () => {
        const equal: ComparisonTable = {
            ...TABLE,
            first: { source: [[{ cell: 'A1', value: 'Datatype Person', style: { background: '#ffff00' } }]]},
            second: { source: [[{ cell: 'A1', value: 'Datatype Person', style: { background: '#ffff00' } }]]},
        }

        render(<ComparisonPanes showEqualRows error={null} loading={false} table={equal} />)

        const cell = screen.getByTestId('compare-pane-first').querySelector('td') as HTMLElement
        expect(cell.style.background).toContain('rgb(255, 255, 0)')
    })

    it('keeps a merge whole when the rows it spans are kept apart', () => {
        const merged: ComparisonTable = {
            ...TABLE,
            first: {
                source: [
                    [{ cell: 'A1', value: 'group', rowspan: 4 }, { cell: 'B1', value: 'one' }],
                    [{ covered: true }, { cell: 'B2', value: 'two' }],
                    [{ covered: true }, { cell: 'B3', value: 'three' }],
                    [{ covered: true }, { cell: 'B4', value: 'four' }],
                ],
                changedCells: ['B2', 'B4'],
            },
            second: {
                source: [
                    [{ cell: 'A1', value: 'group', rowspan: 4 }, { cell: 'B1', value: 'one' }],
                    [{ covered: true }, { cell: 'B2', value: 'TWO' }],
                    [{ covered: true }, { cell: 'B3', value: 'three' }],
                    [{ covered: true }, { cell: 'B4', value: 'FOUR' }],
                ],
                changedCells: ['B2', 'B4'],
            },
        }

        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={merged} />)

        // The merge reaches both rows that are kept, so the cells beside it stay in their column.
        const rows = screen.getByTestId('compare-pane-first').querySelectorAll('tr')
        expect(rows[0]!.querySelectorAll('td')).toHaveLength(2)
        expect(rows[0]!.querySelector('td')).toHaveAttribute('rowspan', '2')
        expect(rows[1]!.querySelectorAll('td')).toHaveLength(1)
        expect(rows[1]!.querySelector('td')).toHaveTextContent('four')
    })

    it('shows every row when the equal ones are asked for', () => {
        render(<ComparisonPanes showEqualRows error={null} loading={false} table={TABLE} />)

        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('name')
    })

    it('shows a table whole when no cell of it differs', () => {
        // A table that differs only in its size carries no changed cell at all.
        const properties: ComparisonTable = {
            ...TABLE,
            first: { source: TABLE.first!.source },
            second: { source: TABLE.second!.source },
        }

        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={properties} />)

        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('name')
    })

    it('keeps the same rows in both files when only one of them marks a change', () => {
        const oneSided: ComparisonTable = {
            ...TABLE,
            first: { source: TABLE.first!.source },
            second: { ...TABLE.second!, changedCells: ['A2']},
        }

        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={oneSided} />)

        // Without this the file that marks nothing would show every row against one row of the other.
        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('int')
        expect(screen.getByTestId('compare-pane-first')).not.toHaveTextContent('name')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('double')
        expect(screen.getByTestId('compare-pane-second')).not.toHaveTextContent('name')
    })

    it('stands a row only the second file holds against an empty row of the first', () => {
        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={ADDED_ROW} />)

        // The changed row and the added one are left, and they stand against each other in both files.
        const first = screen.getByTestId('compare-pane-first').querySelectorAll('tr')
        const second = screen.getByTestId('compare-pane-second').querySelectorAll('tr')
        expect(first).toHaveLength(2)
        expect(second).toHaveLength(2)
        expect(first[0]).toHaveTextContent('int')
        expect(first[1]).toHaveTextContent('')
        expect(second[1]).toHaveTextContent('age')
    })

    it('paints a row only one file holds in the colours of the paper', () => {
        renderInTheme(
            <ComparisonPanes showEqualRows error={null} loading={false} table={ADDED_ROW} />,
            { theme: 'nord', mode: 'dark' }
        )

        const added = screen.getByTestId('compare-pane-second').querySelector('[data-cell="A4"]')
        expect(added).toHaveStyle({ backgroundColor: paperToken().colorSuccessBg })
    })

    it('stands a column only the first file holds against an empty column of the second', () => {
        const wider: ComparisonTable = {
            ...TABLE,
            first: {
                source: [
                    [{ cell: 'A1', value: 'Datatype Person' }, { cell: 'B1', value: 'note' }],
                    [{ cell: 'A2', value: 'int' }, { cell: 'B2', value: 'kept' }],
                    [{ cell: 'A3', value: 'name' }, { cell: 'B3', value: 'kept' }],
                ],
                changedCells: ['A2'],
            },
            rows: [
                { first: 0, second: 0, status: 'equal' },
                { first: 1, second: 1, status: 'changed' },
                { first: 2, second: 2, status: 'equal' },
            ],
            columns: [{ first: 0, second: 0, status: 'changed' }, { first: 1, status: 'removed' }],
        }

        renderInTheme(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={wider} />)

        // A column runs through every row, so no row is left out.
        const second = screen.getByTestId('compare-pane-second').querySelectorAll('tr')
        expect(second).toHaveLength(3)
        expect(second[0]!.querySelectorAll('td')).toHaveLength(2)
        const removed = screen.getByTestId('compare-pane-first').querySelector('[data-cell="B1"]')
        expect(removed).toHaveStyle({ backgroundColor: paperToken().colorErrorBg })
    })

    it('keeps a merge whole over the empty row standing against a row the other file adds', () => {
        const merged: ComparisonTable = {
            ...TABLE,
            first: {
                source: [
                    [{ cell: 'A1', value: 'US', rowspan: 2 }, { cell: 'B1', value: 'East' }],
                    [{ covered: true }, { cell: 'B2', value: 'West' }],
                ],
            },
            second: {
                source: [
                    [{ cell: 'A1', value: 'US', rowspan: 3 }, { cell: 'B1', value: 'East' }],
                    [{ covered: true }, { cell: 'B2', value: 'South' }],
                    [{ covered: true }, { cell: 'B3', value: 'West' }],
                ],
            },
            rows: [
                { first: 0, second: 0, status: 'equal' },
                { second: 1, status: 'added' },
                { first: 1, second: 2, status: 'equal' },
            ],
            columns: [{ first: 0, second: 0, status: 'equal' }, { first: 1, second: 1, status: 'equal' }],
        }

        render(<ComparisonPanes showEqualRows error={null} loading={false} table={merged} />)

        const first = screen.getByTestId('compare-pane-first').querySelectorAll('tr')
        expect(first).toHaveLength(3)
        expect(first[0]!.querySelector('td')).toHaveAttribute('rowspan', '3')
        expect(first[2]).toHaveTextContent('West')
    })

    it('explains the colours of the table and says where a moved table comes from', () => {
        const moved: ComparisonTable = {
            ...ADDED_ROW,
            moved: true,
            formattingChanged: true,
            firstRange: 'A1:A3',
            secondRange: 'C5:C8',
        }

        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={moved} />)

        const legend = screen.getByTestId('compare-legend')
        expect(legend).toHaveTextContent('status_changed')
        expect(legend).toHaveTextContent('status_added')
        expect(legend).not.toHaveTextContent('status_removed')
        expect(screen.getByTestId('compare-moved')).toHaveTextContent('moved_places')
        expect(screen.getByTestId('compare-formatting-changed')).toHaveTextContent('formatting_changed')
    })

    it('explains no colour of a table that differs in nothing', () => {
        render(<ComparisonPanes showEqualRows error={null} loading={false} table={{ ...TABLE, status: 'equal' }} />)

        expect(screen.queryByTestId('compare-legend')).toBeNull()
    })

    it('cuts a merge down to the rows that are left', () => {
        const merged: ComparisonSide = {
            source: [
                [{ cell: 'A1', value: 'Rules', colspan: 2 }, { covered: true }],
                [{ cell: 'A2', value: 'Region', rowspan: 3 }, { cell: 'B2', value: 'one' }],
                [{ covered: true }, { cell: 'B3', value: 'two' }],
                [{ covered: true }, { cell: 'B4', value: 'three' }],
            ],
            changedCells: ['B4'],
        }
        const table: ComparisonTable = { ...TABLE, first: merged, second: merged }

        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={table} />)

        // The row that is left keeps the value of the merge it was covered by, and both columns are drawn.
        const cells = screen.getByTestId('compare-pane-first').querySelectorAll('td')
        expect([...cells].map(cell => cell.textContent)).toEqual(['Region', 'three'])
        expect(cells[0]).toHaveAttribute('rowspan', '1')
    })

    it('stands the second file on the left when asked to, the controls staying at the ends of the line', () => {
        render(
            <ComparisonPanes
                secondOnLeft
                error={null}
                leading={<span>leading</span>}
                loading={false}
                showEqualRows={false}
                table={TABLE}
                titles={{ first: 'Base version', second: 'Your version' }}
                trailing={<span>trailing</span>}
            />
        )

        const panes = screen.getAllByTestId(/^compare-pane-(first|second)$/)
        expect(panes.map(pane => pane.dataset['testid'])).toEqual(['compare-pane-second', 'compare-pane-first'])
        const [left, right] = panes.map(pane => pane.parentElement!.textContent)
        expect(left).toMatch(/^leadingYour version/)
        expect(right).toMatch(/^Base versiontrailing/)
        // The legend of the colours stands on a line of its own above both files, so their rows stay level.
        expect(screen.getByTestId('compare-notes')).toHaveTextContent('status_changed')
    })

    it('says which file does not hold the table', () => {
        // A table only the second file holds carries no first side at all.
        const { first: _absent, ...withoutFirst } = TABLE
        const added: ComparisonTable = { ...withoutFirst, status: 'added' }

        render(<ComparisonPanes showEqualRows error={null} loading={false} table={added} />)

        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('absent')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('double')
    })

    it('asks for an element to be picked while none is', () => {
        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={null} />)

        // Both files keep their heading and say for themselves that there is nothing to show yet.
        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('nothing_selected')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('nothing_selected')
    })

    it('says that the element could not be read', () => {
        render(<ComparisonPanes error="broken" loading={false} showEqualRows={false} table={TABLE} />)

        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('broken')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('broken')
    })

    describe('drawn as one table', () => {
        const combined = (table: ComparisonTable | null, showEqualRows = true) => render(
            <ComparisonPanes
                error={null}
                loading={false}
                showEqualRows={showEqualRows}
                table={table}
                view="combined"
            />
        )

        it('carries both values in a cell the two files read differently', () => {
            combined(TABLE)

            const pane = screen.getByTestId('compare-pane-combined')
            // What the first file had is shown before what the second one has in its place.
            expect(pane).toHaveTextContent('int → double')
            // The two versions share one table, so there is no second pane to read it against.
            expect(screen.queryByTestId('compare-pane-second')).toBeNull()
        })

        it('leads every row with the sign it is read by', () => {
            combined(TABLE)

            const rows = screen.getByTestId('compare-combined').querySelectorAll('tr')
            expect(rows).toHaveLength(3)
            expect(rows[0]!.querySelector('td')).toHaveTextContent('')
            expect(rows[1]!.querySelector('td')).toHaveTextContent('→')
        })

        it('reads a row changed and a row inserted under it in a merged group as one table', () => {
            const grouped: ComparisonTable = {
                ...TABLE,
                first: {
                    source: [
                        [{ cell: 'A1', value: 'SmartRules Collect TableHeader[] headers', colspan: 2 }, { covered: true }],
                        [{ cell: 'A2', value: 'Table ID' }, { cell: 'B2', value: 'Id' }],
                        [{ cell: 'A3', value: 'CostSummaryTable', rowspan: 2 }, { cell: 'B3', value: 'TotalMonthlyBenefit' }],
                        [{ covered: true }, { cell: 'B4', value: 'CoverageRate' }],
                        [{ cell: 'A5', value: 'ExpensesTable' }, { cell: 'B5', value: 'Tax' }],
                    ],
                    changedCells: ['B4'],
                },
                second: {
                    source: [
                        [{ cell: 'A1', value: 'SmartRules Collect TableHeader[] headers', colspan: 2 }, { covered: true }],
                        [{ cell: 'A2', value: 'Table ID' }, { cell: 'B2', value: 'Id' }],
                        [{ cell: 'A3', value: 'CostSummaryTable', rowspan: 3 }, { cell: 'B3', value: 'TotalMonthlyBenefit' }],
                        [{ covered: true }, { cell: 'B4', value: 'ManualCoverageRate' }],
                        [{ covered: true }, { cell: 'B5', value: 'PayableCoverageRate' }],
                        [{ cell: 'A6', value: 'ExpensesTable' }, { cell: 'B6', value: 'Tax' }],
                    ],
                    changedCells: ['B4'],
                },
                rows: [
                    { first: 0, second: 0, status: 'equal' },
                    { first: 1, second: 1, status: 'equal' },
                    { first: 2, second: 2, status: 'equal' },
                    { first: 3, second: 3, status: 'changed' },
                    { second: 4, status: 'added' },
                    { first: 4, second: 5, status: 'equal' },
                ],
                columns: [{ first: 0, second: 0, status: 'equal' }, { first: 1, second: 1, status: 'changed' }],
            }

            combined(grouped)

            const table = screen.getByTestId('compare-combined')
            const leads = [...table.querySelectorAll('tr')].map(row => row.querySelector('td')?.textContent)
            // Neither version is removed and added whole: one row changed, one added, the rest the same.
            expect(leads).toEqual(['', '', '', '→', '+', ''])
            expect(table).toHaveTextContent('CoverageRate → ManualCoverageRate')
            // The id of the group stays one merged cell over its rows, the added one included.
            expect(table.querySelector('[data-cell="A3"]')).toHaveAttribute('rowspan', '3')
            expect(table.textContent?.match(/ExpensesTable/g)).toHaveLength(1)
        })

        it('shows only the rows that differ when the equal ones are not asked for', () => {
            combined(TABLE, false)

            const pane = screen.getByTestId('compare-pane-combined')
            expect(pane).toHaveTextContent('int → double')
            expect(pane).not.toHaveTextContent('name')
        })

        it('paints a row only one file holds in the colours of the paper, whatever the theme', () => {
            const { second: _absent, ...withoutSecond } = TABLE
            renderInTheme(
                <ComparisonPanes
                    showEqualRows
                    error={null}
                    loading={false}
                    table={{ ...withoutSecond, status: 'removed' }}
                    view="combined"
                />,
                { theme: 'nord', mode: 'dark' }
            )

            const dropped = screen.getByTestId('compare-combined').querySelector('[data-cell="A2"]')
            expect(dropped).toHaveStyle({ backgroundColor: paperToken().colorErrorBg })
        })

        it('reads a table only one file holds as one the other dropped', () => {
            const { second: _absent, ...withoutSecond } = TABLE
            const removed: ComparisonTable = { ...withoutSecond, status: 'removed' }

            combined(removed)

            const rows = screen.getByTestId('compare-combined').querySelectorAll('tr')
            expect(rows[0]!.querySelector('td')).toHaveTextContent('−')
            expect(screen.getByTestId('compare-pane-combined')).toHaveTextContent('int')
        })

        it('stands a row only the second file holds where it stands in that file', () => {
            combined(ADDED_ROW)

            const leads = [...screen.getByTestId('compare-combined').querySelectorAll('tr')]
                .map(row => row.querySelector('td')?.textContent)
            expect(leads).toEqual(['', '→', '', '+'])
            expect(screen.getByTestId('compare-pane-combined')).toHaveTextContent('age')
        })

        it('keeps what the workbook fills a changed row with', () => {
            const coloured: ComparisonTable = {
                ...TABLE,
                second: {
                    source: [
                        [{ cell: 'A1', value: 'Datatype Person', style: { background: '#ffff00' } }],
                        [{ cell: 'A2', value: 'double' }],
                        [{ cell: 'A3', value: 'name' }],
                    ],
                    changedCells: ['A2'],
                },
            }

            combined(coloured)

            const cells = screen.getByTestId('compare-combined').querySelectorAll('td')
            // The lead, then the cell of the row the workbook fills, not painted over.
            expect((cells[1] as HTMLElement).style.background).toContain('rgb(255, 255, 0)')
        })

        it('says what the signs leading the rows mean', () => {
            combined(TABLE)

            // Only the signs this table is actually read by are explained.
            const legend = screen.getByTestId('compare-legend')
            expect(legend).toHaveTextContent('→ status_changed')
            expect(legend).not.toHaveTextContent('status_added')
            // The sign in the table says the same to whoever hovers it; a row that did not change
            // carries no sign at all.
            const leads = [...screen.getByTestId('compare-combined').querySelectorAll('tr')]
                .map(row => row.querySelector('td')!.querySelector('span'))
            expect(leads[0]).toBeNull()
            expect(leads[1]).toHaveAttribute('title', 'status_changed')
        })

        it('explains the sign of a row only one file holds', () => {
            combined(ADDED_ROW)

            const legend = screen.getByTestId('compare-legend')
            expect(legend).toHaveTextContent('+ status_added')
            expect(legend).toHaveTextContent('→ status_changed')
            expect(legend).not.toHaveTextContent('status_removed')
        })

        it('shows a column only the first file holds beside the columns of the second', () => {
            const wider: ComparisonTable = {
                ...TABLE,
                first: {
                    source: [
                        [{ cell: 'A1', value: 'Datatype Person', colspan: 2 }, { covered: true }],
                        [{ cell: 'A2', value: 'int' }, { cell: 'B2', value: 'kept' }],
                        [{ cell: 'A3', value: 'name' }, { cell: 'B3', value: 'kept' }],
                    ],
                    changedCells: ['A2'],
                },
                columns: [{ first: 0, second: 0, status: 'changed' }, { first: 1, status: 'removed' }],
            }

            combined(wider)

            // The rows of both files stand against each other, and the column of the first file stands beside them.
            const rows = screen.getByTestId('compare-combined').querySelectorAll('tr')
            expect(rows).toHaveLength(3)
            expect(rows[1]).toHaveTextContent('int → double')
            expect(rows[1]).toHaveTextContent('kept')
            // A merge of the first file reaching a column of the second one is undone rather than drawn wrong.
            expect(rows[0]!.querySelectorAll('td')).toHaveLength(3)
        })

        it('says for itself that there is nothing to show yet', () => {
            combined(null)

            expect(screen.getByTestId('compare-pane-combined')).toHaveTextContent('nothing_selected')
        })
    })
})

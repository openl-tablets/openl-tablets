import { render, screen } from '@testing-library/react'
import { ComparisonPanes } from './ComparisonPanes'
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

describe('ComparisonPanes', () => {
    it('shows the table of each file, with only the rows that differ', () => {
        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={TABLE} />)

        const first = screen.getByTestId('compare-pane-first')
        expect(first).toHaveTextContent('int')
        expect(first).not.toHaveTextContent('name')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('double')
    })

    it('draws in grey the cells that do not differ', () => {
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
        // What does not differ steps back into grey, so that what does is what the eye lands on.
        expect((cells[0] as HTMLElement).style.background).toContain('rgb(136, 136, 136)')
        // The cell that differs is painted by the screen, so it carries no Excel background at all.
        expect((cells[1] as HTMLElement).style.background).toBe('')
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

    it('shows both files whole when one of them has more rows than the other', () => {
        const added: ComparisonTable = {
            ...TABLE,
            second: {
                source: [...TABLE.second!.source, [{ cell: 'A4', value: 'age' }]],
                changedCells: ['A4'],
            },
        }

        render(<ComparisonPanes error={null} loading={false} showEqualRows={false} table={added} />)

        // The rows no longer stand against each other, so neither file is cut down to a part of itself.
        expect(screen.getByTestId('compare-pane-first')).toHaveTextContent('name')
        expect(screen.getByTestId('compare-pane-second')).toHaveTextContent('age')
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

        it('shows only the rows that differ when the equal ones are not asked for', () => {
            combined(TABLE, false)

            const pane = screen.getByTestId('compare-pane-combined')
            expect(pane).toHaveTextContent('int → double')
            expect(pane).not.toHaveTextContent('name')
        })

        it('reads a table only one file holds as one the other dropped', () => {
            const { second: _absent, ...withoutSecond } = TABLE
            const removed: ComparisonTable = { ...withoutSecond, status: 'removed' }

            combined(removed)

            const rows = screen.getByTestId('compare-combined').querySelectorAll('tr')
            expect(rows[0]!.querySelector('td')).toHaveTextContent('−')
            expect(screen.getByTestId('compare-pane-combined')).toHaveTextContent('int')
        })

        it('reads two tables it cannot align as one dropped and one added', () => {
            const longer: ComparisonTable = {
                ...TABLE,
                second: { source: [...TABLE.second!.source, [{ cell: 'A4', value: 'age' }]]},
            }

            combined(longer)

            const leads = [...screen.getByTestId('compare-combined').querySelectorAll('tr')]
                .map(row => row.querySelector('td')?.textContent)
            expect(leads).toEqual(['−', '−', '−', '+', '+', '+', '+'])
        })

        it('keeps what the workbook fills a changed row with, muted like the rest', () => {
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
            // The lead, then the cell of the row the workbook fills: grey rather than painted over.
            expect((cells[1] as HTMLElement).style.background).toContain('rgb(136, 136, 136)')
        })

        it('says what the signs leading the rows mean', () => {
            combined(TABLE)

            // Only the signs this table is actually read by are explained.
            const legend = screen.getByTestId('compare-legend')
            expect(legend).toHaveTextContent('→status_changed')
            expect(legend).not.toHaveTextContent('status_added')
            // The sign in the table says the same to whoever hovers it; a row that did not change
            // carries no sign at all.
            const leads = [...screen.getByTestId('compare-combined').querySelectorAll('tr')]
                .map(row => row.querySelector('td')!.querySelector('span'))
            expect(leads[0]).toBeNull()
            expect(leads[1]).toHaveAttribute('title', 'status_changed')
        })

        it('explains both signs of two tables it cannot align', () => {
            const longer: ComparisonTable = {
                ...TABLE,
                second: { source: [...TABLE.second!.source, [{ cell: 'A4', value: 'age' }]]},
            }

            combined(longer)

            const legend = screen.getByTestId('compare-legend')
            expect(legend).toHaveTextContent('+status_added')
            expect(legend).toHaveTextContent('−status_removed')
            expect(legend).not.toHaveTextContent('status_changed')
        })

        it('reads two tables of different widths as one dropped and one added', () => {
            // As many rows, but a row of the first file holds a cell the second one has no place for.
            const wider: ComparisonTable = {
                ...TABLE,
                first: {
                    source: [
                        [{ cell: 'A1', value: 'Datatype Person' }, { cell: 'B1', value: 'note' }],
                        [{ cell: 'A2', value: 'int' }, { cell: 'B2', value: 'kept' }],
                        [{ cell: 'A3', value: 'name' }, { cell: 'B3', value: 'kept' }],
                    ],
                },
            }

            combined(wider)

            // Nothing is left out: what one file holds is shown whole, then what the other holds.
            const pane = screen.getByTestId('compare-pane-combined')
            expect(pane).toHaveTextContent('note')
            expect(screen.getByTestId('compare-combined').querySelectorAll('tr')).toHaveLength(6)
        })

        it('says for itself that there is nothing to show yet', () => {
            combined(null)

            expect(screen.getByTestId('compare-pane-combined')).toHaveTextContent('nothing_selected')
        })
    })
})

import { render, screen } from '@testing-library/react'
import { RawTableGrid } from 'components/RawTableGrid'
import type { RawTableCell } from 'types/tables'

const rows: RawTableCell[][] = [
    [
        { cell: 'A1', value: 'Datatype Person', colspan: 2 },
        { covered: true },
    ],
    [
        { cell: 'A2', value: 'String', style: { background: '#ffff00', bold: true } },
        { cell: 'B2', value: 'name' },
    ],
]

const computed: RawTableCell[][] = [[{ cell: 'A1', value: 3, formula: '=1+2' }, { cell: 'B1', value: 'plain' }]]

describe('RawTableGrid', () => {
    it('draws the cells with their merges, leaving the covered ones out', () => {
        render(<RawTableGrid rows={rows} testId="grid" />)

        const cells = screen.getByTestId('grid').querySelectorAll('td')
        expect(cells).toHaveLength(3)
        expect(cells[0]).toHaveAttribute('colspan', '2')
        expect(cells[0]).toHaveAttribute('data-cell', 'A1')
        expect(cells[0]).toHaveTextContent('Datatype Person')
        expect(cells[1]).toHaveTextContent('String')
    })

    it('paints the cell the way the workbook has it', () => {
        render(<RawTableGrid rows={rows} testId="grid" />)

        const styled = screen.getByTestId('grid').querySelectorAll('td')[1] as HTMLElement
        expect(styled.style.background).toContain('rgb(255, 255, 0)')
        expect(styled.style.fontWeight).toBe('bold')
    })

    it('leaves out the Excel background of a cell the screen paints itself', () => {
        render(
            <RawTableGrid
                decorate={cell => (cell.cell === 'A2' ? { className: 'marked', painted: true } : undefined)}
                rows={rows}
                testId="grid"
            />
        )

        const marked = screen.getByTestId('grid').querySelectorAll('td')[1] as HTMLElement
        expect(marked.className).toContain('marked')
        expect(marked.style.background).toBe('')
        // The rest of its Excel styling stays.
        expect(marked.style.fontWeight).toBe('bold')
    })

    it('draws a muted cell in grey, at the brightness of its own colour', () => {
        render(
            <RawTableGrid
                decorate={cell => (cell.cell === 'A2' ? { muted: true } : undefined)}
                rows={rows}
                testId="grid"
            />
        )

        // Yellow averages to 170, and four fifths of that is the grey it steps back to.
        const muted = screen.getByTestId('grid').querySelectorAll('td')[1] as HTMLElement
        expect(muted.style.background).toContain('rgb(136, 136, 136)')
        // The cell keeps everything the colour does not decide.
        expect(muted.style.fontWeight).toBe('bold')
    })

    it('leaves an unfilled cell unfilled when it is muted', () => {
        render(
            <RawTableGrid
                decorate={() => ({ muted: true })}
                rows={rows}
                testId="grid"
            />
        )

        const plain = screen.getByTestId('grid').querySelectorAll('td')[2] as HTMLElement
        expect(plain.style.background).toBe('')
    })

    it('draws an empty table without a row', () => {
        render(<RawTableGrid rows={[]} testId="grid" />)

        expect(screen.getByTestId('grid').querySelectorAll('td')).toHaveLength(0)
    })

    it('draws what a cell computed, and the formula behind it when the screen asks', () => {
        const { rerender } = render(<RawTableGrid rows={computed} testId="grid" />)
        expect(screen.getByTestId('grid').querySelectorAll('td')[0]).toHaveTextContent('3')

        rerender(<RawTableGrid formulas rows={computed} testId="grid" />)

        const cells = screen.getByTestId('grid').querySelectorAll('td')
        expect(cells[0]).toHaveTextContent('=1+2')
        // A cell written as a plain value has no formula to show, so it reads the same either way.
        expect(cells[1]).toHaveTextContent('plain')
    })

    it('leaves the formula unmarked: what the compiler knows describes the value, not the formula', () => {
        const marked: RawTableCell[][] = [[{
            cell: 'A1',
            value: 'Premium',
            formula: '=B1&C1',
            metaInfo: { usages: [{ start: 0, end: 7, description: 'Rules Double Premium()', kind: 'rule' }] },
        }]]

        const { rerender } = render(<RawTableGrid rows={marked} testId="grid" />)
        expect(screen.getByTestId('cell-usage-0')).toHaveTextContent('Premium')

        rerender(<RawTableGrid formulas rows={marked} testId="grid" />)

        // The ranges are measured over the value, so on the formula they would mark whatever happened to be
        // at those positions — and lead somewhere else entirely.
        expect(screen.queryByTestId('cell-usage-0')).toBeNull()
        expect(screen.getByTestId('grid').querySelectorAll('td')[0]).toHaveTextContent('=B1&C1')
    })
})

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

    it('draws an empty table without a row', () => {
        render(<RawTableGrid rows={[]} testId="grid" />)

        expect(screen.getByTestId('grid').querySelectorAll('td')).toHaveLength(0)
    })
})

import type { RawTableCell } from 'types/tables'
import { openlTableOf } from './openlTable'

/** The table as text: a value with its spans, `-` for a covered cell. */
const drawn = (rows: RawTableCell[][]) => rows.map(row => row.map(cell => {
    if (cell.covered) {
        return '-'
    }
    const spans = `${cell.colspan ? `|c${cell.colspan}` : ''}${cell.rowspan ? `|r${cell.rowspan}` : ''}`
    return `${cell.value}${spans}`
}))

describe('openlTableOf', () => {
    it('spans the header over the table, taken as written, and merges the cells the markers join', () => {
        const table = openlTableOf([
            'Rules String greeting(Integer hour, Boolean weekend)',
            'Rule,C1,C2,RET1',
            ',hour,weekend,greeting',
            '---',
            'R10,0-12,false,Good Morning',
            'R20,^,true,Lazy Morning',
            'R30,12-24',
            'Total,<,<,"<"',
        ].join('\n'))

        expect(drawn(table.rows)).toEqual([
            ['Rules String greeting(Integer hour, Boolean weekend)|c4', '-', '-', '-'],
            ['Rule', 'C1', 'C2', 'RET1'],
            ['', 'hour', 'weekend', 'greeting'],
            ['R10', '0-12|r2', 'false', 'Good Morning'],
            ['R20', '-', 'true', 'Lazy Morning'],
            ['R30', '12-24', '', ''],
            ['Total|c3', '-', '-', '<'],
        ])
        expect(table.headerRows).toBe(3)
    })

    it('merges an area spanning rows and columns', () => {
        expect(drawn(openlTableOf('\nHeader\na,<,b\n^,^,c\n').rows)).toEqual([
            ['Header|c3', '-', '-'],
            ['a|c2|r2', '-', 'b'],
            ['-', '-', 'c'],
        ])
    })

    it('shades only the table header without a --- line', () => {
        expect(openlTableOf('Header\nx,y').headerRows).toBe(1)
    })

    it('draws a marker that joins nothing as the text it is', () => {
        expect(drawn(openlTableOf('Header\n<,x').rows)).toEqual([['Header|c2', '-'], ['<', 'x']])
    })

    it('gives every cell the address Excel gives it', () => {
        const wide = openlTableOf(`Header\n${Array.from({ length: 28 }, (_, i) => `v${i}`).join(',')}`)

        expect(wide.rows[1]?.map(cell => cell.cell).slice(24)).toEqual(['Y2', 'Z2', 'AA2', 'AB2'])
    })

    it('draws nothing for an empty block', () => {
        expect(openlTableOf(' \n')).toEqual({ rows: [], headerRows: 0 })
    })
})

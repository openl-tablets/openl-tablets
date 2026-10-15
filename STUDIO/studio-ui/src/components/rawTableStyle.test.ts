import { collapsedBorders, wider } from './rawTableStyle'
import type { RawTableCell, RawTableCellBorderSide } from 'types/tables'

const THIN: RawTableCellBorderSide = { style: 'solid', width: 1 }
const RED: RawTableCellBorderSide = { color: '#ff0000', style: 'solid', width: 1 }
const MEDIUM: RawTableCellBorderSide = { style: 'solid', width: 2 }

describe('collapsedBorders', () => {
    it('draws the wider of two lines on one edge, the first where they are as wide', () => {
        expect(wider(THIN, MEDIUM)).toBe(MEDIUM)
        expect(wider(RED, THIN)).toBe(RED)
        expect(wider(undefined, THIN)).toBe(THIN)
        expect(wider(THIN, undefined)).toBe(THIN)
    })

    it('draws a line two cells share on the upper or the left cell', () => {
        const rows: RawTableCell[][] = [
            [{ cell: 'A1', style: { border: { top: THIN, bottom: THIN } } }, { cell: 'B1' }],
            [{ cell: 'A2', style: { border: { top: MEDIUM } } }, { cell: 'B2', style: { border: { left: RED } } }],
        ]

        const drawn = collapsedBorders(rows)

        expect(drawn[0]?.[0]).toEqual({ bottom: MEDIUM, top: THIN })
        expect(drawn[1]?.[0]).toEqual({ right: RED })
        expect(drawn[1]?.[1]).toBeUndefined()
        expect(drawn[0]?.[1]).toBeUndefined()
    })

    it('keeps the lines along the top and the left edges, and draws the lines of the cells under a merged cell on it', () => {
        const rows: RawTableCell[][] = [
            [{ cell: 'A1', colspan: 2, style: { border: { left: THIN } } }, { covered: true }],
            [{ cell: 'A2', style: { border: { top: THIN } } }, { cell: 'B2', style: { border: { top: MEDIUM } } }],
        ]

        const drawn = collapsedBorders(rows)

        expect(drawn[0]?.[0]).toEqual({ bottom: MEDIUM, left: THIN })
        expect(drawn[0]?.[1]).toBeUndefined()
        expect(drawn[1]?.[0]).toBeUndefined()
    })
})

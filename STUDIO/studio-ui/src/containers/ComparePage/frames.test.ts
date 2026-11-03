import { describe, expect, it } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { framesOf, type FrameSides } from './frames'

/** A grid of plain cells named by their text, `x` for a changed one. */
const gridOf = (...rows: string[]): RawTableCell[][] =>
    rows.map((row, r) => [...row].map((value, c) => ({ cell: `${'ABCDEF'[c]}${r + 1}`, value })))

const changedCells = (cell: RawTableCell) => cell.value === 'x'

/** The frame of the cell at a row and a column. */
const frameAt = (grid: RawTableCell[][], row: number, column: number): FrameSides | undefined =>
    framesOf(grid, changedCells).get(grid[row]![column]!)

const ALL: FrameSides = { top: true, right: true, bottom: true, left: true }

describe('framesOf', () => {
    it('frames a changed cell alone on every side', () => {
        const grid = gridOf('...', '.x.', '...')

        expect(frameAt(grid, 1, 1)).toEqual(ALL)
        expect(framesOf(grid, changedCells).size).toBe(1)
    })

    it('frames a changed row as a whole', () => {
        const grid = gridOf('...', 'xxx', '...')

        expect(frameAt(grid, 1, 0)).toEqual({ top: true, right: false, bottom: true, left: true })
        expect(frameAt(grid, 1, 1)).toEqual({ top: true, right: false, bottom: true, left: false })
        expect(frameAt(grid, 1, 2)).toEqual({ top: true, right: true, bottom: true, left: false })
    })

    it('frames a changed column as a whole', () => {
        const grid = gridOf('.x.', '.x.')

        expect(frameAt(grid, 0, 1)).toEqual({ top: true, right: true, bottom: false, left: true })
        expect(frameAt(grid, 1, 1)).toEqual({ top: false, right: true, bottom: true, left: true })
    })

    it('frames a table changed in every cell along its edges only', () => {
        const grid = gridOf('xx', 'xx')

        expect(frameAt(grid, 0, 0)).toEqual({ top: true, right: false, bottom: false, left: true })
        expect(frameAt(grid, 1, 1)).toEqual({ top: false, right: true, bottom: true, left: false })
    })

    it('frames a merged cell along a side unless every cell beyond it changed', () => {
        const grid: RawTableCell[][] = [
            [{ cell: 'A1', value: 'x', rowspan: 2 }, { cell: 'B1', value: 'x' }],
            [{ covered: true }, { cell: 'B2', value: '.' }],
        ]

        // Only one of the two cells beyond the merge changed, so the frame runs between them.
        expect(frameAt(grid, 0, 0)).toEqual(ALL)
        // The merge beside the cell changed, wherever its head stands.
        expect(frameAt(grid, 0, 1)).toEqual({ top: true, right: true, bottom: true, left: false })
    })

    it('frames a merged cell below changed cells it spans wholly as one with them', () => {
        const grid: RawTableCell[][] = [
            [{ cell: 'A1', value: 'x' }, { cell: 'B1', value: 'x' }],
            [{ cell: 'A2', value: 'x', colspan: 2 }, { covered: true }],
        ]

        expect(frameAt(grid, 1, 0)).toEqual({ top: false, right: true, bottom: true, left: true })
        expect(frameAt(grid, 0, 0)).toEqual({ top: true, right: false, bottom: false, left: true })
    })

    it('frames cells next to each other apart when they are framed in different ways', () => {
        const grid = gridOf('ab')
        const frames = framesOf(grid, cell => (cell.value === '.' ? undefined : cell.value))

        expect(frames.get(grid[0]![0]!)).toEqual(ALL)
        expect(frames.get(grid[0]![1]!)).toEqual(ALL)
    })

    it('leaves a cell framed as false unframed', () => {
        const grid = gridOf('x.')

        expect(framesOf(grid, cell => cell.value === 'x').size).toBe(1)
    })
})

import { describe, expect, it } from 'vitest'
import type { RawTableCell, TableEdit } from 'types/tables'
import { keyOf, NO_EDITS, redo, replay, undo, withEdit } from './tableEdits'

const write = (row: number, column: number, value: string): TableEdit =>
    ({ operation: 'update', target: { type: 'cell', row, column, value } })

const table: RawTableCell[][] = [
    [{ cell: 'A1', value: 'Rules' }, { cell: 'B1', value: 'Greeting' }],
    [{ cell: 'A2', value: 0 }, { cell: 'B2', value: 'Good Morning' }],
]

describe('tableEdits', () => {
    it('keeps the edits in the order they were made', () => {
        const buffer = withEdit(withEdit(NO_EDITS, write(1, 0, '6')), write(1, 1, 'Buenos Dias'))

        expect(buffer.edits.map(edit => edit.target.value)).toEqual(['6', 'Buenos Dias'])
    })

    it('replaces the last edit when the same cell is written again', () => {
        const buffer = withEdit(withEdit(NO_EDITS, write(1, 1, 'Buenos')), write(1, 1, 'Buenos Dias'))

        // The reader means the value they ended with, so the table is asked to write that cell once.
        expect(buffer.edits).toHaveLength(1)
        expect(buffer.edits[0]?.target.value).toBe('Buenos Dias')
    })

    it('takes an edit back and puts it again', () => {
        const written = withEdit(withEdit(NO_EDITS, write(1, 0, '6')), write(1, 1, 'Buenos Dias'))

        const back = undo(written)
        expect(back.edits).toHaveLength(1)
        expect(back.undone).toHaveLength(1)

        const again = redo(back)
        expect(again.edits.map(edit => edit.target.value)).toEqual(['6', 'Buenos Dias'])
        expect(again.undone).toHaveLength(0)
    })

    it('has nothing to take back or put again on an untouched table', () => {
        expect(undo(NO_EDITS)).toBe(NO_EDITS)
        expect(redo(NO_EDITS)).toBe(NO_EDITS)
    })

    it('drops what was undone as soon as something else is written', () => {
        const back = undo(withEdit(NO_EDITS, write(1, 0, '6')))

        expect(withEdit(back, write(1, 1, 'Buenos Dias')).undone).toHaveLength(0)
    })

    it('shows the table as the edits leave it and says which cells they touched', () => {
        const { rows, touched } = replay(table, [write(1, 1, 'Buenos Dias')])

        expect(rows[1]?.[1]?.value).toBe('Buenos Dias')
        // The cell keeps everything else it was read with, so the screen still draws it in its place.
        expect(rows[1]?.[1]?.cell).toBe('B2')
        expect(touched.has(keyOf({ row: 1, column: 1 }))).toBe(true)
        expect(touched.has(keyOf({ row: 1, column: 0 }))).toBe(false)
    })

    it('leaves the table that was read as it stands', () => {
        replay(table, [write(1, 1, 'Buenos Dias')])

        expect(table[1]?.[1]?.value).toBe('Good Morning')
    })

    it('forgets the formula of a cell that is written over', () => {
        const withFormula: RawTableCell[][] = [[{ cell: 'A1', value: 4, formula: '=2+2' }]]

        expect(replay(withFormula, [write(0, 0, '5')]).rows[0]?.[0]).toEqual({ cell: 'A1', value: '5' })
    })

    it('passes by an edit of a cell the table no longer has', () => {
        const { rows, touched } = replay(table, [write(9, 9, 'nowhere')])

        expect(rows).toEqual(table)
        expect(touched.size).toBe(0)
    })
})

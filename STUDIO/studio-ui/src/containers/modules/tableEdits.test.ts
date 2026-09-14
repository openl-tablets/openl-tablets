import { describe, expect, it } from 'vitest'
import type { RawTableCell } from 'types/tables'
import {
    blankLine,
    compile,
    type EditStep,
    keyOf,
    NO_EDITS,
    redo,
    replay,
    undo,
    withStep,
} from './tableEdits'

const write = (row: number, column: number, value: string): EditStep =>
    ({ kind: 'value', at: { row, column }, value })

const table: RawTableCell[][] = [
    [{ cell: 'A1', value: 'Rules' }, { cell: 'B1', value: 'Greeting' }],
    [{ cell: 'A2', value: 0 }, { cell: 'B2', value: 'Good Morning' }],
    [{ cell: 'A3', value: 12 }, { cell: 'B3', value: 'Good Afternoon' }],
]

/** The table as the given steps leave it. */
const after = (...steps: EditStep[]) => replay(table, steps)

/** What the given steps ask the table to be written with. */
const sent = (...steps: EditStep[]) => compile(table, after(...steps))

describe('tableEdits', () => {
    describe('what the reader did', () => {
        it('keeps the steps in the order they were made', () => {
            const buffer = withStep(withStep(NO_EDITS, write(1, 0, '6')), write(1, 1, 'Buenos Dias'))

            expect(buffer.steps).toHaveLength(2)
        })

        it('replaces the last step when the same cell is written again', () => {
            const buffer = withStep(withStep(NO_EDITS, write(1, 1, 'Buenos')), write(1, 1, 'Buenos Dias'))

            // The reader means the value they ended with, so the table is asked to write that cell once.
            expect(buffer.steps).toHaveLength(1)
        })

        it('takes a step back and puts it again', () => {
            const written = withStep(withStep(NO_EDITS, write(1, 0, '6')), write(1, 1, 'Buenos Dias'))

            const back = undo(written)
            expect(back.steps).toHaveLength(1)
            expect(redo(back).steps).toHaveLength(2)
        })

        it('has nothing to take back or put again on an untouched table', () => {
            expect(undo(NO_EDITS)).toBe(NO_EDITS)
            expect(redo(NO_EDITS)).toBe(NO_EDITS)
        })

        it('drops what was undone as soon as something else is done', () => {
            const back = undo(withStep(NO_EDITS, write(1, 0, '6')))

            expect(withStep(back, write(1, 1, 'Buenos Dias')).undone).toHaveLength(0)
        })
    })

    describe('the table as the steps leave it', () => {
        it('shows what was written and says which cells were touched', () => {
            const state = after(write(1, 1, 'Buenos Dias'))

            expect(state.rows[1]?.[1]?.value).toBe('Buenos Dias')
            // The cell keeps everything else it was read with, so the screen still draws it in its place.
            expect(state.rows[1]?.[1]?.cell).toBe('B2')
            expect(state.touched.has(keyOf(state, { row: 1, column: 1 }))).toBe(true)
        })

        it('leaves the table that was read as it stands', () => {
            after(write(1, 1, 'Buenos Dias'))

            expect(table[1]?.[1]?.value).toBe('Good Morning')
        })

        it('forgets the formula of a cell that is written over', () => {
            const withFormula: RawTableCell[][] = [[{ cell: 'A1', value: 4, formula: '=2+2' }]]

            expect(replay(withFormula, [write(0, 0, '5')]).rows[0]?.[0]).toEqual({ cell: 'A1', value: '5' })
        })

        it('pushes the rows below down when one is added', () => {
            const state = after({ kind: 'insertRow', at: 1 })

            expect(state.rows).toHaveLength(4)
            expect(state.rows[1]?.[0]?.value).toBe('')
            expect(state.rows[2]?.[0]?.value).toBe(0)
            // The added row is not one of the table's own, which is what tells the save to add it.
            expect(state.rowIds[1]?.startsWith('n')).toBe(true)
        })

        it('writes into the row that was added, not the one it pushed down', () => {
            const state = after({ kind: 'insertRow', at: 1 }, write(1, 0, '6'))

            expect(state.rows[1]?.[0]?.value).toBe('6')
            expect(state.rows[2]?.[0]?.value).toBe(0)
        })

        it('keeps the styling the reader asked for on the cell it was asked for', () => {
            const state = after({ kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } })

            expect(state.rows[1]?.[1]?.style?.bold).toBe(true)
        })

        it('carries the styling with the cell when a row is added above it', () => {
            const state = after(
                { kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } },
                { kind: 'insertRow', at: 1 }
            )

            expect(state.rows[2]?.[1]?.style?.bold).toBe(true)
            expect(state.rows[1]?.[1]?.style?.bold).toBeUndefined()
        })
    })

    describe('what is sent when the reader saves', () => {
        it('sends nothing at all for a table nobody changed', () => {
            expect(sent()).toEqual([])
        })

        it('sends nothing for a cell written back to what it held', () => {
            expect(sent(write(1, 1, 'Buenos Dias'), write(1, 1, 'Good Morning'))).toEqual([])
        })

        it('sends the cells that differ, addressed as they stand now', () => {
            expect(sent(write(1, 1, 'Buenos Dias'))).toEqual([
                { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: 'Buenos Dias' } },
            ])
        })

        it('sends an added row with the values it ended with', () => {
            expect(sent({ kind: 'insertRow', at: 1 }, write(1, 0, '6'), write(1, 1, 'Buenos Dias'))).toEqual([
                {
                    operation: 'insert',
                    target: {
                        type: 'rows',
                        position: 1,
                        cells: [[{ value: '6' }, { value: 'Buenos Dias' }]],
                    },
                },
            ])
        })

        it('takes rows away from the bottom up, so the ones before them do not move', () => {
            expect(sent({ kind: 'removeRow', at: 1 }, { kind: 'removeRow', at: 1 })).toEqual([
                { operation: 'delete', target: { type: 'rows', position: 2, count: 1 } },
                { operation: 'delete', target: { type: 'rows', position: 1, count: 1 } },
            ])
        })

        it('takes a column away before it addresses any row', () => {
            const edits = sent({ kind: 'removeColumn', at: 1 }, write(1, 0, '6'))

            expect(edits[0]).toEqual({ operation: 'delete', target: { type: 'columns', position: 1, count: 1 } })
            expect(edits[1]).toEqual({
                operation: 'update',
                target: { type: 'cell', row: 1, column: 0, value: '6' },
            })
        })

        it('sends an added column holding only the rows the table already had', () => {
            const edits = sent({ kind: 'insertColumn', at: 1 }, write(0, 1, 'Note'))

            expect(edits).toEqual([{
                operation: 'insert',
                target: {
                    type: 'columns',
                    position: 1,
                    cells: [[{ value: 'Note' }, { value: '' }, { value: '' }]],
                },
            }])
        })

        it('sends the styling the reader asked for, and only that', () => {
            expect(sent({ kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } })).toEqual([
                {
                    operation: 'style',
                    target: { type: 'cells', row: 1, column: 1, rowspan: 1, colspan: 1, style: { bold: true } },
                },
            ])
        })
    })

    describe('what the table cannot take', () => {
        it('refuses a row the reader added and left empty', () => {
            expect(blankLine(after({ kind: 'insertRow', at: 1 }))).toBe('row')
        })

        it('refuses a column the reader added and left empty', () => {
            expect(blankLine(after({ kind: 'insertColumn', at: 1 }))).toBe('column')
        })

        it('takes an added row once something is written into it', () => {
            expect(blankLine(after({ kind: 'insertRow', at: 1 }, write(1, 0, '6')))).toBeNull()
        })

        it('says nothing about a row the table was read with, however empty', () => {
            const empty: RawTableCell[][] = [[{ value: 'Rules' }], [{ value: '' }]]

            expect(blankLine(replay(empty, []))).toBeNull()
        })
    })
})

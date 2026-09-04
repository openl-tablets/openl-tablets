import { describe, expect, it } from 'vitest'
import {
    DiffInputTooLargeError,
    MAX_DIFF_LINES,
    MAX_DIFF_MATRIX_CELLS,
    diffLines,
} from './lineDiff'

describe('diffLines', () => {
    it('marks every line as context when the texts are identical', () => {
        const result = diffLines('a\nb\nc', 'a\nb\nc')
        expect(result.every(line => line.kind === 'context')).toBe(true)
        expect(result).toHaveLength(3)
    })

    it('detects an inserted line', () => {
        const result = diffLines('a\nc', 'a\nb\nc')
        expect(result.map(l => `${l.kind}:${l.text}`)).toEqual(['context:a', 'add:b', 'context:c'])
        expect(result[1]!.oldNumber).toBeNull()
        expect(result[1]!.newNumber).toBe(2)
    })

    it('detects a removed line', () => {
        const result = diffLines('a\nb\nc', 'a\nc')
        expect(result.map(l => `${l.kind}:${l.text}`)).toEqual(['context:a', 'remove:b', 'context:c'])
    })

    it('detects a changed line as a remove plus an add', () => {
        const result = diffLines('a\nB\nc', 'a\nb\nc')
        expect(result.map(l => l.kind)).toEqual(['context', 'remove', 'add', 'context'])
    })

    it('handles empty inputs', () => {
        expect(diffLines('', '')).toEqual([])
        expect(diffLines('', 'x').map(l => l.kind)).toEqual(['add'])
        expect(diffLines('x', '').map(l => l.kind)).toEqual(['remove'])
    })

    it('rejects inputs that would render too many lines', () => {
        const oldText = Array.from({ length: MAX_DIFF_LINES + 1 }, (_, index) => `old-${index}`).join('\n')

        expect(() => diffLines(oldText, '')).toThrow(DiffInputTooLargeError)
    })

    it('rejects inputs that would allocate a large LCS matrix', () => {
        const sideLength = Math.floor(Math.sqrt(MAX_DIFF_MATRIX_CELLS)) + 1
        const oldText = Array.from({ length: sideLength }, (_, index) => `old-${index}`).join('\n')
        const newText = Array.from({ length: sideLength }, (_, index) => `new-${index}`).join('\n')

        expect(() => diffLines(oldText, newText)).toThrow(DiffInputTooLargeError)
    })
})

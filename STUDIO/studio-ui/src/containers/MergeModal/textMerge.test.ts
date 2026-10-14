import {
    holdsSide,
    joinLines,
    lineBreakOf,
    lineCount,
    linesOf,
    mergedTextOf,
    shownLines,
    withoutLastLineBreak,
    takeSide,
    writtenLines,
} from './textMerge'
import type { ConflictedTextBlock, ConflictTextBlock } from './types'

const CONFLICT: ConflictedTextBlock = { kind: 'conflict', ours: 'b=ours\n', base: 'b=2\n', theirs: 'b=theirs\n' }

describe('textMerge', () => {
    it('writes the line breaks most lines of the file end with', () => {
        expect(lineBreakOf([{ kind: 'merged', text: 'a=1\n' }, CONFLICT])).toBe('\n')
        expect(lineBreakOf([{ kind: 'merged', text: 'a=1\r\nb=2\r\nc=3\r\nd=4\r\n' }, CONFLICT])).toBe('\r\n')
        expect(lineBreakOf([{ kind: 'conflict', ours: 'b\r\n', base: '', theirs: '' }])).toBe('\r\n')
        // A line of Windows among lines that end with a line feed alone leaves the file written as it is.
        expect(lineBreakOf([{ kind: 'merged', text: 'a=1\r\n' }, CONFLICT])).toBe('\n')
        // So do as many lines of each kind, and a file of a single line.
        expect(lineBreakOf([{ kind: 'merged', text: 'a=1\r\nb=2\n' }])).toBe('\n')
        expect(lineBreakOf([{ kind: 'merged', text: 'a=1' }])).toBe('\n')
    })

    it('ends lines with a line break only when more lines follow them', () => {
        expect(joinLines('a', 'b', '\n')).toBe('a\nb')
        expect(joinLines('a\n', 'b', '\n')).toBe('a\nb')
        expect(joinLines('a', '', '\n')).toBe('a')
        expect(joinLines('', 'b', '\r\n')).toBe('b')
    })

    it('takes the lines each choice names', () => {
        expect(linesOf(CONFLICT, 'ours', '\n')).toBe('b=ours\n')
        expect(linesOf(CONFLICT, 'theirs', '\n')).toBe('b=theirs\n')
        expect(linesOf(CONFLICT, 'ours-theirs', '\n')).toBe('b=ours\nb=theirs\n')
        expect(linesOf(CONFLICT, 'theirs-ours', '\n')).toBe('b=theirs\nb=ours\n')
        expect(linesOf(CONFLICT, 'base', '\n')).toBe('b=2\n')
        // The last line of a file may end without a line break; the lines put after it start a line of their own.
        expect(linesOf({ kind: 'conflict', ours: 'x', base: '', theirs: 'y' }, 'ours-theirs', '\n')).toBe('x\ny')
        // A version that holds no line gives none.
        expect(linesOf({ kind: 'conflict', ours: '', base: '', theirs: 'y\n' }, 'base', '\n')).toBe('')
    })

    it('takes a version alone, or after the other version taken alone', () => {
        expect(takeSide(undefined, 'ours')).toBe('ours')
        expect(takeSide('base', 'theirs')).toBe('theirs')
        expect(takeSide('edited', 'ours')).toBe('ours')
        expect(takeSide('ours', 'theirs')).toBe('ours-theirs')
        expect(takeSide('theirs', 'ours')).toBe('theirs-ours')
        // A version taken already, after the other one or not, is not taken again.
        expect(holdsSide('ours', 'ours')).toBe(true)
        expect(holdsSide('theirs-ours', 'ours')).toBe(true)
        expect(holdsSide('ours-theirs', 'theirs')).toBe(true)
        expect(holdsSide('theirs', 'ours')).toBe(false)
        expect(holdsSide('base', 'theirs')).toBe(false)
        expect(holdsSide(undefined, 'ours')).toBe(false)
    })

    it('counts the lines of a text, a last line without a line break among them', () => {
        expect(lineCount('')).toBe(0)
        expect(lineCount('a\n')).toBe(1)
        expect(lineCount('a\r\nb')).toBe(2)
        expect(lineCount('\n\n')).toBe(2)
    })

    it('edits lines without their last line break and writes them with the breaks of the file', () => {
        expect(shownLines('b=ours\r\n', CONFLICT)).toBe('b=ours')
        expect(withoutLastLineBreak('a\r\nb\r\n')).toBe('a\r\nb')
        // An editor writes the line breaks of Unix; the lines get those of the file back when written.
        expect(writtenLines('a\nb', CONFLICT, '\r\n')).toBe('a\r\nb\r\n')
        // A line the user adds at the end stays, though it is empty.
        expect(shownLines(writtenLines('a\n', CONFLICT, '\n'), CONFLICT)).toBe('a\n')
        // Nothing written deletes the lines.
        expect(writtenLines('', CONFLICT, '\n')).toBe('')
        // The last lines of a file that ends without a line break stay without one.
        const last: ConflictedTextBlock = { kind: 'conflict', ours: 'x', base: '', theirs: 'y' }
        expect(shownLines('x', last)).toBe('x')
        expect(writtenLines('a\nb', last, '\r\n')).toBe('a\r\nb')
    })

    it('puts the lines merged and the lines decided together in their order', () => {
        const blocks: ConflictTextBlock[] = [
            { kind: 'merged', text: 'a=1\n' },
            CONFLICT,
            { kind: 'merged', text: 'c=3\n' },
            { kind: 'conflict', ours: 'd=ours', base: 'd=4', theirs: 'd=theirs' },
        ]

        expect(mergedTextOf(blocks, [
            { choice: 'edited', lines: 'b=mine' },
            { choice: 'theirs', lines: 'd=theirs' },
        ], '\n')).toBe('a=1\nb=mine\nc=3\nd=theirs')
    })
})

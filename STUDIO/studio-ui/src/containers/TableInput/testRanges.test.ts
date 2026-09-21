import { testRangesOf } from './testRanges'

describe('testRangesOf', () => {
    it('names cases kept one after another by their ends, and a case on its own by its id', () => {
        const ids = ['1', '2', '3', '4', '5', '6', '7']
        const left = new Set(['3', '6'])

        expect(testRangesOf(ids, id => !left.has(id))).toBe('1 - 2,4 - 5,7')
    })

    it('names a long table with one case left out in two words', () => {
        const ids = Array.from({ length: 5000 }, (_, i) => String(i + 1))

        expect(testRangesOf(ids, id => id !== '5')).toBe('1 - 4,6 - 5000')
    })

    it('keeps an id with a dash of its own apart from the dash between the ends', () => {
        expect(testRangesOf(['id-1', 'id-2', 'id-3'], id => id !== 'id-2')).toBe('id-1,id-3')
        expect(testRangesOf(['id-1', 'id-2', 'id-3'], () => true)).toBe('id-1 - id-3')
    })

    it('names nothing when no case is kept', () => {
        expect(testRangesOf(['1', '2'], () => false)).toBe('')
    })
})

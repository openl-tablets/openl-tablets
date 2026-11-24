import { describe, expect, it } from 'vitest'
import { inTagTypeOrder, inTagTypeThenNameOrder, tagTypeRank } from './tagOrder'

describe('tagTypeRank', () => {
    it('ranks a tag type by its place among the configured ones, regardless of case', () => {
        const rank = tagTypeRank(['Region', 'Domain'])

        expect(rank('region')).toBe(0)
        expect(rank(' Domain ')).toBe(1)
        expect(rank('LOB')).toBe(2)
    })
})

describe('inTagTypeOrder', () => {
    it('puts the configured types first, the others after them in the order they came', () => {
        expect(inTagTypeOrder(['Domain', 'LOB'], ['Zone', 'lob', 'Area', 'Domain'], type => type))
            .toEqual(['Domain', 'lob', 'Zone', 'Area'])
    })
})

describe('inTagTypeThenNameOrder', () => {
    it('puts the configured types first, the others after them by name regardless of case', () => {
        expect(inTagTypeThenNameOrder(['Domain', 'LOB'], ['Zone', 'lob', 'area', 'Domain'], type => type))
            .toEqual(['Domain', 'lob', 'area', 'Zone'])
    })
})

import { describe, expect, it } from 'vitest'
import { inTagTypeOrder, inTagTypeThenNameOrder } from './tagOrder'

describe('inTagTypeOrder', () => {
    it('finds a configured tag type regardless of case and surrounding spaces', () => {
        expect(inTagTypeOrder(['Region', 'Domain'], ['LOB', ' Domain ', 'region'], type => type))
            .toEqual(['region', ' Domain ', 'LOB'])
    })

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

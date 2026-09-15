import { describe, expect, it } from 'vitest'
import { joinValues, splitValues } from './multiValue'

describe('multiValue', () => {
    it('tells the values apart by the separator the table writes them with', () => {
        expect(splitValues('AL,AK,AZ', ',')).toEqual(['AL', 'AK', 'AZ'])
        expect(splitValues('AL, AK', ',')).toEqual(['AL', 'AK'])
        expect(splitValues('', ',')).toEqual([])
    })

    it('keeps a separator that belongs to a value', () => {
        // The table writes such a separator after the escaper it declares, so it ends no value.
        expect(splitValues('Smith\\, John,AK', ',', '\\')).toEqual(['Smith, John', 'AK'])
        expect(splitValues('Smith\\, John', ',', '\\')).toEqual(['Smith, John'])
    })

    it('writes the chosen values back the way the cell holds them', () => {
        expect(joinValues(['AL', 'AK'], ',')).toBe('AL,AK')
        expect(joinValues(['Smith, John', 'AK'], ',', '\\')).toBe('Smith\\, John,AK')
    })

    it('reads back what it wrote', () => {
        const values = ['Smith, John', 'plain', 'a, b, c']
        expect(splitValues(joinValues(values, ',', '\\'), ',', '\\')).toEqual(values)
    })
})

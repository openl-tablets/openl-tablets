import { describe, expect, it } from 'vitest'
import { escapeRegExp } from './escapeRegExp'

describe('escapeRegExp', () => {
    it('makes a text with metacharacters match only itself', () => {
        const text = '/a.b+(c)/[d]$'

        expect(new RegExp(`^${escapeRegExp(text)}$`).test(text)).toBe(true)
        expect(new RegExp(escapeRegExp('a.b')).test('axb')).toBe(false)
    })
})

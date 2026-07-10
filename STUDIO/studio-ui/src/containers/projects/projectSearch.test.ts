import { describe, expect, it } from 'vitest'
import { parseProjectSearch } from './projectSearch'

describe('parseProjectSearch', () => {
    it('treats bare words as the name filter', () => {
        expect(parseProjectSearch('alpha beta')).toEqual({ name: 'alpha beta' })
    })

    it('returns an empty object for a blank query', () => {
        expect(parseProjectSearch('   ')).toEqual({})
    })

    it('parses author and branch qualifiers', () => {
        expect(parseProjectSearch('author:jane branch:main')).toEqual({ author: 'jane', branch: 'main' })
    })

    it('combines qualifiers with bare name text', () => {
        expect(parseProjectSearch('author:jane payroll branch:main rules')).toEqual({
            author: 'jane',
            branch: 'main',
            name: 'payroll rules',
        })
    })

    it('supports quoted values with spaces', () => {
        expect(parseProjectSearch('author:"John Doe" name:"My Project"')).toEqual({
            author: 'John Doe',
            name: 'My Project',
        })
    })

    it('keeps an unrecognized qualifier as plain name text', () => {
        expect(parseProjectSearch('foo:bar alpha')).toEqual({ name: 'foo:bar alpha' })
    })

    it('lets the last value of a repeated qualifier win', () => {
        expect(parseProjectSearch('author:jane author:john')).toEqual({ author: 'john' })
    })
})

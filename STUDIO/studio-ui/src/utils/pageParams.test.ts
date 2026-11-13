import { describe, expect, it } from 'vitest'
import { withPage } from './pageParams'

describe('withPage', () => {
    it('writes the page and the size beside the other parameters', () => {
        const next = withPage(new URLSearchParams('q=rating'), 3, 50)

        expect(next.toString()).toBe('q=rating&page=3&size=50')
    })

    it('leaves the first page and the default size out of the address', () => {
        const next = withPage(new URLSearchParams('q=rating&page=3&size=50'), 1, 25)

        expect(next.toString()).toBe('q=rating')
    })

    it('does not change the parameters it was given', () => {
        const params = new URLSearchParams('page=2')

        withPage(params, 4, 100)

        expect(params.toString()).toBe('page=2')
    })
})

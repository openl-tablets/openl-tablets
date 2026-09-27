import { renderHook } from '@testing-library/react'
import { useScript } from './useScript'

const sources = () => Array.from(document.body.querySelectorAll('script'), script => script.getAttribute('src'))

describe('useScript', () => {
    it('adds the scripts to the page and takes them away once unmounted', () => {
        const { unmount } = renderHook(() => useScript(['/extra/one.js', '/extra/two.js']))

        expect(sources()).toEqual(['/extra/one.js', '/extra/two.js'])

        unmount()

        expect(sources()).toEqual([])
    })
})

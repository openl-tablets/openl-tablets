import { afterEach, describe, expect, it, vi } from 'vitest'
import { openImageWindow } from './openImageWindow'

describe('openImageWindow', () => {
    afterEach(() => vi.restoreAllMocks())

    it('opens a blank tab holding the picture alone, titled by its file', () => {
        const page = document.implementation.createHTMLDocument('')
        vi.spyOn(window, 'open').mockReturnValue({ document: page } as unknown as Window)

        expect(openImageWindow('/rest/projects/p1/files/img/logo.svg', 'logo.svg')).toBe(true)

        expect(window.open).toHaveBeenCalledWith('', '_blank')
        expect(page.title).toBe('logo.svg')
        const image = page.querySelector('img')
        // The blank tab has no address of its own to read a relative one against.
        expect(image?.getAttribute('src')).toBe(new URL('/rest/projects/p1/files/img/logo.svg', window.location.href).href)
        expect(image?.alt).toBe('logo.svg')
        expect(page.head.querySelector('style')?.textContent).toContain('object-fit: contain')
    })

    it('tells when the browser opens no tab', () => {
        vi.spyOn(window, 'open').mockReturnValue(null)

        expect(openImageWindow('/rest/x.png', 'x.png')).toBe(false)
    })
})

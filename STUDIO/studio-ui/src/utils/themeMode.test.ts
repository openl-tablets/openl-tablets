import { beforeEach, describe, expect, it } from 'vitest'
import { DEFAULT_THEME_MODE, readThemeMode, storeThemeMode, THEME_MODE_KEY } from './themeMode'

describe('themeMode', () => {
    beforeEach(() => localStorage.clear())

    it('follows the system until the user picks an appearance', () => {
        expect(DEFAULT_THEME_MODE).toBe('auto')
        expect(readThemeMode()).toBe('auto')
    })

    it('remembers the picked appearance', () => {
        storeThemeMode('dark')

        expect(localStorage.getItem(THEME_MODE_KEY)).toBe('dark')
        expect(readThemeMode()).toBe('dark')
    })

    it('falls back to the system when the stored appearance is not one it knows', () => {
        localStorage.setItem(THEME_MODE_KEY, 'sepia')

        expect(readThemeMode()).toBe('auto')
    })
})

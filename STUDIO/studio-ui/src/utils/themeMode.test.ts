import { beforeEach, describe, expect, it } from 'vitest'
import {
    DEFAULT_THEME_MODE,
    readCompactMode,
    readThemeMode,
    storeCompactMode,
    storeThemeMode,
    THEME_COMPACT_KEY,
    THEME_MODE_KEY,
} from './themeMode'

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

    it('lays the application out comfortably until the user asks for compact', () => {
        expect(readCompactMode()).toBe(false)
    })

    it('remembers the picked density', () => {
        storeCompactMode(true)

        expect(localStorage.getItem(THEME_COMPACT_KEY)).toBe('true')
        expect(readCompactMode()).toBe(true)

        storeCompactMode(false)

        expect(readCompactMode()).toBe(false)
    })

    it('stays comfortable when the stored density is not one it knows', () => {
        localStorage.setItem(THEME_COMPACT_KEY, 'cosy')

        expect(readCompactMode()).toBe(false)
    })
})

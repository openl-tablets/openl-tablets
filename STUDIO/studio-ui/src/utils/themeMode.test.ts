import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { beforeEach, describe, expect, it } from 'vitest'
import {
    appearanceOf,
    DEFAULT_THEME_MODE,
    DEFAULT_THEME_NAME,
    readCompactMode,
    readThemeMode,
    readThemeName,
    storeCompactMode,
    storeThemeMode,
    storeThemeName,
    THEME_COMPACT_KEY,
    THEME_MODE_KEY,
    THEME_NAME_KEY,
} from './themeMode'

describe('themeMode', () => {
    it('is read by the page shell under the same key before the bundle loads', () => {
        const shell = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8')
        expect(shell).toContain(`localStorage.getItem('${THEME_MODE_KEY}')`)
    })

    beforeEach(() => localStorage.clear())

    it('names the appearance a mode stands for without waiting for an effect', () => {
        expect(appearanceOf('dark')).toBe('dark')
        expect(appearanceOf('light')).toBe('light')
        const system = matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
        expect(appearanceOf('auto')).toBe(system)
    })

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

    it('starts in the standard theme until the user picks another', () => {
        expect(DEFAULT_THEME_NAME).toBe('standard')
        expect(readThemeName()).toBe('standard')
    })

    it('remembers the picked theme', () => {
        storeThemeName('evergreen')

        expect(localStorage.getItem(THEME_NAME_KEY)).toBe('evergreen')
        expect(readThemeName()).toBe('evergreen')
    })

    it('falls back to the standard theme when the stored one is no longer offered', () => {
        localStorage.setItem(THEME_NAME_KEY, 'midnight')

        expect(readThemeName()).toBe('standard')
    })

    it('falls back to the standard theme when the stored name is one every object answers to', () => {
        localStorage.setItem(THEME_NAME_KEY, 'toString')

        expect(readThemeName()).toBe('standard')
    })
})

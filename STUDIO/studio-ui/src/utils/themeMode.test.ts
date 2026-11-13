import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { beforeEach, describe, expect, it } from 'vitest'
import {
    appearanceOf,
    compactModeSetting,
    DEFAULT_THEME_MODE,
    DEFAULT_THEME_NAME,
    excelFormattingSetting,
    THEME_COMPACT_KEY,
    THEME_EXCEL_FORMATTING_KEY,
    THEME_MODE_KEY,
    THEME_NAME_KEY,
    themeModeSetting,
    themeNameSetting,
} from './themeMode'

describe('themeMode', () => {
    beforeEach(() => localStorage.clear())

    it('is read by the page shell under the same key before the bundle loads', () => {
        const shell = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8')
        expect(shell).toContain(`localStorage.getItem('${THEME_MODE_KEY}')`)
    })

    it('names the appearance a mode stands for without waiting for an effect', () => {
        expect(appearanceOf('dark')).toBe('dark')
        expect(appearanceOf('light')).toBe('light')
        const system = matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
        expect(appearanceOf('auto')).toBe(system)
    })

    it('follows the system until the user picks an appearance', () => {
        expect(DEFAULT_THEME_MODE).toBe('auto')
        expect(themeModeSetting.read()).toBe('auto')
    })

    it('remembers the picked appearance', () => {
        themeModeSetting.write('dark')

        expect(localStorage.getItem(THEME_MODE_KEY)).toBe('dark')
        expect(themeModeSetting.read()).toBe('dark')
    })

    it('stores nothing when the system is followed again', () => {
        themeModeSetting.write('dark')
        themeModeSetting.write('auto')

        expect(localStorage.getItem(THEME_MODE_KEY)).toBeNull()
        expect(themeModeSetting.read()).toBe('auto')
    })

    it('falls back to the system when the stored appearance is not one it knows', () => {
        localStorage.setItem(THEME_MODE_KEY, 'sepia')

        expect(themeModeSetting.read()).toBe('auto')
    })

    it('lays the application out comfortably until the user asks for compact', () => {
        expect(compactModeSetting.read()).toBe(false)
    })

    it('remembers the picked density', () => {
        compactModeSetting.write(true)

        expect(localStorage.getItem(THEME_COMPACT_KEY)).toBe('true')
        expect(compactModeSetting.read()).toBe(true)

        compactModeSetting.write(false)

        expect(compactModeSetting.read()).toBe(false)
        // comfortable is the default, kept as no key at all
        expect(localStorage.getItem(THEME_COMPACT_KEY)).toBeNull()
    })

    it('stays comfortable when the stored density is not one it knows', () => {
        localStorage.setItem(THEME_COMPACT_KEY, 'cosy')

        expect(compactModeSetting.read()).toBe(false)
    })

    it('shows the tables in the table theme until the user asks for their Excel formatting', () => {
        expect(excelFormattingSetting.read()).toBe(false)
    })

    it('remembers whether the tables are shown in their Excel formatting', () => {
        excelFormattingSetting.write(true)

        expect(localStorage.getItem(THEME_EXCEL_FORMATTING_KEY)).toBe('true')
        expect(excelFormattingSetting.read()).toBe(true)

        excelFormattingSetting.write(false)

        expect(excelFormattingSetting.read()).toBe(false)
        expect(localStorage.getItem(THEME_EXCEL_FORMATTING_KEY)).toBeNull()
    })

    it('keeps the table theme when the stored choice is not one it knows', () => {
        localStorage.setItem(THEME_EXCEL_FORMATTING_KEY, 'yes')

        expect(excelFormattingSetting.read()).toBe(false)
    })

    it('starts in the standard theme until the user picks another', () => {
        expect(DEFAULT_THEME_NAME).toBe('standard')
        expect(themeNameSetting.read()).toBe('standard')
    })

    it('remembers the picked theme', () => {
        themeNameSetting.write('dracula')

        expect(localStorage.getItem(THEME_NAME_KEY)).toBe('dracula')
        expect(themeNameSetting.read()).toBe('dracula')

        themeNameSetting.write('standard')

        expect(localStorage.getItem(THEME_NAME_KEY)).toBeNull()
    })

    it('falls back to the standard theme when the stored one is no longer offered', () => {
        // Evergreen, offered by an earlier version
        localStorage.setItem(THEME_NAME_KEY, 'evergreen')

        expect(themeNameSetting.read()).toBe('standard')
    })

    it('falls back to the standard theme when the stored name is one every object answers to', () => {
        localStorage.setItem(THEME_NAME_KEY, 'toString')

        expect(themeNameSetting.read()).toBe('standard')
    })
})

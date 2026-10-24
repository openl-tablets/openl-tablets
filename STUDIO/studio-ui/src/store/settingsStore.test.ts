import { beforeEach, describe, expect, it } from 'vitest'
import { DEFAULT_USER_SETTINGS, readUserSettings, savedTestsOptions, USER_SETTINGS_KEY, useSettingsStore } from './settingsStore'

describe('settingsStore', () => {
    beforeEach(() => {
        localStorage.clear()
        useSettingsStore.setState({ settings: DEFAULT_USER_SETTINGS })
    })

    it('starts from the defaults in a browser where nothing was picked', () => {
        expect(readUserSettings()).toEqual(DEFAULT_USER_SETTINGS)
    })

    it('remembers a picked setting in the browser, beside the others', () => {
        useSettingsStore.getState().setSetting('showFormulas', true)
        useSettingsStore.getState().setSetting('testsPerPage', -1)

        expect(useSettingsStore.getState().settings).toEqual({
            ...DEFAULT_USER_SETTINGS,
            showFormulas: true,
            testsPerPage: -1,
        })
        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, showFormulas: true, testsPerPage: -1 })
    })

    it('falls back setting by setting on what this version does not know', () => {
        localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify({
            showHeader: false,
            showFormulas: 'yes',
            testsPerPage: 7,
            showRealNumbers: true,
        }))

        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, showHeader: false, showRealNumbers: true })
    })

    it.each(['not json', '[true]', 'null'])('falls back to the defaults on a stored %s', stored => {
        localStorage.setItem(USER_SETTINGS_KEY, stored)

        expect(readUserSettings()).toEqual(DEFAULT_USER_SETTINGS)
    })

    it('starts a test run from the testing settings, the count of failures at its default', () => {
        useSettingsStore.setState({
            settings: { ...DEFAULT_USER_SETTINGS, testsPerPage: 20, testsFailuresOnly: true, showComplexResult: true },
        })

        expect(savedTestsOptions()).toEqual({ size: 20, failuresOnly: true, failures: 5, compoundResult: true })
    })

    it('keeps a setting another tab changed meanwhile when it picks one', () => {
        localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify({ ...DEFAULT_USER_SETTINGS, showFormulas: true }))

        useSettingsStore.getState().setSetting('showHeader', false)

        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, showFormulas: true, showHeader: false })
        expect(useSettingsStore.getState().settings.showFormulas).toBe(true)
    })

    it('applies a setting another tab changed at once', () => {
        localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify({ ...DEFAULT_USER_SETTINGS, showRealNumbers: true }))

        window.dispatchEvent(new StorageEvent('storage', { key: USER_SETTINGS_KEY }))

        expect(useSettingsStore.getState().settings.showRealNumbers).toBe(true)
    })

    it('leaves the settings alone when another key of the storage changes', () => {
        localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify({ ...DEFAULT_USER_SETTINGS, showRealNumbers: true }))

        window.dispatchEvent(new StorageEvent('storage', { key: 'openl.theme.mode' }))

        expect(useSettingsStore.getState().settings).toEqual(DEFAULT_USER_SETTINGS)
    })
})

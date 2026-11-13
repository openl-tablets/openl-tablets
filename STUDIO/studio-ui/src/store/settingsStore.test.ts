import { beforeEach, describe, expect, it } from 'vitest'
import { DEFAULT_USER_SETTINGS, readUserSettings, savedTestsOptions, userSettingKey, useSettingsStore } from './settingsStore'

/** What the browser keeps of the settings, key by key. */
const storedSettings = () => Object.fromEntries(
    Object.keys(localStorage)
        .filter(key => key.startsWith('openl.settings'))
        .map(key => [key, localStorage.getItem(key)])
)

describe('settingsStore', () => {
    beforeEach(() => {
        localStorage.clear()
        useSettingsStore.setState({ settings: DEFAULT_USER_SETTINGS })
    })

    it('starts from the defaults in a browser where nothing was picked', () => {
        expect(readUserSettings()).toEqual(DEFAULT_USER_SETTINGS)
    })

    it('remembers each picked setting under a key of its own', () => {
        useSettingsStore.getState().setSetting('showFormulas', true)
        useSettingsStore.getState().setSetting('testsPerPage', -1)
        useSettingsStore.getState().setSetting('dateFormat', 'DD.MM.YYYY')
        useSettingsStore.getState().setSetting('timeFormat', 'HH:mm:ss')

        const picked = {
            ...DEFAULT_USER_SETTINGS,
            showFormulas: true,
            testsPerPage: -1,
            dateFormat: 'DD.MM.YYYY',
            timeFormat: 'HH:mm:ss',
        }
        expect(useSettingsStore.getState().settings).toEqual(picked)
        expect(readUserSettings()).toEqual(picked)
        expect(storedSettings()).toEqual({
            'openl.settings.showFormulas': 'true',
            'openl.settings.testsPerPage': '-1',
            'openl.settings.dateFormat': 'DD.MM.YYYY',
            'openl.settings.timeFormat': 'HH:mm:ss',
        })
    })

    it('forgets a setting set back to its default', () => {
        useSettingsStore.getState().setSetting('showFormulas', true)
        useSettingsStore.getState().setSetting('showHeader', true)
        useSettingsStore.getState().setSetting('showFormulas', false)

        expect(storedSettings()).toEqual({})
        expect(useSettingsStore.getState().settings).toEqual(DEFAULT_USER_SETTINGS)
    })

    it('falls back setting by setting on what this version does not know', () => {
        localStorage.setItem(userSettingKey('showHeader'), 'false')
        localStorage.setItem(userSettingKey('showFormulas'), 'yes')
        localStorage.setItem(userSettingKey('testsPerPage'), '7')
        localStorage.setItem(userSettingKey('showRealNumbers'), 'true')

        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, showHeader: false, showRealNumbers: true })
    })

    it.each(['', ' ', 'all', 'Infinity'])('takes the default page size for a stored "%s"', stored => {
        localStorage.setItem(userSettingKey('testsPerPage'), stored)

        expect(readUserSettings().testsPerPage).toBe(DEFAULT_USER_SETTINGS.testsPerPage)
    })

    it('forgets a format when the system one is picked again', () => {
        useSettingsStore.getState().setSetting('timeFormat', 'h:mm:ss A')
        expect(readUserSettings().timeFormat).toBe('h:mm:ss A')

        useSettingsStore.getState().setSetting('timeFormat', '')
        expect(readUserSettings().timeFormat).toBe('')
        expect(storedSettings()).toEqual({})
    })

    it('takes the system format for a stored format the menu does not offer', () => {
        localStorage.setItem(userSettingKey('dateFormat'), 'YYYY|MM|DD')
        localStorage.setItem(userSettingKey('timeFormat'), 'HH:mm')

        expect(readUserSettings()).toEqual(DEFAULT_USER_SETTINGS)
    })

    it('starts a test run from the testing settings, the count of failures at its default', () => {
        useSettingsStore.setState({
            settings: { ...DEFAULT_USER_SETTINGS, testsPerPage: 20, testsFailuresOnly: true, showComplexResult: true },
        })

        expect(savedTestsOptions()).toEqual({ size: 20, failuresOnly: true, failures: 5, compoundResult: true })
    })

    it('keeps a setting another tab changed meanwhile when it picks one', () => {
        localStorage.setItem(userSettingKey('showFormulas'), 'true')

        useSettingsStore.getState().setSetting('showHeader', false)

        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, showFormulas: true, showHeader: false })
        expect(useSettingsStore.getState().settings.showFormulas).toBe(true)
    })

    it('applies a setting another tab changed at once', () => {
        localStorage.setItem(userSettingKey('showRealNumbers'), 'true')

        window.dispatchEvent(new StorageEvent('storage', { key: userSettingKey('showRealNumbers') }))

        expect(useSettingsStore.getState().settings.showRealNumbers).toBe(true)
    })

    it('leaves the settings alone when another key of the storage changes', () => {
        localStorage.setItem(userSettingKey('showRealNumbers'), 'true')

        window.dispatchEvent(new StorageEvent('storage', { key: 'openl.theme.mode' }))

        expect(useSettingsStore.getState().settings).toEqual(DEFAULT_USER_SETTINGS)
    })
})

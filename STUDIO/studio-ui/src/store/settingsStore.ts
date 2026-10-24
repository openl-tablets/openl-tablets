import { create } from 'zustand'
import { isJsonObject, readJson, writeJson } from 'utils/localStore'
import { FAILURES_PER_TEST, TESTS_PAGE_SIZE, TESTS_PAGE_SIZES } from 'constants/tests'
import type { TestsQuery } from 'services/execution'

/** The choices of **My Settings**: how the tables, the test results and the trace are shown. */
export interface UserSettings {
    /** The technical rows of a table are shown above its data. */
    showHeader: boolean
    /** A cell holding an Excel formula shows the formula rather than its value. */
    showFormulas: boolean
    /** How many test tables one page of the test results holds; `-1` for all of them. */
    testsPerPage: number
    /** The test results list only the failed cases. */
    testsFailuresOnly: boolean
    /** Every test case also carries the whole value the rule returned. */
    showComplexResult: boolean
    /** The trace shows numbers with their full precision. */
    showRealNumbers: boolean
}

/** Where **My Settings** are remembered: in this browser only, for every tab and visit. */
export const USER_SETTINGS_KEY = 'openl.settings'

/** The settings of a browser where nothing was picked yet. */
export const DEFAULT_USER_SETTINGS: Readonly<UserSettings> = {
    showHeader: true,
    showFormulas: false,
    testsPerPage: TESTS_PAGE_SIZE,
    testsFailuresOnly: false,
    showComplexResult: false,
    showRealNumbers: false,
}

/** Whether a stored value can stand for the setting: of the same type, and a page size the screens offer. */
const fits = (key: keyof UserSettings, value: unknown): boolean => key === 'testsPerPage'
    ? TESTS_PAGE_SIZES.includes(value as number)
    : typeof value === typeof DEFAULT_USER_SETTINGS[key]

/**
 * The remembered settings.
 *
 * Each setting falls back to its default on its own: when nothing was stored yet, when the browser refuses
 * storage, or when the stored value is not one this version knows.
 */
export const readUserSettings = (): UserSettings => {
    const stored = readJson<Record<string, unknown>>(USER_SETTINGS_KEY, {}, isJsonObject)
    const settings = { ...DEFAULT_USER_SETTINGS }
    for (const key of Object.keys(settings) as (keyof UserSettings)[]) {
        if (fits(key, stored[key])) {
            Object.assign(settings, { [key]: stored[key] })
        }
    }
    return settings
}

interface SettingsStore {
    settings: UserSettings
    /**
     * Applies one setting at once and remembers it for the next visit.
     *
     * The other settings are taken as stored, so a change made meanwhile in another tab is kept.
     */
    setSetting: <K extends keyof UserSettings>(key: K, value: UserSettings[K]) => void
}

export const useSettingsStore = create<SettingsStore>(set => ({
    settings: readUserSettings(),
    setSetting: (key, value) => {
        const settings = { ...readUserSettings(), [key]: value }
        writeJson(USER_SETTINGS_KEY, settings)
        set({ settings })
    },
}))

// A setting changed in another tab applies here at once; a cleared storage brings the defaults back.
globalThis.addEventListener?.('storage', (event: StorageEvent) => {
    if (event.key === USER_SETTINGS_KEY || event.key === null) {
        useSettingsStore.setState({ settings: readUserSettings() })
    }
})

/** What a test run starts from in **My Settings**: they say whether only the failures are listed, not how many. */
type SavedTestsOptions = Required<Pick<TestsQuery, 'size' | 'failuresOnly' | 'failures' | 'compoundResult'>>

/**
 * The options a test run starts from, as **My Settings** keep them now.
 *
 * The count of failures of a test table is not one of the settings, so it starts at its default.
 */
export const savedTestsOptions = (): SavedTestsOptions => {
    const { settings } = useSettingsStore.getState()
    return {
        size: settings.testsPerPage,
        failuresOnly: settings.testsFailuresOnly,
        failures: FAILURES_PER_TEST,
        compoundResult: settings.showComplexResult,
    }
}

import { create } from 'zustand'
import { booleanText, oneOf, storedSetting, type SettingCodec, type StoredSetting } from 'utils/localStore'
import { FAILURES_PER_TEST, TESTS_PAGE_SIZE, TESTS_PAGE_SIZES } from 'constants/tests'
import type { TestsQuery } from 'services/execution'

/** The choices of **My Settings**: how the tables, the test results, the trace and the dates are shown. */
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
    /** The day.js pattern every screen writes a date in, one of {@link DATE_FORMATS}; empty for the browser's own. */
    dateFormat: string
    /**
     * The day.js pattern every screen writes a time of day in, after the date, one of {@link TIME_FORMATS}; empty for
     * the browser's own.
     */
    timeFormat: string
}

/** The formats a date can be picked in, as day.js patterns, in the order the menu offers them. */
export const DATE_FORMATS = [
    'DD/MM/YYYY',
    'DD.MM.YYYY',
    'DD-MM-YYYY',
    'DD MMM YYYY',
    'YYYY-MM-DD',
    'YYYY.MM.DD',
    'YYYY/MM/DD',
    'MMM D, YYYY',
    'MM/DD/YYYY',
] as const

/**
 * The formats a time of day can be picked in, as day.js patterns, in the order the menu offers them: on a 24-hour
 * clock, such as "13:45:30", and on a 12-hour one, such as "1:45:30 PM".
 *
 * Seconds are shown because the things carrying a time here — revisions, deployments, locks, logins — are routinely
 * made within the same minute, and two of them stripped to the minute read alike.
 */
export const TIME_FORMATS = ['HH:mm:ss', 'h:mm:ss A'] as const

/** The settings of a browser where nothing was picked yet. */
export const DEFAULT_USER_SETTINGS: Readonly<UserSettings> = {
    showHeader: true,
    showFormulas: false,
    testsPerPage: TESTS_PAGE_SIZE,
    testsFailuresOnly: false,
    showComplexResult: false,
    showRealNumbers: false,
    dateFormat: '',
    timeFormat: '',
}

/**
 * Where a setting of **My Settings** is remembered: in this browser only, for every tab and visit, each setting under
 * a key of its own. A setting at its default is not stored.
 */
export const userSettingKey = (key: keyof UserSettings): string => `openl.settings.${key}`

const setting = <K extends keyof UserSettings>(key: K, codec: SettingCodec<UserSettings[K]>) =>
    storedSetting(userSettingKey(key), DEFAULT_USER_SETTINGS[key], codec)

/** How each setting is kept: as text under a key of its own, and not at all while at its default. */
const STORED: {readonly [K in keyof UserSettings]: StoredSetting<UserSettings[K]> } = {
    showHeader: setting('showHeader', booleanText),
    showFormulas: setting('showFormulas', booleanText),
    testsPerPage: setting('testsPerPage', oneOf(TESTS_PAGE_SIZES)),
    testsFailuresOnly: setting('testsFailuresOnly', booleanText),
    showComplexResult: setting('showComplexResult', booleanText),
    showRealNumbers: setting('showRealNumbers', booleanText),
    dateFormat: setting('dateFormat', oneOf<string>(['', ...DATE_FORMATS])),
    timeFormat: setting('timeFormat', oneOf<string>(['', ...TIME_FORMATS])),
}

const SETTING_KEYS = Object.keys(STORED) as (keyof UserSettings)[]

/**
 * The remembered settings.
 *
 * Each setting falls back to its default on its own: when nothing was stored yet, when the browser refuses
 * storage, or when the stored value is not one this version knows.
 */
export const readUserSettings = (): UserSettings => {
    const settings = { ...DEFAULT_USER_SETTINGS }
    for (const key of SETTING_KEYS) {
        Object.assign(settings, { [key]: STORED[key].read() })
    }
    return settings
}

interface SettingsStore {
    settings: UserSettings
    /**
     * Applies one setting at once and remembers it for the next visit.
     *
     * A setting set back to its default is forgotten. The other settings are taken as stored, so a change made
     * meanwhile in another tab is kept.
     */
    setSetting: <K extends keyof UserSettings>(key: K, value: UserSettings[K]) => void
}

export const useSettingsStore = create<SettingsStore>(set => ({
    settings: readUserSettings(),
    setSetting: (key, value) => {
        STORED[key].write(value)
        set({ settings: { ...readUserSettings(), [key]: value } })
    },
}))

// A setting changed in another tab applies here at once; a cleared storage brings the defaults back.
globalThis.addEventListener?.('storage', (event: StorageEvent) => {
    if (event.key === null || Object.values(STORED).some(stored => stored.key === event.key)) {
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

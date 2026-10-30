import dayjs from 'dayjs'
import localizedFormat from 'dayjs/plugin/localizedFormat'
import i18next from 'i18next'

dayjs.extend(localizedFormat)

// The date format follows the application language. A day.js locale for every new application
// language must be registered statically next to its i18n bundle, e.g. import 'dayjs/locale/de'.
const applyLanguage = (language?: string) => {
    if (language) {
        dayjs.locale(language.toLowerCase())
    }
}

// i18next may have been initialized before this module is loaded, so the current language
// is applied once on load and then kept in sync on every change.
applyLanguage(i18next.language)
i18next.on('languageChanged', applyLanguage)

/**
 * Formats a date without time, such as "Aug 16, 2018", using the date format of the current language.
 *
 * Returns the raw string when it cannot be parsed.
 */
export const formatDate = (value: string): string => {
    const date = dayjs(value)
    return date.isValid() ? date.format('ll') : value
}

/**
 * Formats a date with time down to the second, such as "Aug 16, 2018 8:02:30 PM", using the date
 * format of the current language.
 *
 * Seconds are shown because the things carrying a time here — revisions, deployments, locks, logins —
 * are routinely made within the same minute, and two of them stripped to the minute read alike. The
 * date and the full time are combined because the single localized token, 'lll', stops at the minute.
 *
 * Returns null for a missing value and the raw string when it cannot be parsed, so callers can apply
 * their own fallback.
 */
export const formatDateTime = (value?: string): string | null => {
    if (!value) {
        return null
    }
    const date = dayjs(value)
    return date.isValid() ? date.format('ll LTS') : value
}

/** A date and time written with no time zone, as a date property of a table holds it: `2020-07-01T10:30:00.000`. */
const LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/

/** Whether a value is a date and time written with no time zone. */
export const isLocalDateTime = (value: unknown): value is string =>
    typeof value === 'string' && LOCAL_DATE_TIME.test(value)

/**
 * Formats a date and time written with no time zone, such as a date property of a table.
 *
 * Such a value names a day and an hour on the calendar, not a moment, so it is never shifted to the time zone of the
 * browser. A value at midnight holds no time of day and shows the date alone.
 *
 * Returns the raw string when it cannot be parsed.
 */
export const formatLocalDateTime = (value: string): string => {
    const date = dayjs(value)
    if (!date.isValid()) {
        return value
    }
    const midnight = date.hour() === 0 && date.minute() === 0 && date.second() === 0
    return date.format(midnight ? 'll' : 'll LTS')
}

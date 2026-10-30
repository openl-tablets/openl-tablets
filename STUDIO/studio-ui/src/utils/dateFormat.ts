import dayjs, { type Dayjs } from 'dayjs'
import localizedFormat from 'dayjs/plugin/localizedFormat'
import utc from 'dayjs/plugin/utc'
import i18next from 'i18next'

dayjs.extend(localizedFormat)
dayjs.extend(utc)

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

/**
 * The letters a comment writes a moment with, each with the longest run of it a pattern may hold: the ones the server
 * writes (`Comments.isWritten`), so the comment suggested here is the one the server would write.
 */
const PATTERN_LETTERS: Readonly<Record<string, number>> = { y: 4, M: 4, d: 2, H: 2, h: 2, m: 2, s: 2, S: 3, a: 1, E: 4 }

/** The day.js token a run of a letter of a `DateTimeFormatter` pattern is written by, but the fraction of a second. */
const dayjsToken = (letter: string, count: number): string => {
    switch (letter) {
        case 'y':
            return count === 2 ? 'YY' : 'YYYY'
        case 'M':
            return ['M', 'MM', 'MMM', 'MMMM'][count - 1]!
        case 'd':
            return count === 1 ? 'D' : 'DD'
        case 'a':
            return 'A'
        case 'E':
            return count < 4 ? 'ddd' : 'dddd'
        default:
            return letter.repeat(count)
    }
}

/** A part of a pattern as it is written, and where the part ends in the pattern. */
interface WrittenPart {
    text: string
    end: number
}

/**
 * The text in quotes starting at the given quote, two quotes standing for one, and where it ends; nothing for a text
 * no quote closes.
 */
const quotedAt = (pattern: string, quote: number): WrittenPart | null => {
    if (pattern[quote + 1] === "'") {
        return { text: "'", end: quote + 2 }
    }
    let text = ''
    let at = quote + 1
    while (at < pattern.length) {
        const char = pattern[at]!
        if (char === "'" && pattern[at + 1] !== "'") {
            return { text, end: at + 1 }
        }
        text += char
        at += char === "'" ? 2 : 1
    }
    return null
}

/**
 * The run of one character starting at the given place, written as the server writes it, and where it ends. A letter
 * stands for a part of the moment, and any other character for itself. Nothing for a letter the server does not
 * write, or a run of it longer than the server writes.
 */
const runAt = (date: Dayjs, pattern: string, at: number): WrittenPart | null => {
    const char = pattern[at]!
    let end = at
    while (pattern[end] === char) {
        end++
    }
    if (!/[A-Za-z]/.test(char)) {
        return { text: pattern.slice(at, end), end }
    }
    const count = end - at
    if (count > (PATTERN_LETTERS[char] ?? 0)) {
        return null
    }
    const text = char === 'S'
        ? String(date.millisecond()).padStart(3, '0').slice(0, count)
        : date.format(dayjsToken(char, count))
    return { text, end }
}

/**
 * The moment written in a pattern of Java's `DateTimeFormatter`, as the server writes it, or nothing for a pattern
 * of letters the server does not write.
 */
const inJavaPattern = (date: Dayjs, pattern: string): string | null => {
    let written = ''
    let at = 0
    while (at < pattern.length) {
        const part = pattern[at] === "'" ? quotedAt(pattern, at) : runAt(date, pattern, at)
        if (part === null) {
            return null
        }
        written += part.text
        at = part.end
    }
    return written
}

/**
 * The moment of a revision as a commit comment names it, the way the server writes the comments it builds: in ISO 8601
 * and UTC to the second, `2020-06-22T21:02:42Z`, or in the pattern the template names, `{datetime:yyyy-MM-dd HH:mm}`,
 * in UTC as well.
 *
 * A comment is text stored in the repository and read by anyone, so it takes neither the formats of the reader nor
 * a time zone, and names months and days in English. A pattern of other letters than the year, the month, the day,
 * the hours, the minutes, the seconds and their fraction, the day period and the day of the week is written in ISO
 * 8601, as the server writes it. Empty for a missing or unreadable value.
 *
 * @param pattern the pattern of Java's `DateTimeFormatter` the template names, or nothing for ISO 8601
 */
export const commentMoment = (value?: string, pattern?: string): string => {
    const date = dayjs(value)
    if (!value || !date.isValid()) {
        return ''
    }
    const iso = date.toISOString().replace(/\.\d{3}Z$/, 'Z')
    return pattern ? inJavaPattern(date.utc().locale('en'), pattern) ?? iso : iso
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

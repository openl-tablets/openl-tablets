import { useMemo } from 'react'
import dayjs, { type Dayjs } from 'dayjs'
import utc from 'dayjs/plugin/utc'
import i18next from 'i18next'
import { useSettingsStore } from '../store/settingsStore'

dayjs.extend(utc)

// The names of the months follow the application language. A day.js locale for every new application
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

const DATE_PART_TOKENS = {
    day: 'DD',
    month: 'MM',
    year: 'YYYY',
} as const

/**
 * A separator a locale writes between the parts of a date, kept as it is by day.js.
 *
 * A space of any kind is written as a plain one. A locale may separate the time from its day period by a narrow
 * no-break space, as newer browsers do for English, and a reader types a plain space, which the pattern then reads.
 */
const literal = (value: string): string => `[${value.replace(/\s/g, ' ')}]`

/** Date picker pattern derived from the user's locale, independent of the value stored in the workbook. */
export const datePickerFormatForLocale = (locale?: string): string =>
    new Intl.DateTimeFormat(locale, { day: '2-digit', month: '2-digit', year: 'numeric' })
        .formatToParts(new Date(2006, 10, 22))
        .map(part => part.type in DATE_PART_TOKENS
            ? DATE_PART_TOKENS[part.type as keyof typeof DATE_PART_TOKENS]
            : literal(part.value))
        .join('')

/**
 * The pattern of the time of day, down to the second, that a locale writes: the order of its parts, its separators,
 * and a 12-hour clock with its day period where the locale keeps one.
 */
export const timeFormatForLocale = (locale?: string): string => {
    const format = new Intl.DateTimeFormat(locale, { hour: 'numeric', minute: '2-digit', second: '2-digit' })
    const twelveHours = ['h11', 'h12'].includes(format.resolvedOptions().hourCycle ?? '')
    const tokens: Partial<Record<Intl.DateTimeFormatPartTypes, string>> = {
        hour: twelveHours ? 'h' : 'HH',
        minute: 'mm',
        second: 'ss',
        dayPeriod: 'A',
    }
    return format.formatToParts(new Date(2006, 10, 22, 13, 5, 9))
        .map(part => tokens[part.type] ?? literal(part.value))
        .join('')
}

/** The time of day on a 12-hour clock, down to the second, such as "1:45:30 PM". */
const TIME_12_HOURS = 'h:mm:ss A'

/**
 * The time of day on a 24-hour clock, down to the second, such as "13:45:30".
 *
 * Seconds are shown because the things carrying a time here — revisions, deployments, locks, logins — are routinely
 * made within the same minute, and two of them stripped to the minute read alike.
 */
const TIME_24_HOURS = 'HH:mm:ss'

/** The patterns of day.js a date alone and a date with its time are written in. */
export interface DateFormats {
    date: string
    dateTime: string
}

/**
 * The patterns of the date and time settings of **My Settings**: the ones of the regional settings of the browser
 * while the reader follows the preferences of the system, or else the Date Format they picked and the time on the
 * clock they picked.
 */
const patternsOf = (system: boolean, dateFormat: string, hours24: boolean): DateFormats => {
    if (system) {
        const date = datePickerFormatForLocale()
        return { date, dateTime: `${date} ${timeFormatForLocale()}` }
    }
    return { date: dateFormat, dateTime: `${dateFormat} ${hours24 ? TIME_24_HOURS : TIME_12_HOURS}` }
}

/** The patterns dates are written in now, for a call that is handed none. */
export const formats = (): DateFormats => {
    const { useSystemDateFormat, dateFormat, hours24 } = useSettingsStore.getState().settings
    return patternsOf(useSystemDateFormat, dateFormat, hours24)
}

/**
 * The pattern a date picker shows and reads a date in: the one dates are written in, so a date is typed as the
 * reader reads it.
 *
 * A picker gives the date it read as a date; the screen sends it to the server in ISO 8601, whatever the pattern.
 *
 * @param withTime whether the picker shows the time of day after the day
 */
export const datePickerFormat = (withTime = false): string => {
    const patterns = formats()
    return withTime ? patterns.dateTime : patterns.date
}

/**
 * The patterns the reader writes dates in, which subscribe the screen to the date and time settings, so it draws its
 * dates again the moment the reader changes them.
 *
 * Every component that writes a date — through the functions of this module, a version label or a date picker —
 * calls it. A value worked out from dates in `useMemo` hands the patterns to the functions that write the dates and
 * names them among its dependencies, so it is worked out again in the new formats.
 */
export const useDateFormats = (): DateFormats => {
    const system = useSettingsStore(state => state.settings.useSystemDateFormat)
    const dateFormat = useSettingsStore(state => state.settings.dateFormat)
    const hours24 = useSettingsStore(state => state.settings.hours24)
    return useMemo(() => patternsOf(system, dateFormat, hours24), [system, dateFormat, hours24])
}

/**
 * Formats a date without time, such as "12/31/2026", as the date and time settings of the reader write it.
 *
 * A moment, such as `2026-10-29T18:20:19Z`, is shown in the time zone of the browser.
 *
 * Returns the raw string when it cannot be parsed.
 */
export const formatDate = (value: string, patterns: DateFormats = formats()): string => {
    const date = dayjs(value)
    return date.isValid() ? date.format(patterns.date) : value
}

/**
 * Formats a date with its time down to the second, such as "12/31/2026 1:45:30 PM", as the date and time settings of
 * the reader write it.
 *
 * A moment, such as `2026-10-29T18:20:19Z`, is shown in the time zone of the browser, so the same moment reads
 * the same on every screen.
 *
 * Returns null for a missing value and the raw string when it cannot be parsed, so callers can apply
 * their own fallback.
 */
export const formatDateTime = (value?: string, patterns: DateFormats = formats()): string | null => {
    if (!value) {
        return null
    }
    const date = dayjs(value)
    return date.isValid() ? date.format(patterns.dateTime) : value
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
 * Formats a date and time written with no time zone, such as a date property or a date cell of a table, in the
 * formats of the reader.
 *
 * Such a value names a day and an hour on the calendar, not a moment, so it is never shifted to the time zone of the
 * browser. A value at midnight holds no time of day and shows the date alone, in the Date Format; any other is
 * followed by the time in the Time Format.
 *
 * Returns the raw string when it cannot be parsed.
 */
export const formatLocalDateTime = (value: string, patterns: DateFormats = formats()): string => {
    const date = dayjs(value)
    if (!date.isValid()) {
        return value
    }
    const midnight = date.hour() === 0 && date.minute() === 0 && date.second() === 0
    return date.format(midnight ? patterns.date : patterns.dateTime)
}

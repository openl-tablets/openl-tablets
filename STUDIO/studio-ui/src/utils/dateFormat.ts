import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import i18next from 'i18next'
import { useSettingsStore } from '../store/settingsStore'
import { cached } from './cached'

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
 * Only a separator holding a letter is put in brackets, which day.js writes as they are: a letter stands for a part
 * of the date, as the `h`, `min` and `s` of the time of Canadian French would. Punctuation and spaces mean nothing to
 * day.js, so the pattern of most locales reads plainly, such as `MM/DD/YYYY h:mm:ss A`.
 *
 * A space of any kind is written as a plain one. A locale may separate the time from its day period by a narrow
 * no-break space, as newer browsers do for English, and a reader types a plain space, which the pattern then reads.
 */
const literal = (value: string): string => {
    const text = value.replace(/\s/g, ' ')
    return /\p{L}/u.test(text) ? `[${text}]` : text
}

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

/** The patterns of day.js a date alone and a date with its time are written in. */
export interface DateFormats {
    date: string
    dateTime: string
}

/** The patterns of a date alone and of a time of day alone, as the regional settings of the browser write them. */
interface SystemPatterns {
    date: string
    time: string
}

/** The patterns of the regional settings of the browser, once the first date has been written in them. */
let system: SystemPatterns | undefined

/**
 * The patterns of the regional settings of the browser: the formats a reader who picked none writes in.
 *
 * The browser keeps its regional settings while the page is open, so they are read once.
 */
export const systemPatterns = (): SystemPatterns => {
    system ??= { date: datePickerFormatForLocale(), time: timeFormatForLocale() }
    return system
}

/** The patterns of each pair of formats picked, worked out once for every date written in them. */
const PICKED = new Map<string, DateFormats>()

/**
 * The patterns of the Date Format and the Time Format of **My Settings**, each the one of the regional settings of
 * the browser while it is empty. A date with its time is the date, then the time.
 *
 * They are worked out once for each pair: a screen writing thousands of dates pays for them once.
 */
const patternsOf = (dateFormat: string, timeFormat: string): DateFormats =>
    cached(PICKED, `${dateFormat}\n${timeFormat}`, () => {
        const date = dateFormat || systemPatterns().date
        return { date, dateTime: `${date} ${timeFormat || systemPatterns().time}` }
    })

/**
 * The patterns dates are written in now, for a call that is handed none.
 *
 * It costs no more than reading the settings, so a function writing a date may call it for every date it writes.
 */
export const formats = (): DateFormats => {
    const { dateFormat, timeFormat } = useSettingsStore.getState().settings
    return patternsOf(dateFormat, timeFormat)
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
    const dateFormat = useSettingsStore(state => state.settings.dateFormat)
    const timeFormat = useSettingsStore(state => state.settings.timeFormat)
    return patternsOf(dateFormat, timeFormat)
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
 * The moment of a restored revision as its commit comment names it, the way the server writes the comments it builds:
 * `06/22/2020 09:02:42 PM`, in UTC.
 *
 * A comment is text stored in the repository and read by anyone, so it takes neither the formats of the reader nor
 * a time zone, and is written in English. Empty for a missing or unreadable value.
 */
export const commentMoment = (value?: string): string => {
    const date = dayjs(value)
    return value && date.isValid() ? date.utc().locale('en').format('MM/DD/YYYY hh:mm:ss A') : ''
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

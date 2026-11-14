import dayjs from 'dayjs'
import { DEFAULT_USER_SETTINGS, type UserSettings, useSettingsStore } from '../store/settingsStore'
import {
    commentMoment,
    datePickerFormat,
    datePickerFormatForLocale,
    formatDate,
    formatDateTime,
    formatLocalDateTime,
    isLocalDateTime,
    timeFormatForLocale,
} from './dateFormat'

/** The pattern of a date and its time in the regional settings of the browser the tests run in. */
const SYSTEM = () => `${datePickerFormatForLocale()} ${timeFormatForLocale()}`

/** Runs the checks with the given settings in force, and the defaults back after. */
const withSettings = (settings: Partial<UserSettings>, check: () => void) => {
    useSettingsStore.setState({ settings: { ...DEFAULT_USER_SETTINGS, ...settings } })
    try {
        check()
    } finally {
        useSettingsStore.setState({ settings: DEFAULT_USER_SETTINGS })
    }
}

describe('dateFormat', () => {
    it('writes a date and its time as the regional settings of the browser do, by default', () => {
        expect(DEFAULT_USER_SETTINGS.dateFormat).toBe('')
        expect(DEFAULT_USER_SETTINGS.timeFormat).toBe('')
        expect(formatDateTime('2026-07-15T12:00:38')).toBe(dayjs('2026-07-15T12:00:38').format(SYSTEM()))
        expect(formatDate('2026-07-15T12:00:38')).toBe(dayjs('2026-07-15T12:00:38').format(datePickerFormatForLocale()))
    })

    // Revisions, deployments and locks are routinely made within the same minute; without seconds
    // two of them read alike and there is nothing to tell them apart. See EPBDS-16458.
    it('shows the time down to the second, on either clock', () => {
        expect(formatDateTime('2026-07-15T12:00:38Z')).toMatch(/\d:\d{2}:38\b/)
        withSettings({ dateFormat: 'MMM D, YYYY', timeFormat: 'HH:mm:ss' }, () =>
            expect(formatDateTime('2026-07-15T12:00:38Z')).toMatch(/\d:\d{2}:38\b/))
    })

    // A table of thousands of date cells read the regional settings anew for every cell, and the page hung for
    // seconds. See EPBDS-17096.
    it('reads the regional settings of the browser once, whatever the number of dates written', () => {
        const value = '2026-07-15T12:00:38'
        const system = dayjs(value).format(SYSTEM())
        expect(formatDateTime(value)).toBe(system)
        const parts = vi.spyOn(Intl.DateTimeFormat.prototype, 'formatToParts')
        try {
            for (let written = 0; written < 3; written++) {
                expect(formatDateTime(value)).toBe(system)
                expect(formatLocalDateTime(value)).toBe(system)
            }
            withSettings({ dateFormat: 'DD.MM.YYYY', timeFormat: 'h:mm:ss A' }, () =>
                expect(formatDate(value)).toBe('15.07.2026'))
            expect(formatDateTime(value)).toBe(system)
            expect(parts).not.toHaveBeenCalled()
        } finally {
            parts.mockRestore()
        }
    })

    it('returns null for a missing date-time value', () => {
        expect(formatDateTime(undefined)).toBeNull()
        expect(formatDateTime('')).toBeNull()
    })

    it('returns the raw string when the date-time cannot be parsed', () => {
        expect(formatDateTime('not-a-date')).toBe('not-a-date')
    })

    it('returns the raw string when the date cannot be parsed', () => {
        expect(formatDate('????-??-??')).toBe('????-??-??')
    })

    it('writes every date in the format and on the clock the reader picked', () => {
        withSettings({ dateFormat: 'DD.MM.YYYY', timeFormat: 'HH:mm:ss' }, () => {
            expect(formatDate('2020-07-01T13:30:00')).toBe('01.07.2020')
            expect(formatDateTime('2020-07-01T13:30:00')).toBe('01.07.2020 13:30:00')
        })
        withSettings({ dateFormat: 'MMM D, YYYY', timeFormat: 'h:mm:ss A' }, () =>
            expect(formatDateTime('2020-07-01T13:30:00')).toBe('Jul 1, 2020 1:30:00 PM'))
    })

    it('formats a date and time written with no time zone without shifting it', () => {
        withSettings({ dateFormat: 'MMM D, YYYY', timeFormat: 'h:mm:ss A' }, () => {
            // the value names an hour on the calendar, so the browser time zone does not move it
            expect(formatLocalDateTime('2020-07-01T10:30:00.000')).toBe('Jul 1, 2020 10:30:00 AM')
            expect(formatLocalDateTime('2020-07-01T23:59:59')).toBe('Jul 1, 2020 11:59:59 PM')
        })
        expect(formatLocalDateTime('2020-07-01T10:30:00.000')).toBe(dayjs('2020-07-01T10:30:00').format(SYSTEM()))
    })

    it('shows the date alone when a date and time written with no time zone holds no time of day', () => {
        withSettings({ dateFormat: 'DD.MM.YYYY', timeFormat: 'h:mm:ss A' }, () =>
            expect(formatLocalDateTime('2020-01-01T00:00:00.000')).toBe('01.01.2020'))
    })

    it('tells a date and time written with no time zone from a moment and from any other text', () => {
        expect(isLocalDateTime('2020-07-01T10:30:00.000')).toBe(true)
        expect(isLocalDateTime('2020-07-01T10:30')).toBe(true)
        expect(isLocalDateTime('2020-07-01T10:30:00Z')).toBe(false)
        expect(isLocalDateTime('2020-07-01')).toBe(false)
        expect(isLocalDateTime(42)).toBe(false)
    })

    it('returns the raw string when a date and time written with no time zone cannot be parsed', () => {
        expect(formatLocalDateTime('not-a-date')).toBe('not-a-date')
    })

    it('names the moment of a commit comment in UTC and in English, whatever the zone it is given in', () => {
        expect(commentMoment('2020-06-23T00:02:42.517+03:00')).toBe('06/22/2020 09:02:42 PM')
        expect(commentMoment(undefined)).toBe('')
        expect(commentMoment('not-a-date')).toBe('')
    })

    it('takes the date part order and separators of a locale', () => {
        expect(datePickerFormatForLocale('en-US')).toBe('MM/DD/YYYY')
        expect(datePickerFormatForLocale('en-GB')).toBe('DD/MM/YYYY')
        expect(datePickerFormatForLocale('de-DE')).toBe('DD.MM.YYYY')
    })

    it('takes the time of day of a locale, its clock and its day period', () => {
        expect(timeFormatForLocale('en-US')).toBe('h:mm:ss A')
        expect(timeFormatForLocale('de-DE')).toBe('HH:mm:ss')
    })

    it('keeps a separator holding letters as it is, so its letters stand for no part of the date', () => {
        // Canadian French writes 13:05:09 as "13 h 05 min 09 s".
        const pattern = timeFormatForLocale('fr-CA')
        expect(pattern).toBe('HH[ h ]mm[ min ]ss[ s]')
        expect(dayjs('2006-11-22T13:05:09').format(pattern)).toBe('13 h 05 min 09 s')
        expect(datePickerFormatForLocale('bg-BG')).toBe('DD.MM.YYYY[ г.]')
    })

    it('takes the format of the regional settings for whichever of the date and the time was not picked', () => {
        withSettings({ dateFormat: 'DD.MM.YYYY' }, () => {
            expect(formatDate('2020-07-01T13:30:00')).toBe('01.07.2020')
            expect(formatDateTime('2020-07-01T13:30:00'))
                .toBe(dayjs('2020-07-01T13:30:00').format(`DD.MM.YYYY ${timeFormatForLocale()}`))
        })
        withSettings({ timeFormat: 'HH:mm:ss' }, () => {
            expect(formatDate('2020-07-01T13:30:00')).toBe(dayjs('2020-07-01').format(datePickerFormatForLocale()))
            expect(formatDateTime('2020-07-01T13:30:00'))
                .toBe(dayjs('2020-07-01T13:30:00').format(`${datePickerFormatForLocale()} HH:mm:ss`))
        })
    })

    it('writes a space of any kind a locale separates the parts by as a plain one, the space a reader types', () => {
        // Newer browsers separate the time of an English locale from its day period by a narrow no-break space.
        const parts = vi.spyOn(Intl.DateTimeFormat.prototype, 'formatToParts').mockReturnValue([
            { type: 'hour', value: '1' },
            { type: 'literal', value: ':' },
            { type: 'minute', value: '05' },
            { type: 'literal', value: ':' },
            { type: 'second', value: '09' },
            { type: 'literal', value: ' ' },
            { type: 'dayPeriod', value: 'PM' },
        ])
        try {
            expect(timeFormatForLocale('en-US')).toBe('h:mm:ss A')
        } finally {
            parts.mockRestore()
        }
    })

    it('shows a date in a picker as the dates are written', () => {
        expect(datePickerFormat()).toBe(datePickerFormatForLocale())
        expect(datePickerFormat(true)).toBe(SYSTEM())
        withSettings({ dateFormat: 'DD.MM.YYYY', timeFormat: 'HH:mm:ss' }, () => {
            expect(datePickerFormat()).toBe('DD.MM.YYYY')
            expect(datePickerFormat(true)).toBe('DD.MM.YYYY HH:mm:ss')
        })
    })
})

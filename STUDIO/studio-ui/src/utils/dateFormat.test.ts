import 'dayjs/locale/en-gb'
import i18next from 'i18next'
import { commentMoment, formatDate, formatDateTime, formatLocalDateTime, isLocalDateTime } from './dateFormat'

describe('dateFormat', () => {
    it('expands the localized date-time pattern', () => {
        const formatted = formatDateTime('2026-07-15T12:00:00Z')

        expect(formatted).toContain('2026')
        expect(formatted).toContain('Jul')
        // a literal pattern in the output means the localizedFormat plugin is not registered
        expect(formatted).not.toContain('LTS')
    })

    // Revisions, deployments and locks are routinely made within the same minute; without seconds
    // two of them read alike and there is nothing to tell them apart. See EPBDS-16458.
    it('shows the time down to the second', () => {
        expect(formatDateTime('2026-07-15T12:00:38Z')).toMatch(/\d:\d{2}:38\b/)
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

    it('expands the localized date pattern', () => {
        const formatted = formatDate('2026-07-15T12:00:00Z')

        expect(formatted).toContain('2026')
        expect(formatted).toContain('Jul')
        expect(formatted).not.toContain('ll')
    })

    it('formats a date and time written with no time zone without shifting it', () => {
        // the value names an hour on the calendar, so the browser time zone does not move it
        expect(formatLocalDateTime('2020-07-01T10:30:00.000')).toMatch(/^Jul 1, 2020 10:30:00\sAM$/)
        expect(formatLocalDateTime('2020-07-01T23:59:59')).toMatch(/^Jul 1, 2020 11:59:59\sPM$/)
    })

    it('shows the date alone when a date and time written with no time zone holds no time of day', () => {
        expect(formatLocalDateTime('2020-01-01T00:00:00.000')).toBe('Jan 1, 2020')
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

    it('names the moment of a commit comment in UTC to the second, whatever the zone it is given in', () => {
        expect(commentMoment('2020-06-23T00:02:42.517+03:00')).toBe('2020-06-22T21:02:42Z')
        expect(commentMoment(undefined)).toBe('')
        expect(commentMoment('not-a-date')).toBe('')
    })

    it('names the moment of a commit comment in the Java pattern the template gives, in UTC', () => {
        const moment = '2020-06-23T00:02:42.517+03:00'
        expect(commentMoment(moment, 'yyyy-MM-dd HH:mm')).toBe('2020-06-22 21:02')
        expect(commentMoment(moment, "dd.MM.yy 'at' h:mm a")).toBe('22.06.20 at 9:02 PM')
        expect(commentMoment(moment, "yyyy-MM-dd'T'HH:mm:ss.SSS")).toBe('2020-06-22T21:02:42.517')
        expect(commentMoment(moment, 'EEE, d MMM yyyy')).toBe('Mon, 22 Jun 2020')
    })

    it('writes the letters the server writes, and text in quotes, as the server does', () => {
        const moment = '2020-06-22T21:02:42.517Z'
        expect(commentMoment(moment, "EEE, d MMM yyyy h:mm:ss.SSS a 'o''clock'''"))
            .toBe("Mon, 22 Jun 2020 9:02:42.517 PM o'clock'")
        expect(commentMoment(moment, 'ss.S|ss.SS')).toBe('42.5|42.51')
    })

    it('writes the moment in ISO 8601 for a pattern of letters the server does not write', () => {
        const moment = '2020-06-22T21:02:42.517Z'
        for (const pattern of ['HH:mm z', 'D', 'hh aa', "yyyy 'year"]) {
            expect(commentMoment(moment, pattern)).toBe('2020-06-22T21:02:42Z')
        }
    })

    it('follows the application language', () => {
        const emitter = i18next as unknown as { emit: (event: string, language: string) => void }
        emitter.emit('languageChanged', 'en-GB')
        try {
            // the British format puts the day first without a comma
            expect(formatDate('2026-07-15T12:00:00Z')).toMatch(/^\d{1,2} Jul 2026$/)
        } finally {
            emitter.emit('languageChanged', 'en')
        }
    })
})

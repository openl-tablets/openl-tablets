import dayjs from 'dayjs'
import { datePickerFormatForLocale, formatDateValue, ISO_DATE_FORMAT, parseDateValue } from './dateValue'

describe('dateValue', () => {
    describe('datePickerFormatForLocale', () => {
        it('uses the date part order and separators of the user locale', () => {
            expect(datePickerFormatForLocale('en-US')).toBe('MM[/]DD[/]YYYY')
            expect(datePickerFormatForLocale('en-GB')).toBe('DD[/]MM[/]YYYY')
            expect(datePickerFormatForLocale('de-DE')).toBe('DD[.]MM[.]YYYY')
        })
    })

    describe('parseDateValue', () => {
        it('reads the ISO date a value is carried in', () => {
            expect(parseDateValue('2009-01-01')?.format(ISO_DATE_FORMAT)).toBe('2009-01-01')
        })

        it('reads a value that names a moment of the day', () => {
            expect(parseDateValue('2026-08-21T14:33')?.format(ISO_DATE_FORMAT)).toBe('2026-08-21')
            expect(parseDateValue('2026-08-21T14:33:12')?.second()).toBe(12)
            expect(parseDateValue('2026-08-21T14:33:12.345')?.millisecond()).toBe(345)
        })

        it('answers nothing for a text that is not a date value', () => {
            expect(parseDateValue('01/01/2009 12:00 AM')).toBeNull()
            expect(parseDateValue('2009-13-45')).toBeNull()
            expect(parseDateValue('whenever')).toBeNull()
            expect(parseDateValue('')).toBeNull()
            expect(parseDateValue(null)).toBeNull()
            expect(parseDateValue(42)).toBeNull()
        })
    })

    describe('formatDateValue', () => {
        it('writes a picked day as the day alone', () => {
            expect(formatDateValue(dayjs('2010-06-30'))).toBe('2010-06-30')
            expect(formatDateValue(dayjs('2010-06-30'), '2009-01-01')).toBe('2010-06-30')
        })

        it('keeps the time of day the value it replaces carried', () => {
            // The picker chooses a day, so a date naming a moment must not be moved to midnight by touching it.
            expect(formatDateValue(dayjs('2010-06-30'), '2009-12-31T23:59')).toBe('2010-06-30T23:59')
            expect(formatDateValue(dayjs('2010-06-30'), '2009-12-31T23:59:30')).toBe('2010-06-30T23:59:30')
            expect(formatDateValue(dayjs('2010-06-30'), '2009-12-31T23:59:30.345'))
                .toBe('2010-06-30T23:59:30.345')
        })

        it('carries nothing over from a value that is not a date', () => {
            expect(formatDateValue(dayjs('2010-06-30'), 'whenever')).toBe('2010-06-30')
        })
    })
})

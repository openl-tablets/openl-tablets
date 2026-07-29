import { datePickerFormatForLocale } from './dateValue'

describe('dateValue', () => {
    describe('datePickerFormatForLocale', () => {
        it('uses the date part order and separators of the user locale', () => {
            expect(datePickerFormatForLocale('en-US')).toBe('MM[/]DD[/]YYYY')
            expect(datePickerFormatForLocale('en-GB')).toBe('DD[/]MM[/]YYYY')
            expect(datePickerFormatForLocale('de-DE')).toBe('DD[.]MM[.]YYYY')
        })
    })
})

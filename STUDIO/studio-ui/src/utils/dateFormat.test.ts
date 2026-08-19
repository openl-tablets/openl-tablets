import 'dayjs/locale/en-gb'
import i18next from 'i18next'
import { formatDate, formatDateTime } from './dateFormat'

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

    it('expands the localized date pattern', () => {
        const formatted = formatDate('2026-07-15T12:00:00Z')

        expect(formatted).toContain('2026')
        expect(formatted).toContain('Jul')
        expect(formatted).not.toContain('ll')
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

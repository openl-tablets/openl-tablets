import { describe, expect, it } from 'vitest'
import i18n from './i18n'

describe('i18n', () => {
    it('keeps interpolated values as typed, so names with slashes or quotes read correctly', () => {
        i18n.addResource('en', 'test', 'moved', 'The project moved to branch "{{name}}"')

        expect(i18n.t('test:moved', { name: 'Example2-CorporateRating/openl/2026' }))
            .toBe('The project moved to branch "Example2-CorporateRating/openl/2026"')
        expect(i18n.t('test:moved', { name: 'Rates & Rules' }))
            .toBe('The project moved to branch "Rates & Rules"')
    })
})

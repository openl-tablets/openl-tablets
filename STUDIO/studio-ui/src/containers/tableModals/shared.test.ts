import { sheetNameFrom } from './shared'

describe('shared', () => {
    describe('sheetNameFrom', () => {
        it('trims surrounding spaces, then clips to the length Excel accepts', () => {
            expect(sheetNameFrom('  Spaced  ')).toBe('Spaced')
            // Trimming first, so leading spaces do not eat into the 31 characters a worksheet name may hold.
            expect(sheetNameFrom(`  ${'A'.repeat(40)}`)).toBe('A'.repeat(31))
        })
    })
})

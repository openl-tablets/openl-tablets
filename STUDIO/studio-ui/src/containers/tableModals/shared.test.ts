import { property } from './propertyFixture'
import { isValidPropertyValue, sheetNameFrom, toPropertyGroups } from './shared'

describe('shared', () => {
    describe('sheetNameFrom', () => {
        it('trims surrounding spaces, then clips to the length Excel accepts', () => {
            expect(sheetNameFrom('  Spaced  ')).toBe('Spaced')
            // Trimming first, so leading spaces do not eat into the 31 characters a worksheet name may hold.
            expect(sheetNameFrom(`  ${'A'.repeat(40)}`)).toBe('A'.repeat(31))
        })
    })

    describe('toPropertyGroups', () => {
        it('lists display names under their groups, in the order Table Details lists them', () => {
            const groups = toPropertyGroups([
                property({ name: 'buildPhase', displayName: 'Build Phase', group: 'Dev' }),
                property({ name: 'version', displayName: 'Version', group: 'Version' }),
                property({ name: 'state', displayName: 'US States', group: 'Business Dimension' }),
                property({ name: 'category', displayName: 'Category', group: 'Info' }),
                property({ name: 'lob', displayName: 'LOB', group: 'Business Dimension' }),
            ])

            expect(groups.map(group => group.label)).toEqual(['Info', 'Business Dimension', 'Version', 'Dev'])
            // Within a group the display names are in alphabetical order, and each option writes its technical name.
            expect(groups[1]!.options).toEqual([
                { label: 'LOB', value: 'lob' },
                { label: 'US States', value: 'state' },
            ])
        })

        it('lists a group it does not know after the ones it does', () => {
            const groups = toPropertyGroups([
                property({ name: 'custom', group: 'Custom' }),
                property({ name: 'category', group: 'Info' }),
            ])

            expect(groups.map(group => group.label)).toEqual(['Info', 'Custom'])
        })
    })

    describe('isValidPropertyValue', () => {
        const id = property({ name: 'id', pattern: '([a-zA-Z_][a-zA-Z0-9_]*)' })

        it('accepts a value matching the pattern the property states, whole', () => {
            expect(isValidPropertyValue(id, 'rate_2')).toBe(true)
            expect(isValidPropertyValue(id, ' rate_2 ')).toBe(true)
            expect(isValidPropertyValue(id, '2ndRate')).toBe(false)
            // Anchored, so a value that merely contains a legal identifier is refused.
            expect(isValidPropertyValue(id, 'rate-2')).toBe(false)
        })

        it('accepts any value for a property stating no pattern', () => {
            expect(isValidPropertyValue(property({ name: 'lob' }), 'anything')).toBe(true)
            expect(isValidPropertyValue(undefined, 'anything')).toBe(true)
        })
    })
})

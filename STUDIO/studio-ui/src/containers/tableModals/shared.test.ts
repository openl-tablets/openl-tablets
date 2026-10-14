import { property } from './propertyFixture'
import { IDENTIFIER, isValidPropertyValue, sheetNameFrom, toModuleOptions, toPropertyGroups } from './shared'

describe('shared', () => {
    describe('IDENTIFIER', () => {
        it.each(['NewTable', '_rate', '$total', '€Rate', 'Ⅻ', '면간거리', 'имя', 'नाम', 'a\u0301', 'Hour24', 'a\u200d'])(
            'accepts the Java identifier %s',
            name => expect(IDENTIFIER.test(name)).toBe(true)
        )

        it.each(['24Hour', 'New Table', 'Rate-A', 'a.b', '・', 'サービス・コード', '\u0301a', '\u200da', ''])(
            'refuses %j, which is not a Java identifier',
            name => expect(IDENTIFIER.test(name)).toBe(false)
        )
    })

    describe('sheetNameFrom', () => {
        it('trims surrounding spaces, then clips to the length Excel accepts', () => {
            expect(sheetNameFrom('  Spaced  ')).toBe('Spaced')
            // Trimming first, so leading spaces do not eat into the 31 characters a worksheet name may hold.
            expect(sheetNameFrom(`  ${'A'.repeat(40)}`)).toBe('A'.repeat(31))
        })
    })

    describe('toModuleOptions', () => {
        it('offers the named modules a table can be written to, leaving a Markdown module to the file editor', () => {
            const options = toModuleOptions([
                { name: 'Rates', path: 'rules/Rates.xlsx' },
                { name: 'Greetings', path: 'rules/Greetings.md' },
                { name: 'Rating', path: 'rules/Rating' },
                { path: 'rules/Unnamed.xlsx' },
            ])

            expect(options).toEqual([{ name: 'Rates', path: 'rules/Rates.xlsx' }])
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

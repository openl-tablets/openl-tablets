import { describe, expect, it } from 'vitest'
import { highlight, type CodePiece } from './codeHighlight'
import { THEME_ORDER, variantOf } from '../styles/themes'
import { hex } from '../testing/theme'

/** The style of the first piece of a block whose text is the one given. */
const styleOf = (pieces: CodePiece[] | undefined, text: string) => pieces?.find(piece => piece.text === text)?.style

describe('highlight', () => {
    it('splits code into the tokens of its language, the line breaks kept', () => {
        const pieces = highlight('a: 1\nb: true\n', 'yaml', 'standard', false)

        expect(pieces?.map(({ text }) => text).join('')).toBe('a: 1\nb: true\n')
        expect(pieces?.filter(({ text }) => text === '\n')).toHaveLength(2)
        expect(pieces?.map(({ at }) => at)).toEqual(pieces?.map((_, index) =>
            pieces.slice(0, index).reduce((length, { text }) => length + text.length, 0)))
    })

    it('draws each token in the style the code editor of the theme gives it', () => {
        const pieces = highlight('public class Rules {}', 'java', 'dracula', true)

        expect(styleOf(pieces, 'public')).toEqual({ color: '#ff79c6' })
    })

    it('draws the same code anew in another theme', () => {
        const dark = highlight('public class Rules {}', 'java', 'solarized', true)
        const light = highlight('public class Rules {}', 'java', 'standard', false)

        expect(styleOf(dark, 'public')).toEqual({ color: '#859900' })
        expect(styleOf(light, 'public')).toEqual({ color: '#708' })
    })

    it('lays the styles of every rule a token matches over each other, the later rule winning', () => {
        // The name of a close tag that does not match its open tag is both a tag name and invalid.
        const pieces = highlight('<a></b>', 'xml', 'basic', true)

        expect(styleOf(pieces, 'a')).toEqual({ color: '#fda331' })
        expect(styleOf(pieces, 'b')).toEqual({ color: '#B9D2FF', borderBottom: '1px dotted #fc6d24' })
    })

    it('leaves the text between the tokens unstyled', () => {
        const pieces = highlight('a: 1', 'yaml', 'nord', true)

        expect(styleOf(pieces, ' ')).toBeUndefined()
    })

    it('does not split code in a language the guides do not use', () => {
        expect(highlight('IDENTIFICATION DIVISION.', 'cobol', 'nord', true)).toBeUndefined()
    })

    it.each(THEME_ORDER)('draws a value in the hues the theme %s gives a parameter value', name => {
        [false, true].forEach(isDarkMode => {
            const pieces = highlight('{"name": "text", "count": 1, "flag": true}', 'json', name, isDarkMode)
            const { syntax } = variantOf(name, isDarkMode)

            expect(hex(styleOf(pieces, '"name"')?.color)).toBe(syntax.name)
            expect(hex(styleOf(pieces, '"text"')?.color)).toBe(syntax.string)
            expect(hex(styleOf(pieces, '1')?.color)).toBe(syntax.number)
            expect(hex(styleOf(pieces, 'true')?.color)).toBe(syntax.boolean)
        })
    })

    describe('the code of OpenL tables', () => {
        const { syntax } = variantOf('standard', false)
        /** The texts of the pieces of an expression, each with the colour it takes. */
        const tokens = (code: string, language = 'openl') => highlight(code, language, 'standard', false)
            ?.filter(piece => piece.text.trim() !== '')
            .map(piece => [piece.text, piece.style?.color === undefined ? undefined : hex(piece.style.color)])

        it('reads an expression as the code editor reads Java', () => {
            const pieces = highlight('= driver.age > 18 && region == "NY"', 'openl', 'standard', false)

            expect(hex(styleOf(pieces, '18')?.color)).toBe(syntax.number)
            expect(hex(styleOf(pieces, '"NY"')?.color)).toBe(syntax.string)
            expect(styleOf(pieces, 'driver')).toBeUndefined()
        })

        it('reads a business literal as one number, its separators included', () => {
            const numbers = tokens('1,000.50 + 10% * 1K - 2.5M + 2026-10-15 + 12:30 + 1e3')
                ?.filter(([, colour]) => colour === syntax.number).map(([text]) => text)

            expect(numbers).toEqual(['1,000.50', '10%', '1K', '2.5M', '2026-10-15', '12:30', '1e3'])
        })

        it('reads the bounds of a range as numbers and a name starting with $ as a name', () => {
            const pieces = tokens('[18..25) + $Premium + $100')

            expect(pieces).toContainEqual(['18', syntax.number])
            expect(pieces).toContainEqual(['25', syntax.number])
            expect(pieces?.find(([text]) => text === '$Premium')?.[1]).not.toBe(syntax.number)
            expect(pieces?.find(([text]) => text === '$100')?.[1]).not.toBe(syntax.number)
        })

        it('reads an operator OpenL writes in words as one keyword', () => {
            const pieces = highlight('age is less than 18 and region is in regions or age is no more than 65',
                'openl', 'standard', false)
            const keyword = styleOf(highlight('if', 'openl', 'standard', false), 'if')

            expect(keyword).toBeDefined()
            for (const operator of ['is less than', 'and', 'is in', 'or', 'is no more than']) {
                expect(styleOf(pieces, operator)).toEqual(keyword)
            }
            expect(styleOf(pieces, 'age')).toBeUndefined()
        })

        it('reads the operations of a TBasic table in any case, and an expression of it as OpenL', () => {
            const keyword = styleOf(highlight('if', 'openl', 'standard', false), 'if')

            for (const operation of ['IF', 'END', 'SET', 'FOR', 'EACH', 'return', 'Sub']) {
                expect(styleOf(highlight(`${operation} x`, 'tbasic', 'standard', false), operation)).toEqual(keyword)
            }
            expect(styleOf(highlight('SET x', 'openl', 'standard', false), 'SET')).toBeUndefined()
            expect(hex(styleOf(highlight('x = 10%', 'tbasic', 'standard', false), '10%')?.color)).toBe(syntax.number)
        })
    })
})

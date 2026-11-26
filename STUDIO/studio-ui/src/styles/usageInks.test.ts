import { describe, expect, it } from 'vitest'
import { roleColoursOf } from 'components/RawTableGrid.styles'
import { colourDistance, contrastRatio, opaque, readableOn } from 'styles/colorMath'
import { THEME_ORDER, variantOf } from 'styles/themes'
import { tokenFor } from 'testing/theme'
import { usageInksOf, WORKBOOK_USAGE_INKS } from './usageInks'

const VARIANTS = THEME_ORDER.flatMap(name => [false, true].map(isDarkMode => ({
    name,
    isDarkMode,
    appearance: isDarkMode ? 'dark' : 'light',
})))

/** The fills of the Studio look, opaque on the surface of the theme, the surface first. */
const groundsOf = (token: ReturnType<typeof tokenFor>) => {
    const roles = roleColoursOf(token)
    return [token.colorBgContainer, roles.inputTitleFill, roles.outputTitleFill, roles.outputFill]
        .map(fill => opaque(fill, token.colorBgContainer))
}

describe('usageInks', () => {
    it.each(VARIANTS)('draws a rule, a datatype and a field readable and apart in $name $appearance',
        ({ name, isDarkMode }) => {
            const token = tokenFor(name, isDarkMode)
            const grounds = groundsOf(token)
            const inks = usageInksOf(variantOf(name, isDarkMode).syntax, token, grounds)
            const text = opaque(token.colorText, token.colorBgContainer)

            for (const ink of Object.values(inks)) {
                for (const ground of grounds) {
                    expect(contrastRatio(ink, ground)).toBeGreaterThanOrEqual(4.5)
                }
            }
            // Each kind reads apart from the text and from the other two.
            const colours = [text, inks.rule, inks.datatype, inks.field]
            colours.forEach((colour, index) => colours.slice(index + 1)
                .forEach(other => expect(colourDistance(colour, other)).toBeGreaterThanOrEqual(40)))
        })

    it('takes the hues the code editor of the theme gives a method called, a type and a member', () => {
        const token = tokenFor('vscode', true)
        const inks = usageInksOf(variantOf('vscode', true).syntax, token, groundsOf(token))

        // Dark Modern writes each of them in a hue of its own, readable on its fills already.
        expect(inks).toEqual({ rule: '#dcdcaa', datatype: '#4ec9b0', field: '#9cdcfe' })
    })

    it('falls back on success and the faintest readable grey where the editor writes kinds alike', () => {
        const token = tokenFor('standard', false)
        const grounds = groundsOf(token)
        const readable = (colour: string) => readableOn(opaque(colour, token.colorBgContainer), grounds)
        const same = '#22863a'
        const inks = usageInksOf({ call: same, type: same, field: same }, token, grounds)

        expect(inks).toEqual({
            rule: readable(same),
            datatype: readable(token.colorSuccessText),
            field: readable(token.colorTextTertiary),
        })
    })

    it('falls back on a link where the editor writes a rule in its plain text, or names no hue for it', () => {
        const token = tokenFor('standard', false)
        const grounds = groundsOf(token)
        const link = readableOn(token.colorLink, grounds)

        expect(usageInksOf({ call: opaque(token.colorText, token.colorBgContainer) }, token, grounds).rule).toBe(link)
        expect(usageInksOf({}, token, grounds).rule).toBe(link)
    })

    it('turns to the hues of the values where neither the own colours of a kind nor its greys stand apart', () => {
        // A text as grey as the faintest readable grey leaves a field no grey of its own.
        const token = { ...tokenFor('standard', false), colorText: '#767676', colorTextSecondary: '#767676' }
        const grounds = ['#ffffff']
        const inks = usageInksOf({ call: '#0000ff', type: '#008000', string: '#aa1111' }, token, grounds)

        expect(inks.field).toBe('#aa1111')
    })

    it('writes a workbook in the blue, the dark green and the dark grey of Excel, readable on its paper', () => {
        for (const ink of Object.values(WORKBOOK_USAGE_INKS)) {
            expect(contrastRatio(ink, '#ffffff')).toBeGreaterThanOrEqual(4.5)
        }
        expect(WORKBOOK_USAGE_INKS).toEqual({ rule: '#0070c0', datatype: '#006100', field: '#595959' })
    })
})

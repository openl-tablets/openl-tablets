import { describe, expect, it } from 'vitest'
import { excelTint, hexOf, OFFICE_THEME, STANDARD_COLOURS } from './excelColours'

/**
 * Each theme colour of Office, then each tint and the colour Excel draws, as the backend tints it
 * (`PoiExcelHelper.applyTint`): the shades the palette of Excel offers, and a few more.
 */
const DRAWN = `
    FFFFFF 0.8:ffffff 0.6:ffffff 0.4:ffffff -0.25:bfbfbf -0.5:808080 -0.05:f2f2f2 -0.15:d9d9d9 -0.35:a6a6a6 -0.1:e6e6e6 -0.75:404040 -0.9:1a1a1a 0.5:ffffff 0.35:ffffff 0.25:ffffff 0.15:ffffff 0.05:ffffff
    000000 0.8:cccccc 0.6:999999 0.4:666666 -0.25:000000 -0.5:000000 -0.05:000000 -0.15:000000 -0.35:000000 -0.1:000000 -0.75:000000 -0.9:000000 0.5:808080 0.35:595959 0.25:404040 0.15:262626 0.05:0d0d0d
    E7E6E6 0.8:fafafa 0.6:f4f4f4 0.4:f0f0f0 -0.25:aeaaaa -0.5:757171 -0.05:dcdada -0.15:c5c2c2 -0.35:989494 -0.1:d0cece -0.75:3a3838 -0.9:161616 0.5:f2f2f2 0.35:efefef 0.25:ececec 0.15:eaeaea 0.05:e8e8e8
    44546A 0.8:d6dce4 0.6:acb9ca 0.4:8497b0 -0.25:333f4f -0.5:222b35 -0.05:404f64 -0.15:394759 -0.35:2d3644 -0.1:3d4b5f -0.75:11141a -0.9:06090b 0.5:99a8bd 0.35:7a8ea9 0.25:657c9c 0.15:576c88 0.05:4a5b73
    4472C4 0.8:d9e1f2 0.6:b4c6e7 0.4:8ea9db -0.25:305496 -0.5:203764 -0.05:3c69bd -0.15:355faa -0.35:284782 -0.1:3864b4 -0.75:101c32 -0.9:060b13 0.5:a2b8e1 0.35:84a2d9 0.25:7395d2 0.15:5f85cd 0.05:4b77c7
    ED7D31 0.8:fce4d6 0.6:f8cbad 0.4:f4b084 -0.25:c65911 -0.5:833c0c -0.05:ec7424 -0.15:de6514 -0.35:aa4d0f -0.1:eb6b16 -0.75:411d05 -0.9:1a0c02 0.5:f5bd98 0.35:f3a978 0.25:f19d65 0.15:ef8f50 0.05:ee833c
    A5A5A5 0.8:ededed 0.6:dbdbdb 0.4:c9c9c9 -0.25:7b7b7b -0.5:525252 -0.05:9c9c9c -0.15:8b8b8b -0.35:6a6a6a -0.1:949494 -0.75:282828 -0.9:101010 0.5:d1d1d1 0.35:c4c4c4 0.25:bbbbbb 0.15:b1b1b1 0.05:a9a9a9
    FFC000 0.8:fff2cc 0.6:ffe699 0.4:ffd966 -0.25:bf8f00 -0.5:806000 -0.05:f2b600 -0.15:d9a300 -0.35:a67c00 -0.1:e6ac00 -0.75:403000 -0.9:1a1300 0.5:ffdf80 0.35:ffd659 0.25:ffcf40 0.15:ffc926 0.05:ffc20d
    5B9BD5 0.8:ddebf7 0.6:bdd7ee 0.4:9bc2e6 -0.25:2f75b5 -0.5:1f4e78 -0.05:4e93d1 -0.15:3685cb -0.35:28659b -0.1:428bce -0.75:0f263c -0.9:060f17 0.5:acccea 0.35:93bde3 0.25:84b4df 0.15:73a9db 0.05:629fd7
    70AD47 0.8:e2efda 0.6:c6e0b4 0.4:a9d08e -0.25:548235 -0.5:375623 -0.05:6aa444 -0.15:5f923d -0.35:48702e -0.1:649b40 -0.75:1c2b11 -0.9:0b1106 0.5:b7d8a0 0.35:a0cc84 0.25:93c572 0.15:84bd5e 0.05:76b64b
`

describe('excelTint', () => {
    it('makes a colour lighter or darker to the last digit, as Excel draws it', () => {
        DRAWN.trim().split('\n').forEach(line => {
            const [colour, ...shades] = line.trim().split(' ')
            shades.forEach(shade => {
                const [tint, drawn] = shade.split(':')
                expect(excelTint(`#${colour}`, Number(tint)), `${colour} ${tint}`).toBe(`#${drawn}`)
            })
        })
    })

    it('leaves a colour as it is for a tint of less than a thousandth', () => {
        expect(excelTint('#4472C4', 0)).toBe('#4472c4')
        expect(excelTint('#4472C4', 0.0004)).toBe('#4472c4')
    })
})

describe('hexOf', () => {
    it('draws a theme colour in the colours of Office, and a standard colour as it is', () => {
        expect(hexOf({ theme: 'accent1' })).toBe(OFFICE_THEME.accent1)
        expect(hexOf({ theme: 'accent1', tint: 0.6 })).toBe('#b4c6e7')
        expect(hexOf({ theme: 'lt1', tint: -0.5 })).toBe('#808080')
        expect(hexOf({ rgb: '#C00000' })).toBe('#c00000')
    })

    it('offers the ten standard colours of Excel, from Dark Red to Purple', () => {
        expect(STANDARD_COLOURS.map(({ rgb }) => rgb)).toEqual(['#C00000', '#FF0000', '#FFC000', '#FFFF00',
            '#92D050', '#00B050', '#00B0F0', '#0070C0', '#002060', '#7030A0'])
    })
})

import { describe, expect, it } from 'vitest'
import { theme } from 'antd'
import { graphPalette, kindColor, kindRules } from './tableGraphTheme'
import { DISPATCHER_KIND } from './tableGraph'

const tokenOf = (dark: boolean) => theme.getDesignToken(dark ? { algorithm: theme.darkAlgorithm } : {})

const LIGHT = tokenOf(false)
const DARK = tokenOf(true)

describe('tableGraphTheme', () => {
    it('draws a kind in the shade the appearance gives that hue', () => {
        expect(kindColor(LIGHT, 'Rules')).toBe(LIGHT.blue6)
        expect(kindColor(DARK, 'Rules')).toBe(DARK.blue6)
        expect(kindColor(LIGHT, 'Rules')).not.toBe(kindColor(DARK, 'Rules'))
    })

    it('falls back to grey for a kind it does not know, and for a node carrying none', () => {
        expect(kindColor(LIGHT, 'Something New')).toBe(LIGHT.colorTextTertiary)
        expect(kindColor(LIGHT)).toBe(LIGHT.colorTextTertiary)
        // a name every object answers to is still a kind this version does not paint
        expect(kindColor(LIGHT, 'constructor')).toBe(LIGHT.colorTextTertiary)
    })

    it('gives the dispatcher and the data model their own hues', () => {
        expect(kindColor(LIGHT, DISPATCHER_KIND)).toBe(LIGHT.gold9)
        expect(kindColor(LIGHT, 'Datatype')).toBe(LIGHT.cyan6)
    })

    it('paints every kind it knows with a rule of its own', () => {
        const rules = kindRules(LIGHT)

        expect(rules).toContainEqual({ selector: 'node[kind = "Rules"]', style: { 'background-color': LIGHT.blue6 } })
        // a kind whose name carries a space still selects, because the selector quotes it
        expect(rules).toContainEqual({ selector: 'node[kind = "Smart Rules"]', style: { 'background-color': LIGHT.geekblue6 } })
        expect(rules.every(rule => rule.style['background-color'])).toBe(true)
    })

    it('hands the canvas real colours, since Cytoscape cannot read a custom property', () => {
        const palette = graphPalette(DARK)

        expect(Object.values(palette).every(colour => colour && !colour.includes('var('))).toBe(true)
        expect(palette.canvas).toBe(DARK.colorBgLayout)
        expect(palette.problem).toBe(DARK.colorError)
        expect(graphPalette(LIGHT).canvas).not.toBe(palette.canvas)
    })
})

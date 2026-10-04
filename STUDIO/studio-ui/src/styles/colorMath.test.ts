import { describe, expect, it } from 'vitest'
import { contrastRatio, mix, opaque } from './colorMath'

describe('colorMath', () => {
    it('moves a colour towards another by the share asked for', () => {
        expect(mix('#ffffff', '#000000', 0)).toBe('#ffffff')
        expect(mix('#ffffff', '#000000', 0.5)).toBe('#808080')
        expect(mix('#ffffff', '#000000', 1)).toBe('#000000')
    })

    it('flattens a translucent colour onto its background', () => {
        expect(opaque('rgba(0, 0, 0, 0.5)', '#ffffff')).toBe('#808080')
        expect(opaque('#123456', '#ffffff')).toBe('#123456')
    })

    it('measures the contrast of two colours as WCAG does, in either order', () => {
        expect(contrastRatio('#000000', '#ffffff')).toBeCloseTo(21)
        expect(contrastRatio('#ffffff', '#000000')).toBeCloseTo(21)
        expect(contrastRatio('#777777', '#777777')).toBe(1)
        expect(contrastRatio('#767676', '#ffffff')).toBeCloseTo(4.54, 2)
    })
})

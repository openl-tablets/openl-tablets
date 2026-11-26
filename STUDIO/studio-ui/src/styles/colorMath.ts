import { FastColor } from '@ant-design/fast-color'

/** A colour moved towards another by the given share, `0` keeping it and `1` reaching the other. */
export const mix = (color: string, towards: string, share: number): string =>
    new FastColor(color).mix(towards, share * 100).toHexString()

/** A translucent colour as it shows over a background, as one opaque colour. */
export const opaque = (color: string, background: string): string =>
    new FastColor(color).onBackground(background).toHexString()

/** The WCAG contrast ratio of two colours, from `1` for the same colour to `21` for black on white. */
export const contrastRatio = (first: string, second: string): number => {
    const one = new FastColor(first).getLuminance()
    const other = new FastColor(second).getLuminance()
    return (Math.max(one, other) + 0.05) / (Math.min(one, other) + 0.05)
}

/** How far apart two colours stand, as the distance between their red, green and blue: `0` for the same colour. */
export const colourDistance = (one: string, other: string): number => {
    const a = new FastColor(one).toRgb()
    const b = new FastColor(other).toRgb()
    return Math.hypot(a.r - b.r, a.g - b.g, a.b - b.b)
}

/** The contrast a text keeps on the grounds it is drawn on, as the text of the application does on its surfaces. */
const READABLE = 4.5

/** How far an ink is moved towards white or black at a time, until it reads. */
const STEP = 0.05

/**
 * An ink moved as little as it takes to read on every ground: towards white on a dark ground and black on a light
 * one, the first ground deciding, which keeps the hue of the ink the longest. An ink that reads already is kept.
 *
 * @param ink     the colour of the text
 * @param grounds the opaque colours the text is drawn on, the main one first
 */
export const readableOn = (ink: string, grounds: readonly string[]): string => {
    const ground = grounds[0] ?? '#ffffff'
    const text = contrastRatio(ground, '#ffffff') > contrastRatio(ground, '#000000') ? '#ffffff' : '#000000'
    for (let share = 0; share < 1; share += STEP) {
        const moved = mix(ink, text, share)
        if (grounds.every(other => contrastRatio(moved, other) >= READABLE)) {
            return moved
        }
    }
    return text
}

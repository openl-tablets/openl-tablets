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

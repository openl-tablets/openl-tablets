import type { ExcelThemeColorName, RawCellColor } from 'types/tables'

/**
 * The ten theme colours of Office 2013 - 2022, the theme of a new workbook of Excel, by the names a workbook gives
 * them. A colour picked for a cell is shown in them until the table is saved and read again in the colours of its own
 * workbook.
 */
export const OFFICE_THEME: Readonly<Record<ExcelThemeColorName, string>> = {
    lt1: '#ffffff',
    dk1: '#000000',
    lt2: '#e7e6e6',
    dk2: '#44546a',
    accent1: '#4472c4',
    accent2: '#ed7d31',
    accent3: '#a5a5a5',
    accent4: '#ffc000',
    accent5: '#5b9bd5',
    accent6: '#70ad47',
}

/** The ten standard colours of Excel, from Dark Red to Purple, each by the name Excel gives it. */
export const STANDARD_COLOURS: readonly { rgb: string, name: string }[] = [
    { rgb: '#C00000', name: 'dark_red' },
    { rgb: '#FF0000', name: 'red' },
    { rgb: '#FFC000', name: 'orange' },
    { rgb: '#FFFF00', name: 'yellow' },
    { rgb: '#92D050', name: 'light_green' },
    { rgb: '#00B050', name: 'green' },
    { rgb: '#00B0F0', name: 'light_blue' },
    { rgb: '#0070C0', name: 'blue' },
    { rgb: '#002060', name: 'dark_blue' },
    { rgb: '#7030A0', name: 'purple' },
]

/** The largest hue, luminance and saturation of the colour model of Windows. */
const HLS_MAX = 240

/** The largest red, green and blue. */
const RGB_MAX = 255

/** How finely a tint moves the luminance: in thousandths, as Excel draws it. */
const TINT_STEPS = 1000

/** The hue Windows gives a grey, which has none. */
const GREY_HUE = (HLS_MAX * 2) / 3

/** A division of whole numbers, as Windows divides them. */
const div = (dividend: number, divisor: number): number => Math.trunc(dividend / divisor)

interface Hls {
    hue: number
    lum: number
    sat: number
}

const hueDelta = (max: number, channel: number, range: number): number =>
    div((max - channel) * (HLS_MAX / 6) + div(range, 2), range)

/** A colour in the hue, luminance and saturation model of Windows, which Excel tints a colour in. */
const toHls = (red: number, green: number, blue: number): Hls => {
    const max = Math.max(red, green, blue)
    const min = Math.min(red, green, blue)
    const lum = div((max + min) * HLS_MAX + RGB_MAX, 2 * RGB_MAX)
    if (max === min) {
        return { hue: GREY_HUE, lum, sat: 0 }
    }
    const range = max - min
    const sat = lum <= HLS_MAX / 2
        ? div(range * HLS_MAX + div(max + min, 2), max + min)
        : div(range * HLS_MAX + div(2 * RGB_MAX - max - min, 2), 2 * RGB_MAX - max - min)
    let hue
    if (red === max) {
        hue = hueDelta(max, blue, range) - hueDelta(max, green, range)
    } else if (green === max) {
        hue = HLS_MAX / 3 + hueDelta(max, red, range) - hueDelta(max, blue, range)
    } else {
        hue = (2 * HLS_MAX) / 3 + hueDelta(max, green, range) - hueDelta(max, red, range)
    }
    return { hue: hue < 0 ? hue + HLS_MAX : hue, lum, sat }
}

const hueToLevel = (low: number, high: number, at: number): number => {
    let hue = at < 0 ? at + HLS_MAX : at
    if (hue > HLS_MAX) {
        hue -= HLS_MAX
    }
    if (hue < HLS_MAX / 6) {
        return low + div((high - low) * hue + HLS_MAX / 12, HLS_MAX / 6)
    }
    if (hue < HLS_MAX / 2) {
        return high
    }
    if (hue < (HLS_MAX * 2) / 3) {
        return low + div((high - low) * ((HLS_MAX * 2) / 3 - hue) + HLS_MAX / 12, HLS_MAX / 6)
    }
    return low
}

const channel = (level: number): string => div(level * RGB_MAX + HLS_MAX / 2, HLS_MAX).toString(16).padStart(2, '0')

const toHex = ({ hue, lum, sat }: Hls): string => {
    if (sat === 0) {
        return `#${channel(lum).repeat(3)}`
    }
    const high = lum <= HLS_MAX / 2
        ? div(lum * (HLS_MAX + sat) + HLS_MAX / 2, HLS_MAX)
        : lum + sat - div(lum * sat + HLS_MAX / 2, HLS_MAX)
    const low = 2 * lum - high
    return `#${channel(hueToLevel(low, high, hue + HLS_MAX / 3))}${channel(hueToLevel(low, high, hue))}`
        + channel(hueToLevel(low, high, hue - HLS_MAX / 3))
}

/**
 * A colour made lighter or darker as Excel draws it, to the last digit.
 *
 * Excel tints a colour in the hue, luminance and saturation model of Windows: a tint above 0 moves the luminance that
 * part of the way to white, and a tint below 0 that part of the way to black, the hue and the saturation kept. A tint
 * of less than a thousandth leaves the colour as it is.
 *
 * @param hex  the colour as #rrggbb
 * @param tint how much lighter the colour is, above 0, or darker, below 0, from -1 to 1
 * @returns the colour drawn, as #rrggbb in lower case
 */
export const excelTint = (hex: string, tint: number): string => {
    const steps = Math.round(tint * TINT_STEPS)
    if (steps === 0) {
        return hex.toLowerCase()
    }
    const value = Number.parseInt(hex.slice(1), 16)
    const colour = toHls((value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff)
    const lum = steps < 0
        ? div(colour.lum * (TINT_STEPS + steps), TINT_STEPS)
        : div(colour.lum * (TINT_STEPS - steps) + HLS_MAX * steps, TINT_STEPS)
    return toHex({ ...colour, lum })
}

/**
 * A colour of the palette of Excel as it is drawn: a standard colour as it is, a theme colour in the colours of
 * Office, made lighter or darker.
 *
 * @param colour the colour
 * @returns the colour as #rrggbb in lower case
 */
export const hexOf = (colour: RawCellColor): string =>
    'rgb' in colour ? colour.rgb.toLowerCase() : excelTint(OFFICE_THEME[colour.theme], colour.tint ?? 0)

const KILOBYTE = 1024
const MEGABYTE = KILOBYTE * KILOBYTE

/** A number of the unit written short in the given UI language, with as many decimals as given. */
const formatUnit = (value: number, unit: 'kilobyte' | 'megabyte', fractionDigits: number, locale: string): string =>
    new Intl.NumberFormat(locale, {
        style: 'unit',
        unit,
        unitDisplay: 'short',
        minimumFractionDigits: fractionDigits,
        maximumFractionDigits: fractionDigits,
    }).format(value)

/**
 * The size of a file in kilobytes, written in the given UI language: `0.5 kB`, `12 kB`.
 *
 * <p>A size under ten kilobytes keeps one decimal, a larger one none. No size gives no text, so a row
 * without one shows nothing in its place.
 */
export const formatFileSize = (bytes: number | undefined, locale: string): string | null => {
    if (bytes === undefined) {
        return null
    }
    const kb = bytes / KILOBYTE
    return formatUnit(kb, 'kilobyte', kb >= 10 ? 0 : 1, locale)
}

/** A size in megabytes with one decimal, written in the given UI language: `0.4 MB`, `125.6 MB`. */
export const formatMegabytes = (bytes: number, locale: string): string => formatUnit(bytes / MEGABYTE, 'megabyte', 1, locale)

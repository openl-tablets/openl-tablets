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
    const kb = bytes / 1024
    const fractionDigits = kb >= 10 ? 0 : 1
    return new Intl.NumberFormat(locale, {
        style: 'unit',
        unit: 'kilobyte',
        unitDisplay: 'short',
        minimumFractionDigits: fractionDigits,
        maximumFractionDigits: fractionDigits,
    }).format(kb)
}

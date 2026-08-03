import dayjs, { type Dayjs } from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'

dayjs.extend(customParseFormat)

export const ISO_DATE_FORMAT = 'YYYY-MM-DD'

const DATE_PART_TOKENS = {
    day: 'DD',
    month: 'MM',
    year: 'YYYY',
} as const

/** Date picker pattern derived from the user's locale, independent of the value stored in the workbook. */
export const datePickerFormatForLocale = (locale?: string): string =>
    new Intl.DateTimeFormat(locale, { day: '2-digit', month: '2-digit', year: 'numeric' })
        .formatToParts(new Date(2006, 10, 22))
        .map(part => part.type in DATE_PART_TOKENS
            ? DATE_PART_TOKENS[part.type as keyof typeof DATE_PART_TOKENS]
            : `[${part.value}]`)
        .join('')

/** Parses the ISO date representation stored in a table cell. */
export const parseDateValue = (value: unknown): Dayjs | null => {
    if (typeof value !== 'string' || !value) {
        return null
    }
    const parsed = dayjs(value, ISO_DATE_FORMAT, true)
    return parsed.isValid() ? parsed : null
}

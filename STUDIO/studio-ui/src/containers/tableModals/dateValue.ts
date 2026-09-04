import dayjs, { type Dayjs } from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'

dayjs.extend(customParseFormat)

export const ISO_DATE_FORMAT = 'YYYY-MM-DD'
const ISO_MINUTES_FORMAT = 'YYYY-MM-DDTHH:mm'
const ISO_SECONDS_FORMAT = 'YYYY-MM-DDTHH:mm:ss'
const ISO_MILLIS_FORMAT = 'YYYY-MM-DDTHH:mm:ss.SSS'

/**
 * The ISO 8601 forms a date value is written in: the day alone, and the day with the time it carries.
 *
 * <p>A date property usually means a day, and that is how one is written. It may name a moment of that day
 * instead — an effective date set to `09:00` dispatches on that moment — and such a value is written whole, down
 * to the millisecond, so that it reads back as the moment it was.
 */
const ISO_DATE_FORMATS = [ISO_DATE_FORMAT, ISO_MINUTES_FORMAT, ISO_SECONDS_FORMAT, ISO_MILLIS_FORMAT]

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

/** Parses the ISO date representation a date value is carried in, answering `null` for a text that is not one. */
export const parseDateValue = (value: unknown): Dayjs | null => {
    if (typeof value !== 'string' || !value) {
        return null
    }
    const parsed = dayjs(value, ISO_DATE_FORMATS, true)
    return parsed.isValid() ? parsed : null
}

/**
 * The text a picked day is written as, keeping the time of day the value it replaces carried.
 *
 * <p>The picker chooses a day, not a moment. A date set to `09:00` would be moved to midnight by picking the day
 * it already stands on, and for a dimension date that changes the requests the table answers — so the time is
 * carried over rather than dropped.
 *
 * <p>The text takes the shape the server writes: the day alone, and the time after it only when there is one.
 */
export const formatDateValue = (picked: Dayjs, replaced?: unknown): string => {
    const carried = parseDateValue(replaced)
    const moment = carried
        ? picked
            .hour(carried.hour())
            .minute(carried.minute())
            .second(carried.second())
            .millisecond(carried.millisecond())
        : picked
    if (moment.millisecond()) {
        return moment.format(ISO_MILLIS_FORMAT)
    }
    if (moment.second()) {
        return moment.format(ISO_SECONDS_FORMAT)
    }
    if (moment.hour() || moment.minute()) {
        return moment.format(ISO_MINUTES_FORMAT)
    }
    return moment.format(ISO_DATE_FORMAT)
}

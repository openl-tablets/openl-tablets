import { formatLocalDateTime, isLocalDateTime } from './dateFormat'

/** A value of a dimension property as a reader reads it: a date formatted, several values separated by commas. */
export const dimensionValue = (value: unknown): string => {
    if (Array.isArray(value)) {
        return value.map(dimensionValue).join(',')
    }
    return isLocalDateTime(value) ? formatLocalDateTime(value) : String(value)
}

/**
 * The name that tells a version of a table from the others: the name of the table, then the dimension properties
 * that tell the versions apart, such as `carPrice [effectiveDate=Jul 1, 2020 10:30:00 AM, state=AR]`.
 *
 * A table written in one version has no dimensions and is named as it is.
 */
export const versionLabel = (name: string, dimensions?: Record<string, unknown>): string => {
    const values = Object.entries(dimensions ?? {}).map(([key, value]) => `${key}=${dimensionValue(value)}`)
    return values.length > 0 ? `${name} [${values.join(', ')}]` : name
}

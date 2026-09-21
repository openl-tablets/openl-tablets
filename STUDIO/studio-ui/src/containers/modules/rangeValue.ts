/**
 * What a range cell holds, as its two bounds.
 *
 * <p>A bound left empty is no bound at all: a range with only a lower one reads "greater than", with only an
 * upper one "less than", and with neither it is not a range.
 */
export interface RangeBounds {
    from: string
    to: string
    fromIncluded: boolean
    toIncluded: boolean
}

export const NO_RANGE: RangeBounds = { from: '', to: '', fromIncluded: true, toIncluded: true }

/** A number as a range is written with: digits, a sign, a decimal point. */
const NUMBER = String.raw`[+-]?\d+(?:\.\d+)?`

/** The ways OpenL writes two bounds between brackets, and the ways it writes them without. */
const BRACKETED = new RegExp(`^([[(])\\s*(${NUMBER})\\s*(?:\\.{2,3}|…|;|-)\\s*(${NUMBER})\\s*([\\])])$`)
const SEPARATED = new RegExp(`^(${NUMBER})\\s*(\\.{2,3}|…|;|-)\\s*(${NUMBER})$`)
const AT_LEAST = new RegExp(`^(>=?)\\s*(${NUMBER})$`)
const AT_MOST = new RegExp(`^(<=?)\\s*(${NUMBER})$`)
const AND_MORE = new RegExp(`^(${NUMBER})\\s*\\+$`)
const EXACT = new RegExp(`^(${NUMBER})$`)

/** A separator that leaves both bounds outside the range, as OpenL reads it. */
const OPEN_SEPARATORS = new Set(['...', '…'])

/**
 * The bounds a range cell holds, or empty bounds when its text is not a range.
 *
 * <p>Every form OpenL reads is understood: between brackets, between a separator, "greater than", "less than",
 * "and more", and a single value standing for itself.
 */
export const parseRange = (text: string): RangeBounds => {
    const written = text.trim()

    const bracketed = BRACKETED.exec(written)
    if (bracketed) {
        return {
            from: bracketed[2] ?? '',
            to: bracketed[3] ?? '',
            fromIncluded: bracketed[1] === '[',
            toIncluded: bracketed[4] === ']',
        }
    }
    const separated = SEPARATED.exec(written)
    if (separated) {
        const included = !OPEN_SEPARATORS.has(separated[2] ?? '')
        return { from: separated[1] ?? '', to: separated[3] ?? '', fromIncluded: included, toIncluded: included }
    }
    const atLeast = AT_LEAST.exec(written)
    if (atLeast) {
        return { ...NO_RANGE, from: atLeast[2] ?? '', fromIncluded: atLeast[1] === '>=' }
    }
    const atMost = AT_MOST.exec(written)
    if (atMost) {
        return { ...NO_RANGE, to: atMost[2] ?? '', toIncluded: atMost[1] === '<=' }
    }
    const andMore = AND_MORE.exec(written)
    if (andMore) {
        return { ...NO_RANGE, from: andMore[1] ?? '' }
    }
    const exact = EXACT.exec(written)
    return exact ? { ...NO_RANGE, from: exact[1] ?? '', to: exact[1] ?? '' } : NO_RANGE
}

/** The four shapes a range takes, which the reader picks between as the old editor let them. */
export type RangeShape = 'at-least' | 'at-most' | 'between' | 'exact'

/**
 * The shape the bounds have: between two of them, above one, below one, or exactly one value.
 *
 * <p>Bounds that are no range at all read as "above one", which is the shape an empty panel offers first.
 */
export const shapeOf = (bounds: RangeBounds): RangeShape => {
    const lower = bounds.from.trim()
    const upper = bounds.to.trim()
    if (lower !== '' && upper !== '') {
        return lower === upper && bounds.fromIncluded && bounds.toIncluded ? 'exact' : 'between'
    }
    return upper !== '' ? 'at-most' : 'at-least'
}

/**
 * The text the bounds are written into the cell as, or an empty string when they are no range at all.
 *
 * <p>Written the way the Editor wrote it, which is the way OpenL reads it back: the two dots carry a space on
 * either side — `IntRange` and `DoubleRange` say so in as many words — and a bound stands against its sign with
 * nothing between them. Brackets are written only where they say something: bounds that are both inside the
 * range need none.
 */
export const formatRange = (bounds: RangeBounds): string => {
    const lower = bounds.from.trim()
    const upper = bounds.to.trim()
    switch (shapeOf(bounds)) {
        case 'exact':
            return lower
        case 'between': {
            const between = `${lower} .. ${upper}`
            return bounds.fromIncluded && bounds.toIncluded
                ? between
                : `${bounds.fromIncluded ? '[' : '('}${between}${bounds.toIncluded ? ']' : ')'}`
        }
        case 'at-most':
            return `${bounds.toIncluded ? '<=' : '<'}${upper}`
        case 'at-least':
        default:
            return lower === '' ? '' : `${bounds.fromIncluded ? '>=' : '>'}${lower}`
    }
}

/** Why the bounds do not make a range, or null when they do. */
export const rangeProblem = (bounds: RangeBounds): 'empty' | 'inverted' | null => {
    const lower = bounds.from.trim()
    const upper = bounds.to.trim()
    if (lower === '' && upper === '') {
        return 'empty'
    }
    if (lower !== '' && upper !== '' && Number(lower) > Number(upper)) {
        return 'inverted'
    }
    return null
}

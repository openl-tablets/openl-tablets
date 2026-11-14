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
const BRACKETED = new RegExp(String.raw`^([[(])\s*(${NUMBER})\s*(?:\.{2,3}|…|;|-)\s*(${NUMBER})\s*([\])])$`)
/** The separator is taken with the spaces around it, so a range can be written back the way it was. */
const SEPARATED = new RegExp(String.raw`^(${NUMBER})(\s*(?:\.{2,3}|…|;|-)\s*)(${NUMBER})$`)
const AT_LEAST = new RegExp(String.raw`^(>=?)\s*(${NUMBER})$`)
const AT_MOST = new RegExp(String.raw`^(<=?)\s*(${NUMBER})$`)
const AND_MORE = new RegExp(String.raw`^(${NUMBER})\s*\+$`)
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
        const included = !OPEN_SEPARATORS.has((separated[2] ?? '').trim())
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
 *
 * <p>When the cell held a range already, the new bounds keep the way the author wrote it — `3+`, `1-5`,
 * `[1; 5)` — as long as that way can still say them. A range whose shape changed is written the Editor's way.
 */
export const formatRange = (bounds: RangeBounds, writtenAs = ''): string => writtenLike(bounds, writtenAs.trim())
    ?? canonical(bounds)

/** The bounds written the way the author wrote the range, or null when that way cannot say them. */
const writtenLike = (bounds: RangeBounds, written: string): string | null => {
    const lower = bounds.from.trim()
    const upper = bounds.to.trim()
    const shape = shapeOf(bounds)
    if (shape === 'at-least' && bounds.fromIncluded && lower !== '' && AND_MORE.test(written)) {
        return `${lower}+`
    }
    if (shape !== 'between') {
        return null
    }
    const separated = SEPARATED.exec(written)
    if (separated && bounds.fromIncluded === bounds.toIncluded
        && bounds.fromIncluded !== OPEN_SEPARATORS.has((separated[2] ?? '').trim())) {
        return `${lower}${separated[2]}${upper}`
    }
    const bracketed = BRACKETED.exec(written)
    if (bracketed) {
        const inside = written.slice(1, -1).trim()
        const separator = SEPARATED.exec(inside)?.[2] ?? ' .. '
        const between = `${lower}${separator}${upper}`
        return `${bounds.fromIncluded ? '[' : '('}${between}${bounds.toIncluded ? ']' : ')'}`
    }
    return null
}

const canonical = (bounds: RangeBounds): string => {
    const lower = bounds.from.trim()
    const upper = bounds.to.trim()
    switch (shapeOf(bounds)) {
        case 'exact':
            return lower
        case 'between': {
            const between = `${lower} .. ${upper}`
            if (bounds.fromIncluded && bounds.toIncluded) {
                return between
            }
            return `${bounds.fromIncluded ? '[' : '('}${between}${bounds.toIncluded ? ']' : ')'}`
        }
        case 'at-most':
            return `${bounds.toIncluded ? '<=' : '<'}${upper}`
        case 'at-least':
        default:
            if (lower === '') {
                return ''
            }
            return `${bounds.fromIncluded ? '>=' : '>'}${lower}`
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

interface RawJson {
    rawJSON(text: string): unknown
}

const JSON_NUMBER = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/
const MAY_HOLD_EXACT = /(?:\d\.?){16}|-0\b|\d[eE]/
const LONGEST_PLAIN_EXPONENT = 1000
const DECIMAL = /^(-?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/

export class ExactNumber {
    readonly source: string

    constructor(source: string) {
        this.source = source
    }

    toString(): string {
        return this.source
    }

    toJSON(): unknown {
        return (JSON as unknown as RawJson).rawJSON(this.source)
    }
}

export const isExactNumber = (value: unknown): value is ExactNumber => value instanceof ExactNumber

const canonical = (text: string): string | undefined => {
    const match = DECIMAL.exec(text)
    if (!match) {
        return undefined
    }
    const [, sign = '', whole = '', fraction = '', exponent = '0'] = match
    const digits = (whole + fraction).replace(/^0+/, '')
    let end = digits.length
    while (end > 0 && digits[end - 1] === '0') {
        end--
    }
    return end === 0
        ? '0'
        : `${sign}${digits.slice(0, end)}e${Number(exponent) - fraction.length + digits.length - end}`
}

const holds = (value: number, source: string): boolean =>
    String(value) === source || (!Object.is(value, -0) && canonical(String(value)) === canonical(source))

const keepExact = (_key: string, value: unknown, context?: { source?: string }): unknown =>
    (typeof value === 'number' && context?.source !== undefined && !holds(value, context.source)
        ? new ExactNumber(context.source)
        : value)

export const parseJson = (text: string): ReturnType<typeof JSON.parse> =>
    (MAY_HOLD_EXACT.test(text) ? JSON.parse(text, keepExact) : JSON.parse(text))

export const numberOf = (text: string): ExactNumber | number | undefined => {
    if (!JSON_NUMBER.test(text)) {
        return undefined
    }
    const value = Number(text)
    return holds(value, text) ? value : new ExactNumber(text)
}

export const plainDecimal = (source: string): string => {
    const match = DECIMAL.exec(source)
    const exponent = Number(match?.[4] ?? 0)
    if (!match || Math.abs(exponent) > LONGEST_PLAIN_EXPONENT) {
        return source
    }
    const [, sign = '', whole = '', fraction = ''] = match
    const digits = whole + fraction
    const point = whole.length + exponent
    if (point <= 0) {
        return `${sign}0.${'0'.repeat(-point)}${digits}`
    }
    return point >= digits.length
        ? `${sign}${digits}${'0'.repeat(point - digits.length)}`
        : `${sign}${digits.slice(0, point)}.${digits.slice(point)}`
}

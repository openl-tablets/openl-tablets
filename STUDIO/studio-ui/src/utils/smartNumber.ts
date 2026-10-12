const JAVA_PLAIN_MIN = 1e-3
const JAVA_PLAIN_MAX = 1e7

interface ShortestDecimal {
    digits: string
    exponent: number
}

const shortestDecimal = (value: number): ShortestDecimal => {
    const [mantissa = '0', exponent = '0'] = Math.abs(value).toExponential().split('e')
    return { digits: mantissa.replace('.', ''), exponent: Number(exponent) }
}

const javaScale = (value: number): number => {
    if (value === 0) {
        return 1
    }
    const { digits, exponent } = shortestDecimal(value)
    const magnitude = Math.abs(value)
    return magnitude >= JAVA_PLAIN_MIN && magnitude < JAVA_PLAIN_MAX
        ? Math.max(1, digits.length - 1 - exponent)
        : Math.max(1, digits.length - 1) - exponent
}

const ulp = (value: number): number => {
    const view = new DataView(new ArrayBuffer(8))
    view.setFloat64(0, value)
    const biased = (view.getUint16(0) >> 4) & 0x7ff
    if (biased === 0) {
        return Number.MIN_VALUE
    }
    const exponent = biased - 1075
    view.setBigUint64(0, exponent >= -1022 ? BigInt(exponent + 1023) << 52n : 1n << BigInt(exponent + 1074))
    return view.getFloat64(0)
}

const plainNotation = (value: number): string => {
    const { digits, exponent } = shortestDecimal(value)
    let text: string
    if (exponent >= digits.length - 1) {
        text = digits + '0'.repeat(exponent - digits.length + 1)
    } else if (exponent >= 0) {
        text = `${digits.slice(0, exponent + 1)}.${digits.slice(exponent + 1)}`
    } else {
        text = `0.${'0'.repeat(-exponent - 1)}${digits}`
    }
    return value < 0 ? `-${text}` : text
}

export const formatSmartNumber = (value: number): string => {
    if (!Number.isFinite(value)) {
        return String(value)
    }
    if (Number.isInteger(value)) {
        return plainNotation(value)
    }
    const scale = javaScale(value)
    let best = value
    let bestScale = scale
    let below = value
    let above = value
    for (let step = 0; step < 2; step++) {
        below -= ulp(below)
        above += ulp(above)
        for (const candidate of [below, above]) {
            const candidateScale = javaScale(candidate)
            if (candidateScale < bestScale) {
                bestScale = candidateScale
                best = candidate
            }
        }
    }
    return plainNotation(scale - bestScale > 1 ? best : value)
}

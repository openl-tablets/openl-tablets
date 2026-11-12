import { ExactNumber, isExactNumber, numberOf, parseJson, plainDecimal } from 'utils/exactJson'

const EXACT = [
    '2.000000000000000005',
    '-2.000000000000000005',
    '0.6666666666666666666666666666666667',
    '9007199254740993',
    '12345678901234567890',
    '0.1000000000000000055511151231257827',
    '1e400',
    '1e-400',
    '-0',
    '-0.0',
]

const PLAIN: [string, number][] = [
    ['2.5', 2.5],
    ['2.50', 2.5],
    ['100.0', 100],
    ['1e2', 100],
    ['1E+2', 100],
    ['1.0E10', 10000000000],
    ['0', 0],
    ['0.0', 0],
    ['0.1', 0.1],
    ['0.30000000000000004', 0.30000000000000004],
    ['20.759999999999998', 20.759999999999998],
    ['9007199254740992', 9007199254740992],
    ['5e-324', 5e-324],
    ['1e21', 1e21],
]

describe('parseJson', () => {
    it.each(EXACT)('keeps %s, which a double does not hold, as it is written', source => {
        const value = parseJson(source)

        expect(isExactNumber(value)).toBe(true)
        expect(String(value)).toBe(source)
    })

    it.each(PLAIN)('reads %s, which a double holds, as a number', (source, expected) => {
        expect(parseJson(source)).toBe(expected)
    })

    it('reads the numbers inside objects and lists, and leaves texts alone', () => {
        const text = '{"params":{"v":2.000000000000000005,"n":[1,9007199254740993],"s":"2.000000000000000005"}}'
        const value = parseJson(text)

        expect(value).toStrictEqual({
            params: {
                v: new ExactNumber('2.000000000000000005'),
                n: [1, new ExactNumber('9007199254740993')],
                s: '2.000000000000000005',
            },
        })
    })

    it('reads what is not a number as JSON.parse does', () => {
        expect(parseJson('[true,null,"a",{}]')).toEqual([true, null, 'a', {}])
        expect(() => parseJson('{')).toThrow(SyntaxError)
    })
})

describe('ExactNumber', () => {
    it.each(EXACT)('writes %s back as the number it was read from', source => {
        expect(JSON.stringify(parseJson(source))).toBe(source)
    })

    it('is written inside a structure with every digit, indented or not', () => {
        const text = '{"params":{"v":2.000000000000000005,"n":[1,9007199254740993]}}'

        expect(JSON.stringify(parseJson(text))).toBe(text)
        expect(JSON.stringify(parseJson('{"v":2.000000000000000005}'), null, 2))
            .toBe('{\n  "v": 2.000000000000000005\n}')
    })
})

describe('numberOf', () => {
    it.each(EXACT)('keeps the typed %s as it is written', source => {
        expect(numberOf(source)).toStrictEqual(new ExactNumber(source))
    })

    it.each(PLAIN)('gives the typed %s as a number', (source, expected) => {
        expect(numberOf(source)).toBe(expected)
    })

    it.each(['', ' ', '-', '1.', '.5', '+1', '01', 'abc', '1e', 'NaN', 'Infinity'])(
        'gives nothing for %j, which is not a JSON number',
        text => {
            expect(numberOf(text)).toBeUndefined()
        }
    )
})

describe('plainDecimal', () => {
    it.each([
        ['2.000000000000000005', '2.000000000000000005'],
        ['1E+2', '100'],
        ['1.5e3', '1500'],
        ['-1.234567890123456789E-7', '-0.0000001234567890123456789'],
        ['1.23E+25', '12300000000000000000000000'],
        ['12.5e-1', '1.25'],
        ['0E-10', '0.0000000000'],
        ['1e2000', '1e2000'],
    ])('writes %s without an exponent as %s', (source, plain) => {
        expect(plainDecimal(source)).toBe(plain)
    })
})

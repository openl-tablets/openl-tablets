import { formatSmartNumber } from 'utils/smartNumber'

const formatsAs = (cases: [number, string][]) => {
    for (const [value, text] of cases) {
        expect(formatSmartNumber(value), String(value)).toBe(text)
    }
}

describe('formatSmartNumber', () => {
    it('shows the short decimal a binary result stands one or two units in the last place from', () => {
        formatsAs([
            [20.759999999999998, '20.76'],
            [0.08333999999999997, '0.08334'],
            [0.1 + 0.2, '0.3'],
            [1.0000000000000002, '1'],
            [99.99999999999999, '100'],
            [434.99999999999994, '435'],
            [0.19999999999999998, '0.2'],
            [-20.759999999999998, '-20.76'],
            [-0.0034758125999999998, '-0.0034758126'],
            [-132.79510000000002, '-132.7951'],
            [9.999999999999998e-4, '0.001'],
            [3.0000000000000003e-4, '0.0003'],
            [8.231717999999999e-7, '0.0000008231718'],
            [9999999.999999998, '10000000'],
        ])
    })

    it('keeps a value that no near decimal is much shorter than', () => {
        formatsAs([
            [0.08333, '0.08333'],
            [23 / 19, '1.2105263157894737'],
            [1 / 3, '0.3333333333333333'],
            [0.1, '0.1'],
            [2.675, '2.675'],
            [-0.80453169, '-0.80453169'],
        ])
    })

    it('writes every number in plain notation', () => {
        formatsAs([
            [1e-7, '0.0000001'],
            [-8e-10, '-0.0000000008'],
            [12345678.9, '12345678.9'],
            [10000000.1, '10000000.1'],
            [3.3333333333333335e15, '3333333333333333.5'],
            [1e21, '1000000000000000000000'],
            [1.4285714285714287e21, '1428571428571428700000'],
            [6.624354836e23, '662435483600000000000000'],
            [5e-324, '0'],
            [Number.MAX_VALUE, `17976931348623157${'0'.repeat(292)}`],
        ])
    })

    it('writes a whole number as it is', () => {
        formatsAs([
            [0, '0'],
            [25, '25'],
            [-468, '-468'],
        ])
    })

    it('writes what is not a finite number as JavaScript does', () => {
        formatsAs([
            [Number.NaN, 'NaN'],
            [Number.POSITIVE_INFINITY, 'Infinity'],
            [Number.NEGATIVE_INFINITY, '-Infinity'],
        ])
    })
})

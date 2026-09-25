import { describe, expect, it } from 'vitest'
import type { BenchmarkResult } from 'types/execution'
import { formatRate, formatRatio, formatTime, metricsOf } from './benchmarkMetrics'

const measurement = (over: Partial<BenchmarkResult> = {}): BenchmarkResult => ({
    id: 'm1',
    tableId: 't1',
    name: 'PolicyTest',
    testTable: true,
    testCases: 1,
    runs: 1000,
    executionTimeMs: 4000,
    ...over,
})

describe('the speed a measurement stands for', () => {
    it('divides the measured time by the runs, and a run by the test cases it covers', () => {
        // 3500 ms over 512 runs of 3 test cases.
        const metrics = metricsOf(measurement({ runs: 512, testCases: 3, executionTimeMs: 3500 }))

        expect(metrics.runMs).toBeCloseTo(6.836, 3)
        expect(metrics.testCaseMs).toBeCloseTo(2.279, 3)
        expect(metrics.runsPerSecond).toBeCloseTo(146.29, 2)
        expect(metrics.testCasesPerSecond).toBeCloseTo(438.86, 2)
    })

    it('counts a run as a test case of its own when the measured table holds one', () => {
        const metrics = metricsOf(measurement({ runs: 1000, testCases: 1, executionTimeMs: 4000 }))

        expect(metrics.runMs).toBe(metrics.testCaseMs)
        expect(metrics.runsPerSecond).toBe(metrics.testCasesPerSecond)
        expect(metrics.runsPerSecond).toBe(250)
    })

    // A measurement too fast for the clock reports no time at all; the rates it works out are not numbers, and
    // the formatters are what turns them into a dash rather than into `Infinity` on the screen.
    it('works out no rate from a measurement that took no measurable time', () => {
        const metrics = metricsOf(measurement({ executionTimeMs: 0 }))

        expect(metrics.runMs).toBe(0)
        expect(metrics.runsPerSecond).toBe(Number.POSITIVE_INFINITY)
    })
})

describe('a measured time', () => {
    // The smaller the time, the more decimals it is worth reading to. The bands are pinned at their edges
    // because the first two are entered by `>=` and the rest by `>`.
    it.each([
        ['whole units from a second up', 1500, '1,500'],
        ['whole units at the edge of a second', 1000, '1,000'],
        ['two decimals above a millisecond', 6.8359375, '6.84'],
        ['two decimals at a millisecond', 1, '1.00'],
        ['three decimals above a tenth', 0.5, '0.500'],
        ['four decimals at a tenth', 0.1, '0.1000'],
        ['four decimals above a hundredth', 0.05, '0.0500'],
        ['five decimals at a hundredth', 0.01, '0.01000'],
        ['five decimals above a thousandth', 0.005, '0.00500'],
        ['six decimals at a thousandth', 0.001, '0.001000'],
        ['six decimals below a thousandth', 0.0005, '0.000500'],
        ['six decimals of no time at all', 0, '0.000000'],
    ])('is written to %s', (_band, ms, written) => {
        expect(formatTime(ms, 'en-US')).toBe(written)
    })

    it('is a dash when it is no number', () => {
        expect(formatTime(Number.NaN, 'en-US')).toBe('—')
        expect(formatTime(Number.POSITIVE_INFINITY, 'en-US')).toBe('—')
    })
})

describe('a measured rate', () => {
    it('is written in whole units, grouped as the locale groups them', () => {
        expect(formatRate(438.86, 'en-US')).toBe('439')
        expect(formatRate(12345.6, 'en-US')).toBe('12,346')
    })

    it('is a dash when the measurement was too fast to work one out', () => {
        expect(formatRate(Number.POSITIVE_INFINITY, 'en-US')).toBe('—')
        expect(formatRate(Number.NaN, 'en-US')).toBe('—')
    })
})

describe('how much slower a compared measurement is', () => {
    it('is written to two decimals, and the fastest one stands at one', () => {
        expect(formatRatio(1, 'en-US')).toBe('1.00')
        expect(formatRatio(2.5, 'en-US')).toBe('2.50')
    })

    it('is a dash for a measurement that is in no comparison', () => {
        expect(formatRatio(Number.NaN, 'en-US')).toBe('—')
    })
})

// The numbers are written in the UI locale, not in the locale of the machine the browser runs on: on a Russian
// or German machine an English screen used to show `6,84` where the rest of it kept its decimal point.
describe('the locale the numbers are written in', () => {
    it('is the one it is asked for, and not the one of the machine', () => {
        expect(formatTime(6.8359375, 'de-DE')).toBe('6,84')
        expect(formatRatio(2.5, 'de-DE')).toBe('2,50')
        expect(formatRate(12345.6, 'de-DE')).toBe('12.346')
    })
})

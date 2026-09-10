import type { BenchmarkResult } from 'types/execution'

const MS_IN_SECOND = 1000

/** What one measurement says about the speed of a table. */
export interface BenchmarkMetrics {
    /** How long one test case took. */
    testCaseMs: number
    /** How many test cases ran in a second. */
    testCasesPerSecond: number
    /** How long one run of the measured table took. */
    runMs: number
    /** How many runs of the measured table fitted in a second. */
    runsPerSecond: number
}

/**
 * The speed one measurement stands for.
 *
 * A benchmark reports how long every run took together. One run took that time divided by the number of runs,
 * and one test case took as much again divided by the cases a run covers.
 */
export const metricsOf = (measurement: BenchmarkResult): BenchmarkMetrics => {
    const runMs = measurement.executionTimeMs / measurement.runs
    const testCaseMs = runMs / measurement.testCases
    return {
        runMs,
        testCaseMs,
        runsPerSecond: MS_IN_SECOND / runMs,
        testCasesPerSecond: MS_IN_SECOND / testCaseMs,
    }
}

/** How many decimals a measured time is worth reading to: the smaller the time, the more of them. */
const decimalsOf = (value: number): number => {
    if (value >= 1000) {
        return 0
    }
    if (value >= 1) {
        return 2
    }
    if (value > 0.1) {
        return 3
    }
    if (value > 0.01) {
        return 4
    }
    if (value > 0.001) {
        return 5
    }
    return 6
}

/** A measured time in milliseconds, written to as many decimals as it is worth reading to. */
export const formatTime = (ms: number): string => (Number.isFinite(ms)
    ? ms.toLocaleString(undefined, { minimumFractionDigits: decimalsOf(ms), maximumFractionDigits: decimalsOf(ms) })
    : '—')

/** A measured rate, written in whole units. */
export const formatRate = (perSecond: number): string => (Number.isFinite(perSecond)
    ? Math.round(perSecond).toLocaleString(undefined, { maximumFractionDigits: 0 })
    : '—')

/** How many times slower a measurement is than the fastest of those compared, written to two decimals. */
export const formatRatio = (ratio: number): string => (Number.isFinite(ratio)
    ? ratio.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '—')

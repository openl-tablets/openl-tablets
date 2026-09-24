import type { MessageDescription, TraceParameterValue } from './trace'

/** How a test unit, or one of its assertions, ended. */
export type TestStatus = 'TR_OK' | 'TR_NEQ' | 'TR_EXCEPTION'

/** A calculated spreadsheet, laid out by the rows and the columns its author wrote. */
export interface SpreadsheetResultView {
    columns: (string | null)[]
    rows: (string | null)[]
    /** The value of every cell, row by row. A cell that holds nothing carries none. */
    cells: unknown[][]
}

/** The result of running one table: what it returned, what it was given, and what went wrong. */
export interface RunResult {
    tableName: string
    tableId: string
    executionTimeMs: number
    /** The value the table returned, as the execution APIs publish it. */
    result?: unknown
    /** JSON schema of the returned value. */
    resultSchema?: object | null
    /** The returned value as the spreadsheet it was calculated by, when the table returns one. */
    resultSpreadsheet?: SpreadsheetResultView | null
    /** The parameters the table was run with. */
    parameters?: TraceParameterValue[]
    /** The runtime context the rules received, when the project provides one. */
    contextParameters?: TraceParameterValue[]
    errors?: MessageDescription[]
}

/** One assertion of a test unit: what was expected, what came out, and whether they matched. */
interface TestAssertionResult {
    description?: string
    expectedValue?: unknown
    actualValue?: unknown
    /**
     * The actual value is referred to instead of written: it has inner structure, or the server let go of it to free
     * memory. It is read a level at a time; a value the server let go of runs its case again.
     */
    actualLazy?: boolean
    /** The expected value is referred to instead of written: it has inner structure. It is read a level at a time. */
    expectedLazy?: boolean
    status: TestStatus
}

/** One line of a level of a value: a field, an element or an entry. */
export interface ValueLine {
    /** What the line is shown as: the name of a field, the index of an element in brackets, or the key of an entry. */
    name: string
    /** What opens the value of the line on a path: the name of a field, or the position of an element or an entry. */
    segment: string
    /** The value of the line, when it is plain. */
    value?: unknown
    /** Display name of the type of a value with inner structure. */
    type?: string | null
    /** How many lines a value with inner structure opens into; absent for a plain value. */
    size?: number | null
    /** The lines of a value with inner structure are elements rather than fields. */
    elements?: boolean | null
}

/** One level of a value with inner structure: a page of the lines it opens into. */
export interface ValueLevel {
    /** Display name of the type of the value. */
    type?: string | null
    /** How many lines the value opens into, on every page. */
    total: number
    /** The lines are elements rather than fields. */
    elements?: boolean | null
    /** The lines of the page; absent when there are none. */
    lines?: ValueLine[]
    /** The value as it is written, when it opens into no lines; absent for one that opens. */
    value?: unknown
}

/** One case of a test table, as it ran. */
export interface TestUnitResult {
    id: string
    description?: string
    executionTimeMs: number
    status: TestStatus
    testAssertions?: TestAssertionResult[]
    /**
     * The whole value the tested rule returned, asked for by the compound-result option. A value with inner
     * structure can be referred to instead, and reading the case reads it.
     */
    result?: TraceParameterValue
    parameters?: TraceParameterValue[]
    contextParameters?: TraceParameterValue[]
    errors?: MessageDescription[]
}

/** The results of one test table: its cases and how many of them failed. */
export interface TestTableResult {
    name: string
    tableId: string
    /** The module the test table is written in; absent when no module of the project holds it. */
    module?: string
    description?: string
    executionTimeMs: number
    numberOfTests: number
    numberOfFailures: number
    /** The table is a Run table, which states no expected values, so its cases neither pass nor fail. */
    runTable?: boolean
    testUnits?: TestUnitResult[]
}

/** A page of the test tables that ran, with the totals of the whole run. */
export interface TestsSummary {
    testCases: TestTableResult[]
    pageNumber: number
    pageSize: number
    numberOfElements: number
    /** How many test tables ran in all. */
    total: number
    executionTimeMs: number
    numberOfTests: number
    numberOfFailures: number
}

/** One measurement of a benchmark: how fast a table ran, and what was measured. */
export interface BenchmarkResult {
    id: string
    tableId: string
    /** The module the measured table is written in; absent when no module of the project holds it. */
    module?: string
    name: string
    /** The measured table is a test table, whose test cases were run. */
    testTable: boolean
    /** The measured table is a Run table, which states no expected values. */
    runTable?: boolean
    /** How many test cases one run covers. */
    testCases: number
    /** How many times the table ran during the measurement. */
    runs: number
    /** How long every run took together. */
    executionTimeMs: number
    /** The input of the measured test case. A whole test table is measured without one. */
    parameters?: TraceParameterValue[]
}

/**
 * What a run or a test run is reported as while it goes on.
 *
 * A run reports every one of them; a test run never reports `ERROR`, and says `INTERRUPTED` instead.
 */
export type ExecutionStatus = 'PENDING' | 'STARTED' | 'COMPLETED' | 'INTERRUPTED' | 'ERROR'

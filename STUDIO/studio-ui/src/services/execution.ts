import type { BenchmarkResult, RunResult, TestsSummary, TestUnitResult } from 'types/execution'
import apiCall, { asArray, isApiHttpError } from './apiCall'
import { toUrlSafeId } from './projectId'

const EXECUTION_API_OPTIONS = { throwError: true, suppressErrorPages: true }

// A result is served as JSON or as a workbook, and the endpoint asks which of them is wanted.
const AS_JSON = { headers: { Accept: 'application/json' } }

/** The workbook the run and the test results are saved as. */
export const XLSX_MEDIA_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

/** How many test tables one page of the results holds, and the sizes the screen offers instead. */
export const TESTS_PAGE_SIZE = 20
/** A size of {@link ALL_TESTS_ON_A_PAGE} puts every test table on one page. */
export const TESTS_PAGE_SIZES = [1, 5, 20, -1]
export const ALL_TESTS_ON_A_PAGE = -1

/** How many failures of a test table the results show, and the counts the screen offers instead. */
export const FAILURES_PER_TEST = 5
export const FAILURES_PER_TEST_OPTIONS = [1, 5, 20]

/** What the results screen is asked for: which test units to show, and which page of the tables. */
export interface TestsQuery {
    failuresOnly?: boolean
    failures?: number
    compoundResult?: boolean
    /** Ask for a value with inner structure to be referred to instead of written, and read it on demand. */
    lazyValues?: boolean
    page?: number
    /** How many test tables a page holds, or {@link ALL_TESTS_ON_A_PAGE} for all of them at once. */
    size?: number
}

/**
 * Whether a result is missing because the run is still going on, rather than because it failed.
 *
 * A run that has not ended yet answers that its result is not ready. Any other answer means the result will
 * not come.
 */
export const isStillRunning = (error: unknown): boolean => isApiHttpError(error) && error.status === 409

/**
 * How long a screen keeps asking for a result after the run says it has ended.
 *
 * The status is reported from inside the run, so it can arrive a moment before the result is there to read.
 */
const SETTLE_INTERVAL_MS = 300
const SETTLE_ATTEMPTS = 10

const delay = (ms: number): Promise<void> => new Promise(resolve => {
    setTimeout(resolve, ms)
})

/**
 * Reads a result that a run has just reported as ended.
 *
 * The read is repeated for a moment while the answer is that the run has not ended, because the status of a run
 * is reported from inside it. A run that is still going on answers that way to the last attempt as well, and
 * the caller waits for the next status.
 */
const readSettled = <T, >(read: () => Promise<T>, attemptsLeft = SETTLE_ATTEMPTS): Promise<T> =>
    read().catch(error => {
        if (!isStillRunning(error) || attemptsLeft <= 0) {
            throw error
        }
        return delay(SETTLE_INTERVAL_MS).then(() => readSettled(read, attemptsLeft - 1))
    })

const projectUrl = (projectId: string, suffix: string): string =>
    `/projects/${toUrlSafeId(projectId)}${suffix}`

const withModule = (params: URLSearchParams, fromModule?: string | undefined): URLSearchParams => {
    if (fromModule) {
        params.set('fromModule', fromModule)
    }
    return params
}

/**
 * Starts the run of a table with the input the panel collected.
 *
 * The run goes on in the background: it reports on its own topic and the result is read when it ends.
 */
export const startRun = async (
    projectId: string,
    tableId: string,
    inputJson: string,
    options: { fromModule?: string } = {}
): Promise<void> => {
    const params = withModule(new URLSearchParams({ tableId }), options.fromModule)
    await apiCall(
        projectUrl(projectId, `/run?${params}`),
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: inputJson },
        { ...EXECUTION_API_OPTIONS, skipWorkspaceEvent: true }
    )
}

/**
 * Reads the result of the run that has just ended: what the table returned, and what it was given.
 *
 * A screen that shows a spreadsheet result as the table it was calculated by asks for that layout. It repeats
 * the values of the result, so the result is read without it by default.
 */
export const getRunResult = async (projectId: string, options: { spreadsheet?: boolean } = {}): Promise<RunResult> => {
    const params = new URLSearchParams({ spreadsheet: String(options.spreadsheet ?? false) })
    return await apiCall(projectUrl(projectId, `/run/result?${params}`), AS_JSON, EXECUTION_API_OPTIONS) as RunResult
}

/** Reads the result of a run that has just ended, waiting out the moment the run needs to publish it. */
export const readRunResult = (projectId: string, options: { spreadsheet?: boolean } = {}): Promise<RunResult> =>
    readSettled(() => getRunResult(projectId, options))

/** How the workbook of a run writes the input the table ran with. */
export interface RunFileOptions {
    /** Leave the input values that are empty out of the workbook. */
    skipEmptyParameters?: boolean
    /** Write every field of an input on a row of its own. */
    flattenParameters?: boolean
}

/** Reads the result of the run as the workbook the user saves. */
export const getRunResultWorkbook = async (projectId: string, options: RunFileOptions = {}): Promise<Blob> => {
    // Only what the caller asked for is sent. How a workbook is written when nothing is asked is the
    // endpoint's to say, and saying it here as well would be a second place to keep it.
    const params = new URLSearchParams({
        ...(options.skipEmptyParameters !== undefined
            && { skipEmptyParameters: String(options.skipEmptyParameters) }),
        ...(options.flattenParameters !== undefined && { flattenParameters: String(options.flattenParameters) }),
    })
    const query = params.toString()
    return await apiCall(
        projectUrl(projectId, `/run/result${query === '' ? '' : `?${query}`}`),
        { headers: { Accept: XLSX_MEDIA_TYPE } },
        { ...EXECUTION_API_OPTIONS, responseType: 'blob' }
    ) as Blob
}

/** Reads the workbook of a run that has just ended, waiting out the moment the run needs to publish it. */
export const readRunResultWorkbook = (projectId: string, options: RunFileOptions = {}): Promise<Blob> =>
    readSettled(() => getRunResultWorkbook(projectId, options))

/**
 * Runs tests: the cases of one test table, every test that tests one rule table, or every test of the project.
 *
 * The run goes on in the background, reporting each test table as it finishes.
 */
export const runTests = async (
    projectId: string,
    options: { tableId?: string, testRanges?: string, fromModule?: string } = {}
): Promise<void> => {
    const params = withModule(new URLSearchParams(), options.fromModule)
    if (options.tableId) {
        params.set('tableId', options.tableId)
    }
    if (options.testRanges) {
        params.set('testRanges', options.testRanges)
    }
    const query = params.toString()
    const suffix = query ? `?${query}` : ''
    await apiCall(
        projectUrl(projectId, `/tests/run${suffix}`),
        { method: 'POST' },
        { ...EXECUTION_API_OPTIONS, skipWorkspaceEvent: true }
    )
}

const testsQueryParams = (query: TestsQuery): URLSearchParams => {
    const params = new URLSearchParams()
    params.set('failuresOnly', String(query.failuresOnly ?? false))
    params.set('failures', String(query.failures ?? FAILURES_PER_TEST))
    params.set('compoundResult', String(query.compoundResult ?? false))
    if (query.lazyValues) {
        params.set('lazyValues', 'true')
    }
    const size = query.size ?? TESTS_PAGE_SIZE
    if (size === ALL_TESTS_ON_A_PAGE) {
        params.set('unpaged', 'true')
    } else {
        params.set('page', String(query.page ?? 0))
        params.set('size', String(size))
    }
    return params
}

/** Reads a page of the test tables that ran, with the totals of the whole run. */
export const getTestsSummary = async (projectId: string, query: TestsQuery = {}): Promise<TestsSummary> => {
    const summary = await apiCall(
        projectUrl(projectId, `/tests/summary?${testsQueryParams(query)}`),
        AS_JSON,
        EXECUTION_API_OPTIONS
    ) as TestsSummary | null
    return { ...summary, testCases: asArray(summary?.testCases) } as TestsSummary
}

/** Reads the results of a test run that has just ended, waiting out the moment the run needs to publish them. */
export const readTestsSummary = (projectId: string, query: TestsQuery = {}): Promise<TestsSummary> =>
    readSettled(() => getTestsSummary(projectId, query))

/**
 * Reads one case of the test run that has ended, with every value written out.
 *
 * A summary asked with `lazyValues` only refers to a value with inner structure; this answers with the case
 * that holds it.
 */
export const getTestCaseResult = async (projectId: string, tableId: string, caseId: string): Promise<TestUnitResult> =>
    await apiCall(
        projectUrl(projectId, `/tests/summary/${encodeURIComponent(tableId)}/cases/${encodeURIComponent(caseId)}`),
        AS_JSON,
        EXECUTION_API_OPTIONS
    ) as TestUnitResult

/**
 * Starts a benchmark of a table: the cases of a test table, or the input the panel collected.
 *
 * The measurement goes on in the background and reports on its own topic. It runs the table over and over, so
 * it takes a few seconds at the least.
 */
export const startBenchmark = async (
    projectId: string,
    tableId: string,
    options: { testRanges?: string, fromModule?: string, inputJson?: string } = {}
): Promise<void> => {
    const params = withModule(new URLSearchParams({ tableId }), options.fromModule)
    if (options.testRanges) {
        params.set('testRanges', options.testRanges)
    }
    await apiCall(
        projectUrl(projectId, `/benchmarks?${params}`),
        {
            method: 'POST',
            ...(options.inputJson !== undefined && {
                headers: { 'Content-Type': 'application/json' },
                body: options.inputJson,
            }),
        },
        { ...EXECUTION_API_OPTIONS, skipWorkspaceEvent: true }
    )
}

/** Reads the measurements taken in this session, the newest first. */
export const getBenchmarks = async (projectId: string): Promise<BenchmarkResult[]> =>
    asArray<BenchmarkResult>(await apiCall(projectUrl(projectId, '/benchmarks'), AS_JSON, EXECUTION_API_OPTIONS))

/** Reads the measurements of a benchmark that has just ended, waiting out the moment it needs to publish them. */
export const readBenchmarks = (projectId: string): Promise<BenchmarkResult[]> =>
    readSettled(() => getBenchmarks(projectId))

/** Forgets the measurements named, or every one of them when none is named. */
export const deleteBenchmarks = async (projectId: string, ids: string[] = []): Promise<void> => {
    const params = new URLSearchParams()
    ids.forEach(id => params.append('id', id))
    const query = params.toString()
    const suffix = query ? `?${query}` : ''
    await apiCall(
        projectUrl(projectId, `/benchmarks${suffix}`),
        { method: 'DELETE' },
        { ...EXECUTION_API_OPTIONS, skipWorkspaceEvent: true }
    )
}

/** Reads the test results as the workbook the user saves. */
export const getTestsSummaryWorkbook = async (projectId: string, query: TestsQuery = {}): Promise<Blob> =>
    await apiCall(
        projectUrl(projectId, `/tests/summary?${testsQueryParams(query)}`),
        { headers: { Accept: XLSX_MEDIA_TYPE } },
        { ...EXECUTION_API_OPTIONS, responseType: 'blob' }
    ) as Blob

/** Reads the workbook of a test run that has just ended, waiting out the moment the run needs to publish it. */
export const readTestsSummaryWorkbook = (projectId: string, query: TestsQuery = {}): Promise<Blob> =>
    readSettled(() => getTestsSummaryWorkbook(projectId, query))

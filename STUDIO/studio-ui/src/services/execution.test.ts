import apiCall, { ApiHttpError, readTaskResult } from 'services/apiCall'
import {
    getBenchmarks,
    getRunResult,
    getRunResultWorkbook,
    getTestsSummary,
    readRunResult,
    readTestsSummaryWorkbook,
    runTests,
    startRun,
    XLSX_MEDIA_TYPE,
} from 'services/execution'
import { ResultNotReadyError } from 'services/taskResult'
import { jsonResponse } from 'testing/responses'

vi.mock('services/apiCall', async importOriginal => {
    const actual = await importOriginal<typeof import('services/apiCall')>()
    return {
        __esModule: true,
        default: vi.fn(),
        readTaskResult: vi.fn(),
        asArray: (value: unknown) => (Array.isArray(value) ? value : []),
        ApiHttpError: actual.ApiHttpError,
    }
})

/** The workbook a result endpoint answers with. */
const workbook = () => new Response('xlsx', { headers: { 'Content-Type': XLSX_MEDIA_TYPE } })

const call = apiCall as ReturnType<typeof vi.fn>
/** A result is read through the reader that tells a task still going on from a result that is there. */
const read = readTaskResult as ReturnType<typeof vi.fn>

describe('execution service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        call.mockResolvedValue({})
    })

    it('starts a run with the input the panel collected', async () => {
        await startRun('p1', 't1', '{"params":{"age":7}}', { fromModule: 'Main' })

        const [url, init] = call.mock.calls[0] as [string, RequestInit]
        expect(url).toBe('/projects/p1/run?tableId=t1&fromModule=Main')
        expect(init.method).toBe('POST')
        expect(init.body).toBe('{"params":{"age":7}}')
    })

    it('runs the tests of one table, of a rule table, or of the whole project', async () => {
        await runTests('p1', { tableId: 't1', testRanges: '1,2' })
        expect(call.mock.calls[0]?.[0]).toBe('/projects/p1/tests/run?tableId=t1&testRanges=1%2C2')

        await runTests('p1')
        expect(call.mock.calls[1]?.[0]).toBe('/projects/p1/tests/run')
    })

    it('asks the summary for what the results screen shows', async () => {
        read.mockResolvedValue(jsonResponse({ testCases: [{ tableId: 't1' }], total: 3 }))

        const summary = await getTestsSummary('p1', { failuresOnly: true, failures: 20, page: 1, size: 5 })

        const [url, init] = read.mock.calls[0] as [string, RequestInit]
        expect(url).toBe(
            '/projects/p1/tests/summary?failuresOnly=true&failures=20&compoundResult=false&page=1&size=5'
        )
        expect((init.headers as Record<string, string>)['Accept']).toBe('application/json')
        expect(summary.testCases).toHaveLength(1)
    })

    it('asks for every test table at once when no page size is set', async () => {
        read.mockResolvedValue(jsonResponse({ testCases: []}))

        await getTestsSummary('p1', { size: -1 })

        expect(read.mock.calls[0]?.[0]).toBe(
            '/projects/p1/tests/summary?failuresOnly=false&failures=5&compoundResult=false&unpaged=true'
        )
    })

    it('reads a result as the workbook the user saves, laid out the way it is asked for', async () => {
        read.mockResolvedValue(workbook())

        const saved = await getRunResultWorkbook('p1', { skipEmptyParameters: true, flattenParameters: false })

        const [url, init] = read.mock.calls[0] as [string, RequestInit]
        expect(url).toBe('/projects/p1/run/result?skipEmptyParameters=true&flattenParameters=false')
        expect((init.headers as Record<string, string>)['Accept']).toBe(XLSX_MEDIA_TYPE)
        expect(await saved.text()).toBe('xlsx')
    })

    it('asks for the table a spreadsheet result was calculated by only when a screen shows it', async () => {
        // A response is read once, so every ask is answered with one of its own.
        read.mockImplementation(async () => jsonResponse({ tableName: 'Premium' }))

        await getRunResult('p1')
        await getRunResult('p1', { spreadsheet: true })

        expect(read.mock.calls[0]?.[0]).toBe('/projects/p1/run/result?spreadsheet=false')
        expect(read.mock.calls[1]?.[0]).toBe('/projects/p1/run/result?spreadsheet=true')
    })

    it('reads the measurements as a list, even when the session has none', async () => {
        read.mockResolvedValueOnce(jsonResponse([{ id: 'm1' }]))
        read.mockResolvedValueOnce(jsonResponse(null))

        expect(await getBenchmarks('p1')).toHaveLength(1)
        expect(await getBenchmarks('p1')).toHaveLength(0)
    })

    it('waits out the moment a run needs to publish what it returned', async () => {
        // The run says it has ended a moment before its result is there: the first read finds it not ready,
        // and the next one answers.
        read.mockRejectedValueOnce(new ResultNotReadyError())
        read.mockResolvedValueOnce(jsonResponse({ tableName: 'Premium' }))

        const result = await readRunResult('p1')

        expect(result.tableName).toBe('Premium')
        expect(read).toHaveBeenCalledTimes(2)
    })

    it('waits out that moment for a result that is saved without being read first', async () => {
        read.mockRejectedValueOnce(new ResultNotReadyError())
        read.mockResolvedValueOnce(workbook())

        await readTestsSummaryWorkbook('p1')

        expect(read).toHaveBeenCalledTimes(2)
    })

    it('gives up on a run that answers anything but "not ended yet"', async () => {
        read.mockRejectedValue(new ApiHttpError(404, 'no run execution task found'))

        await expect(readRunResult('p1')).rejects.toThrow('no run execution task found')
        expect(read).toHaveBeenCalledTimes(1)
    })
})

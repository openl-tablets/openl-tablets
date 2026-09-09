import apiCall, { ApiHttpError } from 'services/apiCall'
import {
    getRunResult,
    getRunResultWorkbook,
    getTestsSummary,
    readRunResult,
    readTestsSummaryWorkbook,
    runTests,
    startRun,
    XLSX_MEDIA_TYPE,
} from 'services/execution'

vi.mock('services/apiCall', async importOriginal => {
    const actual = await importOriginal<typeof import('services/apiCall')>()
    return {
        __esModule: true,
        default: vi.fn(),
        asArray: (value: unknown) => (Array.isArray(value) ? value : []),
        isApiHttpError: actual.isApiHttpError,
        ApiHttpError: actual.ApiHttpError,
    }
})

const call = apiCall as ReturnType<typeof vi.fn>

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
        call.mockResolvedValue({ testCases: [{ tableId: 't1' }], total: 3 })

        const summary = await getTestsSummary('p1', { failuresOnly: true, failures: 20, page: 1, size: 5 })

        expect(call.mock.calls[0]?.[0]).toBe(
            '/projects/p1/tests/summary?failuresOnly=true&failures=20&compoundResult=false&page=1&size=5'
        )
        expect(summary.testCases).toHaveLength(1)
    })

    it('asks for every test table at once when no page size is set', async () => {
        call.mockResolvedValue({ testCases: []})

        await getTestsSummary('p1', { size: -1 })

        expect(call.mock.calls[0]?.[0]).toBe(
            '/projects/p1/tests/summary?failuresOnly=false&failures=5&compoundResult=false&unpaged=true'
        )
    })

    it('reads a result as the workbook the user saves, laid out the way it is asked for', async () => {
        call.mockResolvedValue(new Blob())

        await getRunResultWorkbook('p1', { skipEmptyParameters: true, flattenParameters: false })

        const [url, init, options] = call.mock.calls[0] as [string, RequestInit, Record<string, unknown>]
        expect(url).toBe('/projects/p1/run/result?skipEmptyParameters=true&flattenParameters=false')
        expect((init.headers as Record<string, string>)['Accept']).toBe(XLSX_MEDIA_TYPE)
        expect(options['responseType']).toBe('blob')
    })

    it('asks for the table a spreadsheet result was calculated by only when a screen shows it', async () => {
        call.mockResolvedValue({ tableName: 'Premium' })

        await getRunResult('p1')
        await getRunResult('p1', { spreadsheet: true })

        expect(call.mock.calls[0]?.[0]).toBe('/projects/p1/run/result?spreadsheet=false')
        expect(call.mock.calls[1]?.[0]).toBe('/projects/p1/run/result?spreadsheet=true')
    })

    it('waits out the moment a run needs to publish what it returned', async () => {
        call.mockRejectedValueOnce(new ApiHttpError(409, 'not completed'))
        call.mockResolvedValueOnce({ tableName: 'Premium' })

        const result = await readRunResult('p1')

        expect(result.tableName).toBe('Premium')
        expect(call).toHaveBeenCalledTimes(2)
    })

    it('waits out that moment for a result that is saved without being read first', async () => {
        call.mockRejectedValueOnce(new ApiHttpError(409, 'not completed'))
        call.mockResolvedValueOnce(new Blob())

        await readTestsSummaryWorkbook('p1')

        expect(call).toHaveBeenCalledTimes(2)
    })

    it('gives up on a run that answers anything but "not ended yet"', async () => {
        call.mockRejectedValue(new ApiHttpError(404, 'no run execution task found'))

        await expect(readRunResult('p1')).rejects.toThrow('no run execution task found')
        expect(call).toHaveBeenCalledTimes(1)
    })
})

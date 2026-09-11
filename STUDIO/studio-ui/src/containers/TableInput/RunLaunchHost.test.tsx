import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getProject } from 'services/repositories'
import { readRunResult, readRunResultWorkbook, readTestsSummaryWorkbook, runTests, startRun } from 'services/execution'
import { getTableInput, getTableInputCases } from 'services/tables'
import type { TableInput } from 'types/tables'
import { saveFile } from 'utils/download'
import { RunLaunchHost } from 'containers/TableInput/RunLaunchHost'

vi.mock('services/repositories', () => ({ getProject: vi.fn() }))

vi.mock('services/tables', () => ({
    getTableInput: vi.fn(),
    getTableInputCase: vi.fn(),
    getTableInputCases: vi.fn(),
    TEST_CASES_PAGE_SIZE: 25,
}))

vi.mock('services/execution', () => ({
    startRun: vi.fn(),
    runTests: vi.fn(),
    readRunResult: vi.fn(),
    readRunResultWorkbook: vi.fn(),
    readTestsSummaryWorkbook: vi.fn(),
    isStillRunning: (error: unknown) => (error as { status?: number })?.status === 409,
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

// The panel listens for the run; here it is told at once that the run has ended.
// The panel forgets what the last run reported before it starts another one.
const { reset, progress } = vi.hoisted(() => ({
    reset: vi.fn(),
    progress: {
        current: { status: 'COMPLETED' as string | null, error: null as string | null, subscribed: true },
    },
}))

vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => ({ ...progress.current, arrived: 0, reset }),
    isFinished: (status: string | null) => status !== null && ['COMPLETED', 'INTERRUPTED', 'ERROR'].includes(status),
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

// The results have their own tests; here only the window that opens for a run is of interest.
vi.mock('containers/execution/RunResultModal', () => ({
    RunResultModal: () => <div data-testid="run-result-modal" />,
}))

vi.mock('containers/execution/TestsResultModal', () => ({
    TestsResultModal: () => <div data-testid="tests-result-modal" />,
}))

// The static notification renders outside the tree and schedules a timer; the tests assert on the run.
vi.mock('antd', async importOriginal => {
    const actual = await importOriginal<typeof import('antd')>()
    return { ...actual, notification: { ...actual.notification, error: vi.fn() } }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const projectRead = getProject as ReturnType<typeof vi.fn>
const inputRead = getTableInput as ReturnType<typeof vi.fn>
const casesRead = getTableInputCases as ReturnType<typeof vi.fn>
const run = startRun as ReturnType<typeof vi.fn>
const tests = runTests as ReturnType<typeof vi.fn>
const result = readRunResult as ReturnType<typeof vi.fn>
const workbook = readRunResultWorkbook as ReturnType<typeof vi.fn>
const testsWorkbook = readTestsSummaryWorkbook as ReturnType<typeof vi.fn>
const save = saveFile as ReturnType<typeof vi.fn>

const anchor = { left: 10, top: 20, width: 40, height: 30 }

const ruleTable: TableInput = {
    tableId: 't1',
    name: 'Premium',
    testTable: false,
    parameters: [{ name: 'age', description: 'int', lazy: false, schema: { type: 'integer' } }],
}

const testTable: TableInput = { tableId: 't1', name: 'PremiumTest', testTable: true }

const open = (detail: Record<string, unknown> = {}) => act(async () => {
    window.dispatchEvent(new CustomEvent('openRunLaunch', {
        detail: { projectId: 'p1', tableId: 't1', moduleName: 'Main', anchor, ...detail },
    }))
    await new Promise(resolve => setTimeout(resolve, 20))
})

describe('RunLaunchHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        progress.current = { status: 'COMPLETED', error: null, subscribed: true }
        projectRead.mockResolvedValue({ id: 'real-p1', name: 'P' })
        run.mockResolvedValue(undefined)
        tests.mockResolvedValue(undefined)
        result.mockResolvedValue({ tableName: 'Premium', result: { premium: 100 } })
        workbook.mockResolvedValue(new Blob(['x']))
        testsWorkbook.mockResolvedValue(new Blob(['x']))
        casesRead.mockResolvedValue({
            total: 2,
            content: [
                { id: '1', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 25 }]},
                { id: '2', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 70 }]},
            ],
        })
    })

    it('runs a rule table with the input the panel collected and opens the window it reports in', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<RunLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '42{enter}')
        await userEvent.click(screen.getByTestId('run-start'))

        await waitFor(() => expect(run).toHaveBeenCalledTimes(1))
        const [projectId, tableId, inputJson] = run.mock.calls[0] as [string, string, string]
        expect(projectId).toBe('real-p1')
        expect(tableId).toBe('t1')
        expect(JSON.parse(inputJson)).toEqual({ params: { age: 42 } })
        expect(await screen.findByTestId('run-result-modal')).toBeInTheDocument()
    })

    it('runs every case of a test table, and reports them as a test run', async () => {
        inputRead.mockResolvedValue(testTable)
        render(<RunLaunchHost />)

        await open()
        await screen.findByTestId('test-cases')
        await userEvent.click(screen.getByTestId('run-start'))

        await waitFor(() => expect(tests).toHaveBeenCalledWith('real-p1', { tableId: 't1' }))
        expect(await screen.findByTestId('tests-result-modal')).toBeInTheDocument()
    })

    it('runs only the cases that are ticked', async () => {
        inputRead.mockResolvedValue(testTable)
        render(<RunLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('pick-case-2'))
        await userEvent.click(screen.getByTestId('run-start'))

        await waitFor(() => expect(tests).toHaveBeenCalledWith('real-p1', { tableId: 't1', testRanges: '2' }))
    })

    it('offers a test table the options of its results and saves them as a workbook', async () => {
        inputRead.mockResolvedValue(testTable)
        render(<RunLaunchHost />)

        await open()
        await screen.findByTestId('test-cases')

        // A test table has nothing to lay out in a workbook of a run; it has results to show.
        expect(screen.queryByTestId('run-resultInJson')).toBeNull()
        expect(screen.getByTestId('tests-failures-only')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('tests-compound-result'))
        await userEvent.click(screen.getByTestId('run-into-file'))

        await waitFor(() => expect(testsWorkbook).toHaveBeenCalledWith('real-p1', {
            failuresOnly: false,
            compoundResult: true,
        }))
        expect(save).toHaveBeenCalledWith(expect.any(Blob), 'test-results.xlsx', 'application/xlsx')
    })

    it('writes the run into a workbook, laid out the way the options ask for', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<RunLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('run-skipEmptyParameters'))
        await userEvent.click(screen.getByTestId('run-into-file'))

        await waitFor(() => expect(workbook).toHaveBeenCalledWith('real-p1', {
            skipEmptyParameters: true,
            flattenParameters: true,
            resultInJson: false,
        }))
        expect(save).toHaveBeenCalledWith(expect.any(Blob), 'run-result.xlsx', 'application/xlsx')
        expect(screen.queryByTestId('run-result-modal')).toBeNull()
    })

    it('writes the returned value into a JSON file when it is asked for', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<RunLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('run-resultInJson'))
        await userEvent.click(screen.getByTestId('run-into-file'))

        await waitFor(() => expect(save).toHaveBeenCalledWith(
            JSON.stringify({ premium: 100 }, null, 2), 'response.json', 'application/json'
        ))
        expect(workbook).not.toHaveBeenCalled()
    })

    it('says why a run that failed saved no file', async () => {
        progress.current = { status: 'ERROR', error: 'Division by zero', subscribed: true }
        inputRead.mockResolvedValue(ruleTable)
        render(<RunLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('run-into-file'))

        expect(await screen.findByTestId('launch-error')).toHaveTextContent('Division by zero')
        // Nothing was produced, so nothing is asked for.
        expect(workbook).not.toHaveBeenCalled()
        expect(save).not.toHaveBeenCalled()
    })

    it('runs a table that takes nothing at once, without asking', async () => {
        const { parameters: _parameters, ...bare } = ruleTable
        inputRead.mockResolvedValue(bare)
        render(<RunLaunchHost />)

        await open()

        await waitFor(() => expect(run).toHaveBeenCalledTimes(1))
        expect(run.mock.calls[0]?.[2]).toBe('{}')
        expect(screen.queryByTestId('run-start')).toBeNull()
    })

    it('asks for the runtime context of a table that takes no parameters', async () => {
        const { parameters: _parameters, ...bare } = ruleTable
        inputRead.mockResolvedValue({
            ...bare,
            runtimeContext: { name: 'runtimeContext', description: 'IRulesRuntimeContext', lazy: false },
        })
        render(<RunLaunchHost />)

        await open()

        // The rules read the context even when the table itself takes nothing, so the panel opens for it.
        expect(await screen.findByTestId('run-start')).toBeInTheDocument()
        expect(run).not.toHaveBeenCalled()
    })

    it('runs within the current module while the project is loading', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<RunLaunchHost />)

        await open({ moduleOnlyLocked: true })
        const moduleOnly = await screen.findByTestId('launch-module-only')
        expect(moduleOnly).toBeChecked()
        await userEvent.click(screen.getByTestId('run-start'))

        await waitFor(() => expect(run).toHaveBeenCalledWith('real-p1', 't1', expect.any(String), { fromModule: 'Main' }))
    })
    it('reads the result once, however often the panel is nudged while the read is on its way', async () => {
        // The connection drops and comes back while the workbook is being read. Another read could find the
        // result as well, and the file would be saved twice.
        progress.current = { status: 'STARTED', error: null, subscribed: true }
        inputRead.mockResolvedValue(ruleTable)
        workbook.mockImplementation(() => new Promise<Blob>(resolve => {
            setTimeout(() => resolve(new Blob(['x'])), 20)
        }))
        const { rerender } = render(<RunLaunchHost />)
        await open()

        await userEvent.click(await screen.findByTestId('run-into-file'))
        expect(workbook).toHaveBeenCalledTimes(1)
        progress.current = { status: 'COMPLETED', error: null, subscribed: false }
        rerender(<RunLaunchHost />)
        progress.current = { status: 'COMPLETED', error: null, subscribed: true }
        rerender(<RunLaunchHost />)

        await waitFor(() => expect(save).toHaveBeenCalledTimes(1))
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 40))
        })
        expect(workbook).toHaveBeenCalledTimes(1)
        expect(save).toHaveBeenCalledTimes(1)
    })

    it('asks for the run result when the panel stops hearing it, and waits while it is not there', async () => {
        // The run has not said it has ended and the connection carrying that message is gone. The panel asks
        // for the result; told that the run is still going on, it keeps waiting rather than reporting.
        progress.current = { status: 'STARTED', error: null, subscribed: true }
        inputRead.mockResolvedValue(ruleTable)
        workbook.mockRejectedValue(Object.assign(new Error('still running'), { status: 409 }))
        render(<RunLaunchHost />)
        await open()
        await screen.findByTestId('run-into-file')
        progress.current = { status: 'STARTED', error: null, subscribed: false }

        await userEvent.click(screen.getByTestId('run-into-file'))

        await waitFor(() => expect(workbook).toHaveBeenCalled())
        expect(save).not.toHaveBeenCalled()
        expect(screen.queryByTestId('launch-error')).toBeNull()
    })
})

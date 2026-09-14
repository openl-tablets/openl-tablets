import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { readTestsSummary, getTestCaseResult, getTestsSummaryWorkbook } from 'services/execution'
import { openTableInEditor, tableUrl } from 'services/tableNavigation'
import { saveFile } from 'utils/download'
import { TestsResultModal } from 'containers/execution/TestsResultModal'

vi.mock('services/execution', () => ({
    readTestsSummary: vi.fn(),
    isStillRunning: () => false,
    getTestCaseResult: vi.fn(),
    getTestsSummaryWorkbook: vi.fn(),
    ALL_TESTS_ON_A_PAGE: -1,
    FAILURES_PER_TEST: 5,
    FAILURES_PER_TEST_OPTIONS: [1, 5, 20],
    TESTS_PAGE_SIZE: 20,
    TESTS_PAGE_SIZES: [1, 5, 20, -1],
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

const saved = vi.hoisted(() => ({ profile: null as Record<string, unknown> | null }))

vi.mock('store', () => ({
    useUserStore: (select: (state: unknown) => unknown) => select({ userProfile: saved.profile }),
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

vi.mock('services/tableNavigation', () => ({ openTableInEditor: vi.fn(), tableUrl: vi.fn() }))



vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => ({ status: null, error: null, arrived: 0, subscribed: true }),
    isFinished: () => false,
}))

// The dialog renders as plain markup: jsdom cannot measure a real one, and the values are what matters.
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({ title, children, footer }: {
        title?: React.ReactNode
        children?: React.ReactNode
        footer?: React.ReactNode
    }) => (
        <div role="dialog">
            <div data-testid="modal-title">{title}</div>
            {children}
            <div data-testid="modal-footer">{footer}</div>
        </div>
    )
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const readSummary = readTestsSummary as ReturnType<typeof vi.fn>
const readWorkbook = getTestsSummaryWorkbook as ReturnType<typeof vi.fn>
const readCase = getTestCaseResult as ReturnType<typeof vi.fn>
const save = saveFile as ReturnType<typeof vi.fn>
const openTable = openTableInEditor as ReturnType<typeof vi.fn>
const readUrl = tableUrl as ReturnType<typeof vi.fn>

const driver = (name: string) => [{ name: 'driver', description: 'Driver', lazy: false, value: name }]

const summary = {
    testCases: [{
        name: 'DriverPremiumTest',
        tableId: 'tt1',
        executionTimeMs: 5,
        numberOfTests: 2,
        numberOfFailures: 1,
        testUnits: [
            {
                id: '1',
                status: 'TR_OK',
                executionTimeMs: 1,
                parameters: driver('Sara'),
                testAssertions: [{ description: 'Premium', expectedValue: 100, actualValue: 100, status: 'TR_OK' }],
            },
            {
                id: '2',
                status: 'TR_NEQ',
                executionTimeMs: 1,
                parameters: driver('Bob'),
                testAssertions: [{ description: 'Premium', expectedValue: 200, actualValue: 150, status: 'TR_NEQ' }],
            },
        ],
    }],
    pageNumber: 0,
    pageSize: 20,
    numberOfElements: 1,
    total: 1,
    executionTimeMs: 5,
    numberOfTests: 2,
    numberOfFailures: 1,
}

const show = async () => act(async () => {
    render(<TestsResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)
    await new Promise(resolve => setTimeout(resolve, 20))
})

describe('TestsResultModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        saved.profile = null
        readSummary.mockResolvedValue(summary)
        readWorkbook.mockResolvedValue(new Blob(['x']))
        readUrl.mockResolvedValue(null)
    })

    it('waits for the tests while they are still running', async () => {
        readSummary.mockReturnValue(new Promise(() => undefined))
        render(<TestsResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('tests.running')).toBeInTheDocument()
    })

    it('shows a case to a row, with a column for what it was given and for what came out', async () => {
        await show()

        const table = screen.getByTestId('test-results-tt1')
        expect(table).toHaveTextContent('tests.id')
        expect(table).toHaveTextContent('Driver')
        expect(table).toHaveTextContent('Premium')
        expect(table).toHaveTextContent('"Sara"')
        expect(table).toHaveTextContent('150')
    })

    it('shows the value that was expected under the value of a case that failed', async () => {
        await show()

        const rows = screen.getByTestId('test-results-tt1').querySelectorAll('tbody tr')
        expect(rows[0]).not.toHaveTextContent('tests.expected')
        expect(rows[1]).toHaveTextContent('tests.expected')
        expect(rows[1]).toHaveTextContent('200')
    })

    it('says the outcome of the whole table by the colour of its name', async () => {
        await show()

        expect(screen.getByTestId('test-table-tt1')).toHaveClass('ant-typography-danger')

        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{ ...summary.testCases[0], numberOfFailures: 0 }],
        })
        await show()

        expect(screen.getAllByTestId('test-table-tt1').at(-1)).toHaveClass('ant-typography-success')
    })

    it('shows only the name of a table the screen is set to leave out', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{ ...summary.testCases[0], numberOfFailures: 0, testUnits: []}],
        })

        await show()

        expect(screen.getByTestId('test-table-tt1')).toBeInTheDocument()
        expect(screen.queryByTestId('test-results-tt1')).toBeNull()
    })

    it('carries the address of the test table its name stands for', async () => {
        const onClose = vi.fn()
        readUrl.mockResolvedValue('#design/Project/Module/table?id=tt1')
        await act(async () => {
            render(<TestsResultModal onClose={onClose} projectId="p1" tableId="t1" />)
            await new Promise(resolve => setTimeout(resolve, 20))
        })

        expect(screen.getByTestId('test-table-tt1')).toHaveAttribute('href', '#design/Project/Module/table?id=tt1')

        await userEvent.click(screen.getByTestId('test-table-tt1'))

        // The browser follows the address itself; the window only steps aside.
        expect(openTable).not.toHaveBeenCalled()
        await waitFor(() => expect(onClose).toHaveBeenCalled())
    })

    it('opens the test table by asking for its address when it is not known yet', async () => {
        const onClose = vi.fn()
        readUrl.mockReturnValue(new Promise(() => undefined))
        openTable.mockResolvedValue(true)
        await act(async () => {
            render(<TestsResultModal onClose={onClose} projectId="p1" tableId="t1" />)
            await new Promise(resolve => setTimeout(resolve, 20))
        })

        await userEvent.click(screen.getByTestId('test-table-tt1'))

        await waitFor(() => expect(openTable).toHaveBeenCalledWith('tt1'))
        await waitFor(() => expect(onClose).toHaveBeenCalled())
    })

    it('marks the case and every comparison of it with a tick or a cross', async () => {
        await show()

        const rows = screen.getByTestId('test-results-tt1').querySelectorAll('tbody tr')
        expect(rows[0]?.querySelectorAll('[title="tests.passed"]')).toHaveLength(2)
        expect(rows[1]?.querySelectorAll('[title="tests.failed"]')).toHaveLength(2)
        // The case says how many of its comparisons did not match.
        expect(rows[1]).toHaveTextContent('(1)')
    })

    it('keeps the compared columns when the first case listed ended with an error', async () => {
        // A case that threw compares nothing, so the columns are read from the case that did.
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [
                    {
                        id: '1',
                        status: 'TR_EXCEPTION',
                        executionTimeMs: 1,
                        parameters: driver('Sara'),
                        errors: [{ summary: 'Division by zero', severity: 'ERROR' }],
                    },
                    summary.testCases[0]!.testUnits[1],
                ],
            }],
        })
        await show()

        const table = screen.getByTestId('test-results-tt1')
        expect(table).toHaveTextContent('Premium')
        expect(table).toHaveTextContent('150')
        expect(table).toHaveTextContent('Division by zero')
    })

    it('marks nothing in a Run table, which states no expected values', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{ ...summary.testCases[0], runTable: true }],
        })

        await show()

        expect(screen.queryByTitle('tests.failed')).toBeNull()
        expect(screen.queryByTitle('tests.passed')).toBeNull()
        expect(screen.getByTestId('test-results-tt1')).not.toHaveTextContent('(1)')
    })

    it('asks again for the results that the screen is set to show', async () => {
        await show()

        await userEvent.click(screen.getByTestId('tests-failures-only'))

        await waitFor(() => expect(readSummary).toHaveBeenLastCalledWith('p1', expect.objectContaining({
            failuresOnly: true,
            page: 0,
        })))
    })

    it('saves the results as the workbook the screen shows', async () => {
        await show()

        await userEvent.click(screen.getByTestId('tests-save'))

        await waitFor(() => expect(readWorkbook).toHaveBeenCalledWith('p1', expect.objectContaining({ size: 20 })))
        expect(save).toHaveBeenCalledWith(expect.any(Blob), 'test-results.xlsx', 'application/xlsx')
    })

    it('starts from the settings the user saved', async () => {
        saved.profile = {
            testsFailuresOnly: true,
            testsFailuresPerTest: 20,
            showComplexResult: true,
            testsPerPage: -1,
        }

        await show()

        expect(readSummary).toHaveBeenCalledWith('p1', {
            failuresOnly: true,
            failures: 20,
            compoundResult: true,
            lazyValues: true,
            page: 0,
            size: -1,
        })
    })

    it('reads a value the results only referred to when it is asked for', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[0],
                    parameters: [{ name: 'driver', description: 'Driver', lazy: true }],
                }],
            }],
        })
        readCase.mockResolvedValue({
            id: '1',
            parameters: [{ name: 'driver', description: 'Driver', lazy: false, value: { name: 'Sara' } }],
        })

        await show()
        await userEvent.click(screen.getByTestId('load-tt1-in-1-0'))

        await waitFor(() => expect(readCase).toHaveBeenCalledWith('p1', 'tt1', '1'))
        expect(await screen.findByText('{1 fields}')).toBeInTheDocument()
    })

    it('reads the whole returned value only when the compound result is asked for', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[0],
                    result: { name: 'result', lazy: true },
                }],
            }],
        })
        readCase.mockResolvedValue({
            id: '1',
            result: { name: 'result', lazy: false, value: { premium: 100 } },
        })

        await show()
        await userEvent.click(screen.getByTestId('tests-compound-result'))
        await userEvent.click(await screen.findByTestId('load-tt1-whole-1'))

        await waitFor(() => expect(readCase).toHaveBeenCalledWith('p1', 'tt1', '1'))
        expect(await screen.findByText('{1 fields}')).toBeInTheDocument()
    })

    it('says that a new read failed instead of leaving the results it did not replace', async () => {
        await show()
        readSummary.mockRejectedValue(new Error('The project is being compiled'))

        await userEvent.click(screen.getByTestId('tests-failures-only'))

        expect(await screen.findByTestId('tests-read-failed')).toHaveTextContent('The project is being compiled')
        // The results that were read last are still there to read.
        expect(screen.getByTestId('test-results-tt1')).toBeInTheDocument()
    })

    it('says why the results could not be read', async () => {
        readSummary.mockRejectedValue(new Error('No tests execution task found'))
        render(<TestsResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('No tests execution task found')).toBeInTheDocument()
    })
})

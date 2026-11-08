import { MemoryRouter } from 'react-router'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { onTestFinished } from 'vitest'
import { layOut } from 'testing/layOut'
import {
    getTestCaseLines, getTestsSummary, getTestsSummaryWorkbook, readTestsSummary, stopExecution,
} from 'services/execution'
import { ResultNotReadyError } from 'services/taskResult'
import { saveFile } from 'utils/download'
import { TestsResults } from 'containers/execution/TestsResults'
import { ExecutionWindowContext } from 'containers/execution/ExecutionScreen'
import { useQuietSpells } from 'containers/execution/useExecutionProgress'
import * as look from 'components/values/parameterValues.styles'
import { DEFAULT_USER_SETTINGS, useSettingsStore } from 'store/settingsStore'

vi.mock('services/execution', () => ({
    getTestsSummary: vi.fn(),
    readTestsSummary: vi.fn(),
    getTestCaseLines: vi.fn(),
    getTestsSummaryWorkbook: vi.fn(),
    stopExecution: vi.fn(),
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

// What the run reported, and how many spells it has said nothing for: a test that needs either sets it.
const run = vi.hoisted(() => ({ status: null as string | null, quietSpells: 0 }))

vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => ({ status: run.status, error: null, arrived: 0, subscribed: true }),
    isFinished: (status: string | null) => status === 'COMPLETED',
    useQuietSpells: vi.fn(() => run.quietSpells),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// The one read the window makes while the run has not said it ended.
const readSummary = getTestsSummary as ReturnType<typeof vi.fn>
// The read that waits out the moment the results of a run that said it ended need to be published.
const readEndedSummary = readTestsSummary as ReturnType<typeof vi.fn>
const readWorkbook = getTestsSummaryWorkbook as ReturnType<typeof vi.fn>
const readLines = getTestCaseLines as ReturnType<typeof vi.fn>
const save = saveFile as ReturnType<typeof vi.fn>
const stop = stopExecution as ReturnType<typeof vi.fn>

const driver = (name: string) => [{ name: 'driver', description: 'Driver', lazy: false, value: name }]

const summary = {
    testCases: [{
        name: 'DriverPremiumTest',
        tableId: 'tt1',
        module: 'Auto Policy Tests',
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

/** The results of one test table of the given number of cases, every one of them passed. */
const manyCases = (count: number) => ({
    ...summary,
    testCases: [{
        ...summary.testCases[0],
        numberOfTests: count,
        numberOfFailures: 0,
        testUnits: Array.from({ length: count }, (_, index) => ({
            id: String(index + 1),
            status: 'TR_OK',
            executionTimeMs: 1,
            parameters: driver(`Driver ${index + 1}`),
            testAssertions: [{ description: 'Premium', expectedValue: 100, actualValue: 100, status: 'TR_OK' }],
        })),
    }],
})

/** The cases the table of the results lists now. */
const listedCases = () => screen.getByTestId('test-results-tt1').querySelectorAll('tbody tr')

/** Turns the pager under the table of the results to the given page. */
const turnTo = async (page: number) => {
    const item = within(screen.getByTestId('test-cases-pagination-tt1')).getByTitle(String(page))
    return userEvent.click(within(item).getByRole('button'))
}

const show = async () => {
    render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)
    await screen.findByTestId('tests-save')
}

describe('TestsResults', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useSettingsStore.setState({ settings: DEFAULT_USER_SETTINGS })
        run.status = null
        run.quietSpells = 0
        readSummary.mockResolvedValue(summary)
        readWorkbook.mockResolvedValue(new Blob(['x']))
    })

    it('names the field of a column whose description another column shares', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    id: '2',
                    status: 'TR_NEQ',
                    executionTimeMs: 1,
                    parameters: driver('Bob'),
                    testAssertions: [
                        { description: 'Rate', field: '$ManualRate', expectedValue: 2.99, actualValue: 2.68, status: 'TR_NEQ' },
                        { description: 'Rate', field: '$FormulaRate', expectedValue: 2.68, actualValue: 2.68, status: 'TR_OK' },
                    ],
                }],
            }],
        })
        await show()

        expect(screen.getByText('Rate ($ManualRate)')).toBeInTheDocument()
        expect(screen.getByText('Rate ($FormulaRate)')).toBeInTheDocument()
    })

    it('names the result columns without a description by the field they compare', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    id: '2',
                    status: 'TR_NEQ',
                    executionTimeMs: 1,
                    parameters: driver('Bob'),
                    testAssertions: [
                        { field: '$ManualRate', expectedValue: 2.99, actualValue: 2.68, status: 'TR_NEQ' },
                        { field: '$FormulaRate', expectedValue: 2.68, actualValue: 2.68, status: 'TR_OK' },
                    ],
                }],
            }],
        })
        await show()

        expect(screen.getByText('tests.result ($ManualRate)')).toBeInTheDocument()
        expect(screen.getByText('tests.result ($FormulaRate)')).toBeInTheDocument()
    })

    it('waits for the tests while they are still running', async () => {
        readSummary.mockReturnValue(new Promise(() => undefined))
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        expect(await screen.findByText('tests.running')).toBeInTheDocument()
    })

    it('stops polling when the saved test results are gone', async () => {
        readSummary.mockRejectedValue({ status: 404 })
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        expect(await screen.findByText('noResults')).toBeInTheDocument()
        expect(useQuietSpells).toHaveBeenLastCalledWith(expect.anything(), false)
        expect(readSummary).toHaveBeenCalledTimes(1)
    })

    it('reads the results once while the tests go on, and again when the run said nothing for a while', async () => {
        // Tests still going on answer one read, and the window waits for the run to say more rather than
        // asking over and over.
        readSummary.mockRejectedValue(new ResultNotReadyError())
        const window = () => (
            <MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>
        )
        const { rerender } = render(window())
        expect(await screen.findByText('tests.running')).toBeInTheDocument()
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 20))
        })
        expect(readSummary).toHaveBeenCalledTimes(1)

        // A run that ended just as the window started listening reports its end to nobody.
        readSummary.mockResolvedValue(summary)
        run.quietSpells = 1
        rerender(window())

        expect(await screen.findByTestId('test-results-tt1')).toBeInTheDocument()
        expect(readSummary).toHaveBeenCalledTimes(2)
        expect(readEndedSummary).not.toHaveBeenCalled()
    })

    it('waits out the results of tests that said they ended', async () => {
        run.status = 'COMPLETED'
        readEndedSummary.mockResolvedValue(summary)

        await show()

        expect(screen.getByTestId('test-results-tt1')).toBeInTheDocument()
        expect(readEndedSummary).toHaveBeenCalledTimes(1)
        expect(readSummary).not.toHaveBeenCalled()
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

    it('reads the look of the values as often however many tables it lists', async () => {
        const readLook = vi.spyOn(look, 'useStyles')
        const readsFor = async (tables: number) => {
            readLook.mockClear()
            readSummary.mockResolvedValue({
                ...summary,
                testCases: Array.from({ length: tables }, (_, index) => ({
                    ...summary.testCases[0],
                    tableId: `tt${index}`,
                })),
            })
            const view = render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" /></MemoryRouter>)
            expect(await screen.findByTestId(`test-results-tt${tables - 1}`)).toBeInTheDocument()
            view.unmount()
            return readLook.mock.calls.length
        }
        try {
            expect(await readsFor(3)).toBe(await readsFor(1))
        } finally {
            readLook.mockRestore()
        }
    })

    it('shows the value that was expected under the value of a case that failed', async () => {
        await show()

        const rows = screen.getByTestId('test-results-tt1').querySelectorAll('tbody tr')
        expect(rows[0]).not.toHaveTextContent('tests.expected')
        expect(rows[1]).toHaveTextContent('tests.expected')
        expect(rows[1]).toHaveTextContent('200')
    })

    it('shows a fractional value as the short decimal its binary result stands for', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[1],
                    parameters: [{ name: 'rate', description: 'Rate', lazy: false, value: 0.30000000000000004 }],
                    testAssertions: [
                        { description: 'num1', expectedValue: 20.76, actualValue: 20.759999999999998, status: 'TR_OK' },
                        {
                            description: 'num2',
                            expectedValue: 0.08333,
                            actualValue: 0.08333999999999997,
                            status: 'TR_NEQ',
                        },
                    ],
                }],
            }],
        })

        await show()

        const row = screen.getByTestId('test-results-tt1').querySelectorAll('tbody tr')[0]
        expect(row).toHaveTextContent('0.3')
        expect(row).not.toHaveTextContent('0.30000000000000004')
        expect(row).toHaveTextContent('20.76')
        expect(row).not.toHaveTextContent('20.759999999999998')
        expect(row).toHaveTextContent('0.08334')
        expect(row).not.toHaveTextContent('0.08333999999999997')
        expect(row).toHaveTextContent('0.08333')
    })

    it('shows a failed comparison exactly when its short decimals would read the same', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[1],
                    testAssertions: [{
                        description: 'num2',
                        expectedValue: 0.08334,
                        actualValue: 0.08333999999999997,
                        status: 'TR_NEQ',
                    }],
                }],
            }],
        })

        await show()

        const row = screen.getByTestId('test-results-tt1').querySelectorAll('tbody tr')[0]
        expect(row).toHaveTextContent('0.08333999999999997')
        expect(row?.textContent?.split('0.08334')).toHaveLength(2)
    })

    it('shows a failed comparison of a value with inner structure exactly', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[1],
                    testAssertions: [{ description: 'Premium', expectedLazy: true, actualLazy: true, status: 'TR_NEQ' }],
                }],
            }],
        })
        readLines.mockResolvedValue({ total: 1, lines: [{ name: 'premium', segment: 'premium', value: 20.759999999999998 }]})

        await show()
        await userEvent.click(await screen.findByTestId('load-tt1-out-2-0'))
        const line = (await screen.findByText('{1 fields}')).closest('.ant-tree-treenode')
        await userEvent.click(line!.querySelector('.ant-tree-switcher')!)

        expect(await screen.findByText('20.759999999999998')).toBeInTheDocument()
        expect(screen.queryByText('20.76')).toBeNull()
    })

    it('shows a fractional value read inside a compared value as a short decimal too', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[0],
                    testAssertions: [{ description: 'Premium', expectedValue: 100, actualLazy: true, status: 'TR_OK' }],
                }],
            }],
        })
        readLines.mockResolvedValue({ total: 1, lines: [{ name: 'premium', segment: 'premium', value: 20.759999999999998 }]})

        await show()
        await userEvent.click(await screen.findByTestId('load-tt1-out-1-0'))
        const line = (await screen.findByText('{1 fields}')).closest('.ant-tree-treenode')
        await userEvent.click(line!.querySelector('.ant-tree-switcher')!)

        expect(await screen.findByText('20.76')).toBeInTheDocument()
        expect(screen.queryByText('20.759999999999998')).toBeNull()
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

    it('lists the cases of a large test table a page at a time', async () => {
        readSummary.mockResolvedValue(manyCases(45))
        await show()

        expect(listedCases()).toHaveLength(20)
        expect(listedCases()[0]).toHaveTextContent('"Driver 1"')

        await turnTo(3)

        expect(listedCases()).toHaveLength(5)
        expect(listedCases()[0]).toHaveTextContent('"Driver 41"')
    })

    it('lists every case of a table that fits on one page, with no pager', async () => {
        await show()

        expect(listedCases()).toHaveLength(2)
        expect(screen.queryByTestId('test-cases-pagination-tt1')).toBeNull()
    })

    it('lists the cases from the first page again when the screen is set to list other cases', async () => {
        readSummary.mockResolvedValue(manyCases(45))
        await show()
        await turnTo(2)
        expect(listedCases()[0]).toHaveTextContent('"Driver 21"')

        await userEvent.click(screen.getByTestId('tests-failures-only'))

        await waitFor(() => expect(readSummary).toHaveBeenLastCalledWith('p1', expect.objectContaining({
            failuresOnly: true,
        })))
        await waitFor(() => expect(listedCases()[0]).toHaveTextContent('"Driver 1"'))
    })

    it('keeps the page of cases when only the compound result is added', async () => {
        readSummary.mockResolvedValue(manyCases(45))
        await show()
        await turnTo(2)

        await userEvent.click(screen.getByTestId('tests-compound-result'))

        // The compound result adds a column to every case listed; it does not change which cases are listed.
        await waitFor(() => expect(screen.getByTestId('test-results-tt1')).toHaveTextContent('tests.compoundResult'))
        expect(listedCases()[0]).toHaveTextContent('"Driver 21"')
    })

    it('leads to the test table, through the module it is written in, in the editor', async () => {
        const openTable = vi.fn(() => true)
        render(
            <MemoryRouter>
                <ExecutionWindowContext.Provider value={{ openTable }}>
                    <TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" />
                </ExecutionWindowContext.Provider>
            </MemoryRouter>
        )

        // The editor opens a module and reads the table through it; the results already name both.
        expect(await screen.findByTestId('test-table-tt1'))
            .toHaveAttribute('href', '/projects/p1/modules/Auto%20Policy%20Tests?table=tt1')

        await userEvent.click(screen.getByTestId('test-table-tt1'))

        expect(openTable).toHaveBeenCalledWith('/projects/p1/modules/Auto%20Policy%20Tests?table=tt1')
    })

    it('writes a table whose module is unknown as its name alone', async () => {
        const { module: _module, ...withoutModule } = summary.testCases[0] as { module?: string }
        readSummary.mockResolvedValue({ ...summary, testCases: [withoutModule]})
        await show()

        // There is no screen to open it with, so the name is not offered as a way in.
        expect(screen.getByTestId('test-table-tt1')).not.toHaveAttribute('href')
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

        await waitFor(() => expect(readWorkbook).toHaveBeenCalledWith('p1', expect.objectContaining({ size: 5 })))
        expect(save).toHaveBeenCalledWith(expect.any(Blob), 'test-results.xlsx', 'application/xlsx')
    })

    it('starts from the settings the user saved', async () => {
        useSettingsStore.setState({
            settings: { ...DEFAULT_USER_SETTINGS, testsFailuresOnly: true, showComplexResult: true, testsPerPage: -1 },
        })

        await show()

        expect(readSummary).toHaveBeenCalledWith('p1', {
            failuresOnly: true,
            failures: 5,
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
                    parameters: [{ name: 'driver', description: 'Driver', lazy: true, type: 'Driver', key: 'Sara' }],
                }],
            }],
        })
        readLines.mockResolvedValue({
            type: 'Driver',
            total: 1,
            lines: [{ name: 'name', segment: 'name', value: 'Sara' }],
        })

        await show()
        // Until it is read, the value is known by its type and the key its data table row is referred to by.
        expect(screen.getByTestId('summary-tt1-in-1-0')).toHaveTextContent('Driver (Sara)')
        await userEvent.click(screen.getByTestId('load-tt1-in-1-0'))

        // The first level of the value is read, and nothing below it.
        await waitFor(() => expect(readLines)
            .toHaveBeenCalledWith('p1', 'tt1', '1', { of: 'parameter', index: 0 }, [], 0))
        // Read, the value comes closed and keeps the key it is known by as its title instead of a count of its fields.
        await waitFor(() => expect(screen.queryByTestId('load-tt1-in-1-0')).toBeNull())
        expect(screen.getByText('Driver (Sara)')).toBeInTheDocument()
        expect(screen.queryByText('"Sara"')).toBeNull()
        expect(screen.queryByText('{1 fields}')).toBeNull()
    })

    it('shows what every case of a Run table returned, with no compound result asked for', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                runTable: true,
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[0],
                    testAssertions: [],
                    result: { name: 'result', lazy: false, value: 'Good Morning' },
                }],
            }],
        })

        await show()

        // The result stands under its own heading, as the returned value of the case rather than a comparison.
        expect(screen.getByText('tests.result')).toBeInTheDocument()
        expect(screen.getByText('"Good Morning"')).toBeInTheDocument()
        expect(screen.queryByText('tests.compoundResult', { selector: 'th *' })).toBeNull()
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
        readLines.mockResolvedValue({ total: 1, lines: [{ name: 'premium', segment: 'premium', value: 100 }]})

        await show()
        await userEvent.click(screen.getByTestId('tests-compound-result'))
        await userEvent.click(await screen.findByTestId('load-tt1-whole-1'))

        await waitFor(() => expect(readLines).toHaveBeenCalledWith('p1', 'tt1', '1', { of: 'result' }, [], 0))
        expect(await screen.findByText('{1 fields}')).toBeInTheDocument()
    })

    it('reads a compared value the server let go of with its case', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[0],
                    testAssertions: [{ description: 'Premium', expectedValue: 100, actualLazy: true, status: 'TR_OK' }],
                }],
            }],
        })
        readLines.mockResolvedValue({ total: 1, lines: [{ name: 'premium', segment: 'premium', value: 100 }]})

        await show()
        await userEvent.click(await screen.findByTestId('load-tt1-out-1-0'))

        await waitFor(() => expect(readLines)
            .toHaveBeenCalledWith('p1', 'tt1', '1', { of: 'assertion', index: 0 }, [], 0))
        expect(await screen.findByText('{1 fields}')).toBeInTheDocument()
    })

    it('reads what a failed comparison expected when it is only referred to', async () => {
        readSummary.mockResolvedValue({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: [{
                    ...summary.testCases[0]?.testUnits[1],
                    testAssertions: [
                        { description: 'Premium', actualValue: 150, expectedLazy: true, status: 'TR_NEQ' },
                    ],
                }],
            }],
        })
        readLines.mockResolvedValue({ total: 1, lines: [{ name: 'premium', segment: 'premium', value: 200 }]})

        await show()
        await userEvent.click(await screen.findByTestId('load-tt1-exp-2-0'))

        await waitFor(() => expect(readLines)
            .toHaveBeenCalledWith('p1', 'tt1', '2', { of: 'expected', index: 0 }, [], 0))
        expect(await screen.findByText('{1 fields}')).toBeInTheDocument()
    })

    it('shows the compound result only once the results read with it are there', async () => {
        await show()
        let answer: (summary: unknown) => void = () => undefined
        readSummary.mockReturnValue(new Promise(resolve => {
            answer = resolve
        }))

        await userEvent.click(screen.getByTestId('tests-compound-result'))

        // The results on screen were read without it: a column over them would hold nothing.
        expect(screen.getByTestId('test-results-tt1')).not.toHaveTextContent('tests.compoundResult')
        await act(async () => answer({
            ...summary,
            testCases: [{
                ...summary.testCases[0],
                testUnits: summary.testCases[0]!.testUnits
                    .map(unit => ({ ...unit, result: { name: 'result', lazy: true } })),
            }],
        }))
        expect(screen.getByTestId('test-results-tt1')).toHaveTextContent('tests.compoundResult')
    })

    it('leaves the whole value of a case that returned none empty', async () => {
        useSettingsStore.setState({ settings: { ...DEFAULT_USER_SETTINGS, showComplexResult: true } })

        await show()

        const table = screen.getByTestId('test-results-tt1')
        expect(table).toHaveTextContent('tests.compoundResult')
        expect(table).not.toHaveTextContent('undefined')
    })

    it('says that a new read failed instead of leaving the results it did not replace', async () => {
        await show()
        readSummary.mockRejectedValue(new Error('The project is being compiled'))

        await userEvent.click(screen.getByTestId('tests-failures-only'))

        expect(await screen.findByTestId('tests-read-failed')).toHaveTextContent('The project is being compiled')
        // The results that were read last are still there to read.
        expect(screen.getByTestId('test-results-tt1')).toBeInTheDocument()
    })

    it('reports 404 after a page change while keeping the last readable results', async () => {
        await show()
        readSummary.mockRejectedValue(Object.assign(new Error('Results released'), { status: 404 }))

        await userEvent.click(screen.getByTestId('tests-failures-only'))

        expect(await screen.findByTestId('tests-read-failed')).toHaveTextContent('Results released')
        expect(screen.getByTestId('test-results-tt1')).toBeInTheDocument()
    })

    it('says why the results could not be read', async () => {
        readSummary.mockRejectedValue(new Error('No tests execution task found'))
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        expect(await screen.findByText('No tests execution task found')).toBeInTheDocument()
    })

    it('stops the tests the reader asks to stop, and says they were stopped', async () => {
        readSummary.mockReturnValue(new Promise(() => undefined))
        stop.mockResolvedValue(undefined)
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        await userEvent.click(await screen.findByTestId('execution-stop'))

        expect(stop).toHaveBeenCalledWith('p1', 'tests')
        expect(await screen.findByTestId('tests-stopped')).toHaveTextContent('tests.stopped')
        expect(screen.queryByTestId('execution-stop')).not.toBeInTheDocument()
        expect(useQuietSpells).toHaveBeenLastCalledWith(expect.anything(), false)
    })

    it('says the tests stopped when they report they were interrupted', async () => {
        run.status = 'INTERRUPTED'
        readSummary.mockReturnValue(new Promise(() => undefined))
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        expect(await screen.findByTestId('tests-stopped')).toBeInTheDocument()
    })

    it('shows the tests that ran before the run stopped by itself, and says it stopped', async () => {
        run.status = 'INTERRUPTED'

        await show()

        expect(screen.getByTestId('tests-stopped')).toHaveTextContent('tests.stoppedPartial')
        expect(screen.getByTestId('test-results-tt1')).toBeInTheDocument()
    })

    it('says why the tests could not be stopped, and goes on waiting for them', async () => {
        readSummary.mockReturnValue(new Promise(() => undefined))
        stop.mockRejectedValue(new Error('The server is not available'))
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        await userEvent.click(await screen.findByTestId('execution-stop'))

        expect(await screen.findByTestId('execution-stop-failed')).toHaveTextContent('The server is not available')
        expect(screen.getByText('tests.running')).toBeInTheDocument()
    })

    it('says the project changed when the server dropped the results for it', async () => {
        readSummary.mockRejectedValue(Object.assign(new Error('The project has changed'), { status: 410 }))
        render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

        expect(await screen.findByTestId('tests-outdated')).toHaveTextContent('The project has changed')
        expect(screen.queryByTestId('execution-stop')).not.toBeInTheDocument()
        expect(useQuietSpells).toHaveBeenLastCalledWith(expect.anything(), false)
    })

    it('stops showing the results on screen once the server dropped them', async () => {
        await show()
        readSummary.mockRejectedValue(Object.assign(new Error('The project has changed'), { status: 410 }))

        await userEvent.click(screen.getByTestId('tests-failures-only'))

        expect(await screen.findByTestId('tests-outdated')).toHaveTextContent('The project has changed')
        expect(screen.queryByTestId('test-results-tt1')).not.toBeInTheDocument()
    })

    // A page can hold every test table of a project; drawn at once, their cells took the browser down. EPBDS-17091.
    describe('on a page of many test tables', () => {
        /** A page of the given number of test tables, each a copy of the table given. */
        const manyTables = (count: number, table: object = summary.testCases[0]!) => ({
            ...summary,
            testCases: Array.from({ length: count }, (_, index) => ({ ...table, tableId: `t${index}` })),
        })
        const listedTables = () => [...document.querySelectorAll<HTMLElement>('[data-testid^="test-results-t"]')]
            .map(table => table.dataset['testid'])

        /**
         * Lays the window out 768 pixels tall, as jsdom says, a table as tall as one not drawn yet is taken to be, and
         * answers how to scroll it, telling the list it scrolled.
         */
        const laidOutWindow = () => {
            let scrolled = 0
            layOut(document.documentElement, 'clientHeight', () => 768)
            layOut(HTMLDivElement.prototype, 'offsetHeight', () => 400)
            layOut(window, 'scrollY', () => scrolled)
            // The list keeps the place the reader scrolled to as tables are measured, which jsdom cannot scroll to.
            const scrollTo = window.scrollTo
            window.scrollTo = vi.fn() as typeof window.scrollTo
            onTestFinished(() => {
                window.scrollTo = scrollTo
            })
            return (to: number) => act(() => {
                scrolled = to
                window.dispatchEvent(new Event('scroll'))
            })
        }

        it('draws the tables in sight as the window scrolls, not the ones far below', async () => {
            const scrollTo = laidOutWindow()
            readSummary.mockResolvedValue(manyTables(30))
            render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

            await waitFor(() => expect(listedTables()).toContain('test-results-t0'))
            expect(listedTables().length).toBeLessThan(10)
            expect(listedTables()).not.toContain('test-results-t29')

            scrollTo(27 * 400)

            await waitFor(() => expect(listedTables()).toContain('test-results-t29'))
            expect(listedTables()).not.toContain('test-results-t0')
        })

        it('keeps the page of cases a table was turned to while it is scrolled out of sight and back', async () => {
            const scrollTo = laidOutWindow()
            readSummary.mockResolvedValue(manyTables(30, manyCases(45).testCases[0] ?? {}))
            render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)
            await waitFor(() => expect(listedTables()).toContain('test-results-t0'))
            const item = within(screen.getByTestId('test-cases-pagination-t0')).getByTitle('2')
            await userEvent.click(within(item).getByRole('button'))
            expect(within(screen.getByTestId('test-results-t0')).getByText('21')).toBeInTheDocument()

            scrollTo(27 * 400)
            await waitFor(() => expect(listedTables()).not.toContain('test-results-t0'))
            scrollTo(0)

            await waitFor(() => expect(listedTables()).toContain('test-results-t0'))
            expect(within(screen.getByTestId('test-results-t0')).getByText('21')).toBeInTheDocument()
        })

        it('draws every table where the window has no layout to measure', async () => {
            readSummary.mockResolvedValue(manyTables(30))
            render(<MemoryRouter><TestsResults onClose={vi.fn()} projectId="p1" tableId="t1" /></MemoryRouter>)

            await waitFor(() => expect(listedTables()).toHaveLength(30))
        })
    })
})

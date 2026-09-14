import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getProject } from 'services/repositories'
import { startBenchmark } from 'services/execution'
import { getTableInput, getTableInputCases } from 'services/tables'
import type { TableInput } from 'types/tables'
import { BenchmarkLaunchHost } from 'containers/TableInput/BenchmarkLaunchHost'

vi.mock('services/repositories', () => ({ getProject: vi.fn() }))

vi.mock('services/tables', () => ({
    getTableInput: vi.fn(),
    getTableInputCase: vi.fn(),
    getTableInputCases: vi.fn(),
    TEST_CASES_PAGE_SIZE: 25,
}))

vi.mock('services/execution', () => ({ startBenchmark: vi.fn() }))

// The results have their own tests; here only the window that opens for a measurement is of interest.
vi.mock('containers/execution/BenchmarkResultModal', () => ({
    BenchmarkResultModal: () => <div data-testid="benchmark-result-modal" />,
}))

// The static notification renders outside the tree and schedules a timer; the tests assert on the measurement.
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
const benchmark = startBenchmark as ReturnType<typeof vi.fn>

const anchor = { left: 10, top: 20, width: 40, height: 30 }

const ruleTable: TableInput = {
    tableId: 't1',
    name: 'Premium',
    testTable: false,
    parameters: [{ name: 'age', description: 'int', lazy: false, schema: { type: 'integer' } }],
}

const testTable: TableInput = { tableId: 't1', name: 'PremiumTest', testTable: true }

const open = (detail: Record<string, unknown> = {}) => act(async () => {
    window.dispatchEvent(new CustomEvent('openBenchmarkLaunch', {
        detail: { projectId: 'p1', tableId: 't1', moduleName: 'Main', anchor, ...detail },
    }))
    await new Promise(resolve => setTimeout(resolve, 20))
})

describe('BenchmarkLaunchHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        projectRead.mockResolvedValue({ id: 'real-p1', name: 'P' })
        benchmark.mockResolvedValue(undefined)
        casesRead.mockResolvedValue({
            total: 2,
            content: [
                { id: '1', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 25 }]},
                { id: '2', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 70 }]},
            ],
        })
    })

    it('measures a rule table over the input the panel collected', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<BenchmarkLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '42{enter}')
        await userEvent.click(screen.getByTestId('benchmark-start'))

        await waitFor(() => expect(benchmark).toHaveBeenCalledTimes(1))
        const [projectId, tableId, options] = benchmark.mock.calls[0] as [string, string, { inputJson: string }]
        expect(projectId).toBe('real-p1')
        expect(tableId).toBe('t1')
        expect(JSON.parse(options.inputJson)).toEqual({ params: { age: 42 } })
        expect(await screen.findByTestId('benchmark-result-modal')).toBeInTheDocument()
    })

    it('measures a test table over every one of its cases at once', async () => {
        inputRead.mockResolvedValue(testTable)
        render(<BenchmarkLaunchHost />)

        await open()
        await screen.findByTestId('test-cases')
        await userEvent.click(screen.getByTestId('benchmark-start'))

        await waitFor(() => expect(benchmark).toHaveBeenCalledWith('real-p1', 't1', {}))
        expect(await screen.findByTestId('benchmark-result-modal')).toBeInTheDocument()
    })

    it('measures only the cases that are ticked, and within the module when that is asked for', async () => {
        inputRead.mockResolvedValue(testTable)
        render(<BenchmarkLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('pick-case-2'))
        await userEvent.click(screen.getByTestId('launch-module-only'))
        await userEvent.click(screen.getByTestId('benchmark-start'))

        await waitFor(() => expect(benchmark).toHaveBeenCalledWith('real-p1', 't1', {
            testRanges: '2',
            fromModule: 'Main',
        }))
    })

    it('measures a table that asks for nothing without a panel', async () => {
        inputRead.mockResolvedValue({ tableId: 't1', name: 'Premium', testTable: false })
        render(<BenchmarkLaunchHost />)

        await open()

        await waitFor(() => expect(benchmark).toHaveBeenCalledWith('real-p1', 't1', { inputJson: '{}' }))
        expect(screen.queryByTestId('benchmark-start')).toBeNull()
    })

    it('says why the measurement could not be started', async () => {
        inputRead.mockResolvedValue(testTable)
        benchmark.mockRejectedValue(new Error('The project is not compiled'))
        render(<BenchmarkLaunchHost />)

        await open()
        await screen.findByTestId('test-cases')
        await userEvent.click(screen.getByTestId('benchmark-start'))

        expect(await screen.findByText('The project is not compiled')).toBeInTheDocument()
    })
})

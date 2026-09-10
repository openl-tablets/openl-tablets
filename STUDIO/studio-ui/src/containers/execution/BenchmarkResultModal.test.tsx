import React from 'react'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { deleteBenchmarks, getBenchmarks, readBenchmarks } from 'services/execution'
import { BenchmarkResultModal } from 'containers/execution/BenchmarkResultModal'
import type { BenchmarkResult } from 'types/execution'

vi.mock('services/execution', () => ({
    readBenchmarks: vi.fn(),
    getBenchmarks: vi.fn(),
    deleteBenchmarks: vi.fn(),
    isStillRunning: () => false,
}))

vi.mock('services/tableNavigation', () => ({
    tableUrl: vi.fn().mockResolvedValue('#design/Project/Module/table?id=t1'),
    openTableInEditor: vi.fn(),
}))

// What the measurement reports while the window is open. A test that follows a measurement to its end sets it.
const { progress } = vi.hoisted(() => ({
    progress: { current: { status: null as string | null, error: null as string | null, arrived: 0, subscribed: true } },
}))

vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => progress.current,
    isFinished: (status: string | null) => status !== null && ['COMPLETED', 'INTERRUPTED', 'ERROR'].includes(status),
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

const read = readBenchmarks as ReturnType<typeof vi.fn>
const readAgain = getBenchmarks as ReturnType<typeof vi.fn>
const forget = deleteBenchmarks as ReturnType<typeof vi.fn>

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

const show = async (measurements: BenchmarkResult[]) => {
    read.mockResolvedValue(measurements)
    readAgain.mockResolvedValue(measurements)
    await act(async () => {
        render(<BenchmarkResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)
        await new Promise(resolve => setTimeout(resolve, 20))
    })
}

describe('BenchmarkResultModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        progress.current = { status: null, error: null, arrived: 0, subscribed: true }
    })

    it('waits for the measurement while it is still going on', async () => {
        readAgain.mockReturnValue(new Promise(() => undefined))
        render(<BenchmarkResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('benchmark.running')).toBeInTheDocument()
    })

    it('reports the speed of every measurement, with the input of the measured case', async () => {
        await show([measurement({ runs: 512, testCases: 3, executionTimeMs: 3500 })])

        const table = screen.getByTestId('benchmark-table')
        expect(table).toHaveTextContent('PolicyTest')
        expect(table).toHaveTextContent('tests.cases')
        // 3500 ms over 512 runs of 3 test cases: 6.84 ms a run, 2.28 ms a test case.
        expect(table).toHaveTextContent('6.84')
        expect(table).toHaveTextContent('2.28')
        expect(table).toHaveTextContent('439')
        expect(table).toHaveTextContent('146')
    })

    it('shows the input of a measured test case', async () => {
        const driver = { name: 'driver', description: 'Driver', lazy: false, value: 'Sara' }
        await show([measurement({ parameters: [driver]})])

        expect(screen.getByTestId('benchmark-table')).toHaveTextContent('Driver')
        expect(screen.getByText('"Sara"')).toBeInTheDocument()
    })

    it('places the ticked measurements by speed, the fastest first', async () => {
        // The newest measurement comes first in the list, and it is the slower of the two.
        await show([
            measurement({ id: 'm2', name: 'Slow', runs: 500, executionTimeMs: 4000 }),
            measurement({ id: 'm1', name: 'Fast', runs: 1000, executionTimeMs: 4000 }),
        ])

        await userEvent.click(screen.getByTestId('benchmark-pick-all'))
        await userEvent.click(screen.getByTestId('benchmark-compare'))

        const comparison = screen.getByTestId('benchmark-comparison')
        const rows = within(comparison).getAllByRole('row').slice(1)
        expect(rows[0]).toHaveTextContent('Fast')
        expect(rows[0]).toHaveTextContent('1.00')
        expect(rows[1]).toHaveTextContent('Slow')
        expect(rows[1]).toHaveTextContent('2.00')
    })

    it('forgets the ticked measurements', async () => {
        await show([measurement({ id: 'm1' }), measurement({ id: 'm2' })])
        forget.mockResolvedValue(undefined)
        // What is left once the ticked measurement is forgotten.
        readAgain.mockResolvedValue([measurement({ id: 'm2' })])

        await userEvent.click(screen.getByTestId('benchmark-pick-m1'))
        await userEvent.click(screen.getByTestId('benchmark-delete'))

        await waitFor(() => expect(forget).toHaveBeenCalledWith('p1', ['m1']))
        await waitFor(() => expect(screen.getByTestId('benchmark-pick-m2')).toBeInTheDocument())
        expect(screen.queryByTestId('benchmark-pick-m1')).toBeNull()
    })

    it('reads the measurements the moment the benchmark reports it has ended', async () => {
        progress.current = { status: 'COMPLETED', error: null, arrived: 0, subscribed: true }
        read.mockResolvedValue([measurement()])

        await act(async () => {
            render(<BenchmarkResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)
            await new Promise(resolve => setTimeout(resolve, 20))
        })

        // The read waits out the moment the benchmark needs to publish what it measured.
        expect(read).toHaveBeenCalledWith('p1')
        expect(readAgain).not.toHaveBeenCalled()
        expect(screen.getByTestId('benchmark-table')).toHaveTextContent('PolicyTest')
    })

    it('says that a benchmark failed instead of showing the measurements before it', async () => {
        progress.current = { status: 'ERROR', error: 'Division by zero', arrived: 0, subscribed: true }
        read.mockResolvedValue([measurement()])
        readAgain.mockResolvedValue([measurement()])

        await act(async () => {
            render(<BenchmarkResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)
            await new Promise(resolve => setTimeout(resolve, 20))
        })

        expect(screen.getByText('Division by zero')).toBeInTheDocument()
        expect(screen.getByText('benchmark.failed')).toBeInTheDocument()
        // The rows of the benchmarks before it are not the result of the one that failed.
        expect(screen.queryByTestId('benchmark-table')).toBeNull()
        expect(read).not.toHaveBeenCalled()
        expect(readAgain).not.toHaveBeenCalled()
    })

    it('says why the measurement could not be read', async () => {
        readAgain.mockRejectedValue(new Error('No benchmark found'))
        render(<BenchmarkResultModal onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('No benchmark found')).toBeInTheDocument()
        expect(screen.getByText('benchmark.failed')).toBeInTheDocument()
    })
})

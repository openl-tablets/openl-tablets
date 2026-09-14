import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { readRunResult, getRunResultWorkbook } from 'services/execution'
import { saveFile } from 'utils/download'
import { RunResultModal } from 'containers/execution/RunResultModal'

vi.mock('services/execution', () => ({
    readRunResult: vi.fn(),
    isStillRunning: () => false,
    getRunResultWorkbook: vi.fn(),
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

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

const readResult = readRunResult as ReturnType<typeof vi.fn>
const readWorkbook = getRunResultWorkbook as ReturnType<typeof vi.fn>
const save = saveFile as ReturnType<typeof vi.fn>

const value = { tableName: 'DetermineVehiclePremium', executionTimeMs: 12.4 }

const show = async (result: Record<string, unknown>) => {
    readResult.mockResolvedValue(result)
    await act(async () => {
        render(<RunResultModal fileOptions={{ skipEmptyParameters: true }} onClose={vi.fn()} projectId="p1" tableId="t1" />)
        await new Promise(resolve => setTimeout(resolve, 20))
    })
}

describe('RunResultModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        readWorkbook.mockResolvedValue(new Blob(['x']))
    })

    it('waits for the run while it is still on its way', async () => {
        readResult.mockReturnValue(new Promise(() => undefined))
        render(<RunResultModal fileOptions={{}} onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('run.running')).toBeInTheDocument()
    })

    it('shows the input the table ran with and the value it returned, a column each', async () => {
        await show({
            ...value,
            result: 1200,
            parameters: [{ name: 'vehicle', description: 'Vehicle', lazy: false, value: 'Toyota' }],
            contextParameters: [{ name: 'currentDate', description: 'Current Date', lazy: false, value: '2026-01-01' }],
        })

        const table = screen.getByTestId('run-result-table')
        expect(table).toHaveTextContent('Current Date')
        expect(table).toHaveTextContent('Vehicle')
        expect(table).toHaveTextContent('run.result')
        expect(table).toHaveTextContent('"Toyota"')
        expect(table).toHaveTextContent('1200')
    })

    it('shows a spreadsheet result as the table it was calculated by', async () => {
        await show({
            ...value,
            result: { Value_Premium: 1200 },
            resultSpreadsheet: {
                columns: ['Description', 'Value'],
                rows: ['Premium'],
                cells: [['Total premium', 1200]],
            },
        })

        const grid = screen.getByTestId('spreadsheet-run-result')
        expect(grid).toHaveTextContent('run.step')
        expect(grid).toHaveTextContent('Description')
        expect(grid).toHaveTextContent('Premium')
        expect(grid).toHaveTextContent('"Total premium"')
        expect(grid).toHaveTextContent('1200')
    })

    it('saves the result as the workbook, laid out the way the run asked for', async () => {
        await show({ ...value, result: 1 })

        await userEvent.click(screen.getByTestId('run-save'))

        await waitFor(() => expect(readWorkbook).toHaveBeenCalledWith('p1', { skipEmptyParameters: true }))
        expect(save).toHaveBeenCalledWith(expect.any(Blob), 'run-result.xlsx', 'application/xlsx')
    })

    it('says why the result could not be read', async () => {
        readResult.mockRejectedValue(new Error('No run execution task found'))
        render(<RunResultModal fileOptions={{}} onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('No run execution task found')).toBeInTheDocument()
        expect(screen.getByText('run.failed')).toBeInTheDocument()
    })
})

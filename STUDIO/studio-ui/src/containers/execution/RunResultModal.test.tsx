import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getRunResult, getRunResultWorkbook, readRunResult } from 'services/execution'
import { ResultNotReadyError } from 'services/taskResult'
import { saveFile } from 'utils/download'
import { RunResultModal } from 'containers/execution/RunResultModal'

vi.mock('services/execution', () => ({
    getRunResult: vi.fn(),
    readRunResult: vi.fn(),
    getRunResultWorkbook: vi.fn(),
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

// What the run reported, and how many spells it has said nothing for: a test that needs either sets it.
const run = vi.hoisted(() => ({ status: null as string | null, quietSpells: 0 }))

vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => ({ status: run.status, error: null, arrived: 0, subscribed: true }),
    isFinished: (status: string | null) => status === 'COMPLETED',
    useQuietSpells: () => run.quietSpells,
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

// The one read the window makes while the run has not said it ended.
const readResult = getRunResult as ReturnType<typeof vi.fn>
// The read that waits out the moment the result of a run that said it ended needs to be published.
const readEndedResult = readRunResult as ReturnType<typeof vi.fn>
const readWorkbook = getRunResultWorkbook as ReturnType<typeof vi.fn>
const save = saveFile as ReturnType<typeof vi.fn>

const value = { tableName: 'DetermineVehiclePremium', executionTimeMs: 12.4 }

const show = async (result: Record<string, unknown>) => {
    readResult.mockResolvedValue(result)
    render(<RunResultModal fileOptions={{ skipEmptyParameters: true }} onClose={vi.fn()} projectId="p1" tableId="t1" />)
    await screen.findByTestId('run-save')
}

describe('RunResultModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        run.status = null
        run.quietSpells = 0
        readWorkbook.mockResolvedValue(new Blob(['x']))
    })

    it('waits for the run while it is still on its way', async () => {
        readResult.mockReturnValue(new Promise(() => undefined))
        render(<RunResultModal fileOptions={{}} onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByText('run.running')).toBeInTheDocument()
    })

    it('reads the result once while the run goes on, and again when the run said nothing for a while', async () => {
        // A run still going on answers one read, and the window waits for the run to say more rather than
        // asking over and over.
        readResult.mockRejectedValue(new ResultNotReadyError())
        const window = () => <RunResultModal fileOptions={{}} onClose={vi.fn()} projectId="p1" tableId="t1" />
        const { rerender } = render(window())
        expect(await screen.findByText('run.running')).toBeInTheDocument()
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 20))
        })
        expect(readResult).toHaveBeenCalledTimes(1)

        // A run that ended just as the window started listening reports its end to nobody.
        readResult.mockResolvedValue({ ...value, result: 1200 })
        run.quietSpells = 1
        rerender(window())

        expect(await screen.findByTestId('run-result-table')).toBeInTheDocument()
        expect(readResult).toHaveBeenCalledTimes(2)
        expect(readEndedResult).not.toHaveBeenCalled()
    })

    it('waits out the result of a run that said it ended', async () => {
        run.status = 'COMPLETED'
        readEndedResult.mockResolvedValue({ ...value, result: 1200 })
        render(<RunResultModal fileOptions={{}} onClose={vi.fn()} projectId="p1" tableId="t1" />)

        expect(await screen.findByTestId('run-result-table')).toBeInTheDocument()
        expect(readEndedResult).toHaveBeenCalledWith('p1', { spreadsheet: true })
        expect(readResult).not.toHaveBeenCalled()
    })

    it('shows the input the table ran with and the value it returned, a column each', async () => {
        await show({
            ...value,
            result: 1200,
            parameters: [{ name: 'vehicle', description: 'Vehicle', lazy: false, value: 'Toyota' }],
            contextParameters: [{ name: 'runtimeContext', lazy: false, value: { caRegion: 'QC', locale: 'fr_CA' } }],
        })

        const table = screen.getByTestId('run-result-table')
        // The runtime context comes first, named the way the input form names it.
        const headers = [...table.querySelectorAll('th')].map(header => header.textContent)
        expect(headers).toEqual(['input.runtimeContext', 'Vehicle', 'run.result'])
        expect(table).toHaveTextContent('{2 fields}')
        expect(table).toHaveTextContent('"Toyota"')
        expect(table).toHaveTextContent('1200')
    })

    it('shows the error of a failed run in the Result column, as the result of the run', async () => {
        await show({
            ...value,
            parameters: [{ name: 'name', description: 'String', lazy: false, value: 'UNKNOWN' }],
            errors: [{ id: 1, summary: 'The name UNKNOWN is not supported', severity: 'ERROR' }],
        })

        const table = screen.getByTestId('run-result-table')
        const cells = [...table.querySelectorAll('td')]
        expect(cells.at(-1)).toHaveTextContent('The name UNKNOWN is not supported')
        // The error is the result of the run, not a banner above it, and no missing value is shown beside it.
        expect(screen.getAllByTestId('execution-error')).toHaveLength(1)
        expect(table).not.toHaveTextContent('undefined')
    })

    it('shows a null result and a null input as null, not as undefined', async () => {
        // The API leaves a null value out of the result.
        await show({ ...value, parameters: [{ name: 'name', description: 'String', lazy: false }]})

        const table = screen.getByTestId('run-result-table')
        expect([...table.querySelectorAll('td')].map(cell => cell.textContent)).toEqual(['null', 'null'])
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

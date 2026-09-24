import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getProject } from 'services/repositories'
import { getTestsSummaryWorkbook, readTestsSummaryWorkbook, runTests } from 'services/execution'
import { ResultNotReadyError } from 'services/taskResult'
import { saveFile } from 'utils/download'
import { TestsLaunchHost } from 'containers/TableInput/TestsLaunchHost'

vi.mock('services/repositories', () => ({ getProject: vi.fn() }))

vi.mock('services/execution', () => ({
    runTests: vi.fn(),
    getTestsSummaryWorkbook: vi.fn(),
    readTestsSummaryWorkbook: vi.fn(),
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

// The panel listens for the run; here it is told at once that the run has ended, unless a test says
// otherwise. The panel forgets what the last run reported before it starts another one.
const { reset, reported } = vi.hoisted(() => ({
    reset: vi.fn(),
    reported: { status: 'COMPLETED' as string | null, subscribed: true, quietSpells: 0 },
}))

vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => ({
        status: reported.status,
        error: null,
        arrived: 0,
        subscribed: reported.subscribed,
        reset,
    }),
    isFinished: (status: string | null) => status === 'COMPLETED',
    useQuietSpells: () => reported.quietSpells,
}))

// The results have their own tests; here only the window that opens for a test run is of interest.
vi.mock('containers/execution/TestsResultModal', () => ({
    TestsResultModal: () => <div data-testid="tests-result-modal" />,
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const projectRead = getProject as ReturnType<typeof vi.fn>
const tests = runTests as ReturnType<typeof vi.fn>
const workbook = readTestsSummaryWorkbook as ReturnType<typeof vi.fn>
// The one read the panel makes while the tests have not said they ended.
const probe = getTestsSummaryWorkbook as ReturnType<typeof vi.fn>
const save = saveFile as ReturnType<typeof vi.fn>

const anchor = { left: 10, top: 20, width: 40, height: 30 }

const open = (detail: Record<string, unknown> = {}) => act(async () => {
    window.dispatchEvent(new CustomEvent('openTestsLaunch', {
        detail: { projectId: 'p1', moduleName: 'Main', anchor, ...detail },
    }))
    await new Promise(resolve => setTimeout(resolve, 20))
})

describe('TestsLaunchHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        reported.status = 'COMPLETED'
        reported.subscribed = true
        reported.quietSpells = 0
        projectRead.mockResolvedValue({ id: 'real-p1', name: 'P' })
        tests.mockResolvedValue(undefined)
        workbook.mockResolvedValue(new Blob(['x']))
        probe.mockResolvedValue(new Blob(['x']))
    })

    it('asks what the results should show before the tests are run', async () => {
        render(<TestsLaunchHost />)

        await open()
        await screen.findByTestId('tests-start')

        expect(screen.getByTestId('tests-per-page')).toBeInTheDocument()
        expect(screen.getByTestId('tests-failures-only')).toBeInTheDocument()
        expect(screen.getByTestId('tests-compound-result')).toBeInTheDocument()
    })

    it('leaves the paging out for the tests of one table, which are a page of their own', async () => {
        render(<TestsLaunchHost />)

        await open({ tableId: 't1' })
        await screen.findByTestId('tests-start')

        expect(screen.queryByTestId('tests-per-page')).toBeNull()
    })

    it('saves the results as a workbook without showing them', async () => {
        render(<TestsLaunchHost />)

        await open({ tableId: 't1' })
        await userEvent.click(await screen.findByTestId('tests-compound-result'))
        await userEvent.click(screen.getByTestId('tests-into-file'))

        await waitFor(() => expect(workbook).toHaveBeenCalledWith('real-p1', expect.objectContaining({
            compoundResult: true,
        })))
        expect(save).toHaveBeenCalledWith(expect.any(Blob), 'test-results.xlsx', 'application/xlsx')
        expect(screen.queryByTestId('tests-result-modal')).toBeNull()
        // What an earlier run reported must not be taken for the end of this one.
        expect(reset).toHaveBeenCalled()
    })

    it('runs every test that tests the table the page shows', async () => {
        render(<TestsLaunchHost />)

        await open({ tableId: 't1' })
        await userEvent.click(await screen.findByTestId('tests-start'))

        await waitFor(() => expect(tests).toHaveBeenCalledWith('real-p1', { tableId: 't1' }))
        expect(await screen.findByTestId('tests-result-modal')).toBeInTheDocument()
    })

    it('runs every test of the project when no table is named, within the module on request', async () => {
        render(<TestsLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('tests-module-only'))
        await userEvent.click(screen.getByTestId('tests-start'))

        await waitFor(() => expect(tests).toHaveBeenCalledWith('real-p1', { fromModule: 'Main' }))
        expect(await screen.findByTestId('tests-result-modal')).toBeInTheDocument()
    })

    it('keeps the panel open with the reason when the tests cannot be started', async () => {
        tests.mockRejectedValueOnce(new Error('the project is being compiled'))
        render(<TestsLaunchHost />)

        await open({ tableId: 't1' })
        await userEvent.click(await screen.findByTestId('tests-start'))

        expect(await screen.findByTestId('tests-launch-error')).toHaveTextContent('the project is being compiled')
        expect(screen.queryByTestId('tests-result-modal')).toBeNull()
    })
    it('asks for the results when the panel stops hearing the run', async () => {
        // The run has not said it has ended, and the connection carrying that message is gone. The panel
        // asks for the results rather than waiting for a message that is not coming.
        reported.status = 'STARTED'
        render(<TestsLaunchHost />)
        await open({ tableId: 't1' })
        await screen.findByTestId('tests-into-file')
        reported.subscribed = false

        await userEvent.click(screen.getByTestId('tests-into-file'))

        await waitFor(() => expect(probe).toHaveBeenCalled())
        await waitFor(() => expect(save).toHaveBeenCalled())
    })

    it('reads the results once, however often the panel is nudged while the read is on its way', async () => {
        // The connection drops and comes back while the workbook is being read. Another read could find the
        // results as well, and the workbook would be saved twice.
        reported.status = 'STARTED'
        probe.mockImplementation(() => new Promise<Blob>(resolve => {
            setTimeout(() => resolve(new Blob(['x'])), 20)
        }))
        const { rerender } = render(<TestsLaunchHost />)
        await open({ tableId: 't1' })
        await screen.findByTestId('tests-into-file')

        await userEvent.click(screen.getByTestId('tests-into-file'))
        await waitFor(() => expect(probe).toHaveBeenCalledTimes(1))
        reported.subscribed = false
        rerender(<TestsLaunchHost />)
        reported.status = 'COMPLETED'
        reported.subscribed = true
        rerender(<TestsLaunchHost />)

        await waitFor(() => expect(save).toHaveBeenCalledTimes(1))
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 40))
        })
        expect(probe).toHaveBeenCalledTimes(1)
        expect(workbook).not.toHaveBeenCalled()
        expect(save).toHaveBeenCalledTimes(1)
    })

    it('reads the results once while the tests go on, and waits them out once they said they ended', async () => {
        // Tests still going on answer one read. Once they say they ended, the read waits out the moment the
        // results need to be published.
        reported.status = 'STARTED'
        probe.mockRejectedValue(new ResultNotReadyError())
        const { rerender } = render(<TestsLaunchHost />)
        await open({ tableId: 't1' })

        await userEvent.click(await screen.findByTestId('tests-into-file'))
        await waitFor(() => expect(probe).toHaveBeenCalledTimes(1))
        expect(workbook).not.toHaveBeenCalled()
        reported.status = 'COMPLETED'
        rerender(<TestsLaunchHost />)

        await waitFor(() => expect(save).toHaveBeenCalledTimes(1))
        expect(workbook).toHaveBeenCalledTimes(1)
        expect(probe).toHaveBeenCalledTimes(1)
    })

    it('asks for the results again when the tests said nothing for a while', async () => {
        // The end of the tests can be lost on the way, and nothing else would make the panel ask again.
        reported.status = 'STARTED'
        probe.mockRejectedValue(new ResultNotReadyError())
        const { rerender } = render(<TestsLaunchHost />)
        await open({ tableId: 't1' })
        await userEvent.click(await screen.findByTestId('tests-into-file'))
        await waitFor(() => expect(probe).toHaveBeenCalledTimes(1))

        probe.mockResolvedValue(new Blob(['x']))
        reported.quietSpells = 1
        rerender(<TestsLaunchHost />)

        await waitFor(() => expect(save).toHaveBeenCalledTimes(1))
        expect(probe).toHaveBeenCalledTimes(2)
    })

    it('goes on waiting when the results are not there to save yet', async () => {
        // Asked for while the tests are still running, the results answer that they are not ready. That is
        // not a failure: the panel keeps waiting rather than reporting one.
        reported.status = 'STARTED'
        probe.mockRejectedValue(new ResultNotReadyError())
        render(<TestsLaunchHost />)
        await open({ tableId: 't1' })
        await screen.findByTestId('tests-into-file')
        reported.subscribed = false

        await userEvent.click(screen.getByTestId('tests-into-file'))

        await waitFor(() => expect(probe).toHaveBeenCalled())
        expect(save).not.toHaveBeenCalled()
        expect(screen.queryByTestId('launch-error')).toBeNull()
    })
})

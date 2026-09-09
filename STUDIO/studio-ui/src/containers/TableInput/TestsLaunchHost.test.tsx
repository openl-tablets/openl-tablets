import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getProject } from 'services/repositories'
import { readTestsSummaryWorkbook, runTests } from 'services/execution'
import { saveFile } from 'utils/download'
import { TestsLaunchHost } from 'containers/TableInput/TestsLaunchHost'

vi.mock('services/repositories', () => ({ getProject: vi.fn() }))

vi.mock('services/execution', () => ({
    runTests: vi.fn(),
    readTestsSummaryWorkbook: vi.fn(),
    ALL_TESTS_ON_A_PAGE: -1,
    TESTS_PAGE_SIZE: 20,
    TESTS_PAGE_SIZES: [1, 5, 20, -1],
    XLSX_MEDIA_TYPE: 'application/xlsx',
}))

vi.mock('utils/download', () => ({ saveFile: vi.fn() }))

// The panel listens for the run; here it is told at once that the run has ended.
// The panel forgets what the last run reported before it starts another one.
const { reset } = vi.hoisted(() => ({ reset: vi.fn() }))

vi.mock('containers/execution/useExecutionProgress', () => ({
    useExecutionProgress: () => ({ status: 'COMPLETED', error: null, arrived: 0, subscribed: true, reset }),
    isFinished: (status: string | null) => status === 'COMPLETED',
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
        projectRead.mockResolvedValue({ id: 'real-p1', name: 'P' })
        tests.mockResolvedValue(undefined)
        workbook.mockResolvedValue(new Blob(['x']))
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
})

import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { getProject } from 'services/repositories'
import { getTableInput, getTableInputCase, getTableInputCases } from 'services/tables'
import { launchTrace } from 'services/traceLaunch'
import { useUserStore } from 'store'
import type { TableInput } from 'types/tables'
import { TraceLaunchHost } from 'containers/TableInput/TraceLaunchHost'

vi.mock('services/repositories', () => ({
    getProject: vi.fn(),
}))

vi.mock('services/tables', () => ({
    getTableInput: vi.fn(),
    getTableInputCase: vi.fn(),
    getTableInputCases: vi.fn(),
    TEST_CASES_PAGE_SIZE: 25,
}))

vi.mock('services/traceLaunch', () => ({
    launchTrace: vi.fn(),
}))

// The static `notification` renders into a global holder outside the component tree and schedules an
// auto-close timer that would fire after jsdom is gone. The tests assert on the launch, not the toast.
vi.mock('antd', async (importOriginal) => {
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
const caseRead = getTableInputCase as ReturnType<typeof vi.fn>
const launch = launchTrace as ReturnType<typeof vi.fn>

const anchor = { left: 10, top: 20, width: 40, height: 30 }

const ruleTable: TableInput = {
    tableId: 't1',
    name: 'Premium',
    testTable: false,
    parameters: [{ name: 'age', description: 'int', lazy: false, schema: { type: 'integer' } }],
}

const testTable: TableInput = {
    tableId: 't1',
    name: 'PremiumTest',
    testTable: true,
}

const casesPage = {
    total: 2,
    content: [
        { id: '1', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 25 }]},
        { id: '2', parameters: [{ name: 'age', description: 'Age', lazy: false, value: 70 }]},
    ],
}

const open = (detail: Record<string, unknown> = {}) => act(async () => {
    window.dispatchEvent(new CustomEvent('openTraceLaunch', {
        detail: { projectId: 'p1', tableId: 't1', moduleName: 'Main', anchor, ...detail },
    }))
    await new Promise(resolve => setTimeout(resolve, 20))
})

const launchRequest = () => launch.mock.calls[0]?.[0] as Record<string, unknown>

describe('TraceLaunchHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        projectRead.mockResolvedValue({ id: 'real-p1', name: 'P' })
        casesRead.mockResolvedValue(casesPage)
        launch.mockResolvedValue(undefined)
        useUserStore.setState({ userProfile: { showRealNumbers: true } as never })
    })

    it('collects the parameters of a rule table and starts the trace with them', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<TraceLaunchHost />)

        await open()

        // The project is read by the id the page sent. The table input is read by the project's real id.
        expect(projectRead).toHaveBeenCalledWith('p1', expect.anything(), expect.anything())
        expect(inputRead).toHaveBeenCalledWith('real-p1', 't1', {})
        await userEvent.click(await screen.findByTestId('edit-age'))
        await userEvent.type(screen.getByTestId('input-age'), '42{enter}')
        await userEvent.click(screen.getByTestId('trace-start'))

        await waitFor(() => expect(launch).toHaveBeenCalledTimes(1))
        const request = launchRequest()
        expect(request).toMatchObject({ projectId: 'real-p1', tableId: 't1', download: false, advanced: false, showRealNumbers: true })
        expect(request['fromModule']).toBeUndefined()
        expect(JSON.parse(request['inputJson'] as string)).toEqual({ params: { age: 42 } })
        // A started trace closes the launcher.
        await waitFor(() => expect(screen.queryByTestId('trace-start')).toBeNull())
    })

    it('passes the options along: the module only and the advanced debugger, or the file download', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<TraceLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('launch-module-only'))
        await userEvent.click(screen.getByTestId('trace-advanced'))
        await userEvent.click(screen.getByTestId('trace-download'))

        await waitFor(() => expect(launch).toHaveBeenCalledTimes(1))
        expect(launchRequest()).toMatchObject({ fromModule: 'Main', advanced: true, download: true })
    })

    it('locks the trace to the module while the project is loading', async () => {
        inputRead.mockResolvedValue(ruleTable)
        render(<TraceLaunchHost />)

        await open({ moduleOnlyLocked: true })

        expect(inputRead).toHaveBeenCalledWith('real-p1', 't1', { fromModule: 'Main' })
        const moduleOnly = await screen.findByTestId('launch-module-only')
        expect(moduleOnly).toBeChecked()
        expect(moduleOnly).toBeDisabled()
        await userEvent.click(screen.getByTestId('trace-start'))
        await waitFor(() => expect(launchRequest()).toMatchObject({ fromModule: 'Main' }))
    })

    it('traces the picked case of a test table; the first one is picked at first', async () => {
        inputRead.mockResolvedValue(testTable)
        render(<TraceLaunchHost />)

        await open()
        await screen.findByTestId('test-cases')
        expect(casesRead).toHaveBeenCalledWith('real-p1', 't1', { page: 0, size: 25 })
        await userEvent.click(screen.getByTestId('pick-case-2'))
        await userEvent.click(screen.getByTestId('trace-start'))

        await waitFor(() => expect(launch).toHaveBeenCalledTimes(1))
        expect(launchRequest()).toMatchObject({ testRanges: '2' })
        expect(launchRequest()['inputJson']).toBeUndefined()
    })

    it('reads the next page of cases and keeps the case already picked', async () => {
        inputRead.mockResolvedValue(testTable)
        casesRead.mockResolvedValue({ total: 120, content: casesPage.content })
        render(<TraceLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTitle('2'))

        await waitFor(() => expect(casesRead).toHaveBeenLastCalledWith('real-p1', 't1', { page: 1, size: 25 }))
        await userEvent.click(screen.getByTestId('trace-start'))
        await waitFor(() => expect(launchRequest()).toMatchObject({ testRanges: '1' }))
    })

    it('asks for a case when the table has none to pick', async () => {
        inputRead.mockResolvedValue(testTable)
        casesRead.mockResolvedValue({ total: 0, content: []})
        render(<TraceLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('trace-start'))

        expect(await screen.findByTestId('launch-error')).toHaveTextContent('testCases.noCase')
        expect(launch).not.toHaveBeenCalled()
    })

    it('reads the values a page leaves out within the module the table was read in', async () => {
        caseRead.mockResolvedValue({ id: '1', parameters: [{ name: 'car', description: 'Car', lazy: false, value: { vin: 'X' } }]})
        inputRead.mockResolvedValue(testTable)
        casesRead.mockResolvedValue({ total: 1, content: [{ id: '1', parameters: [{ name: 'car', description: 'Car', lazy: true }]}]})
        render(<TraceLaunchHost />)

        await open({ moduleOnlyLocked: true })
        await userEvent.click(await screen.findByTestId('load-case-1-0'))

        expect(casesRead).toHaveBeenCalledWith('real-p1', 't1', { fromModule: 'Main', page: 0, size: 25 })
        expect(caseRead).toHaveBeenCalledWith('real-p1', 't1', '1', { fromModule: 'Main' })
        expect(await screen.findByText('{1 fields}')).toBeInTheDocument()
    })

    it('still asks a table that takes nothing, since the panel carries more than its parameters', async () => {
        // The API leaves an empty parameter list out altogether.
        const { parameters: _parameters, ...bare } = ruleTable
        inputRead.mockResolvedValue(bare)
        render(<TraceLaunchHost />)

        await open()

        // The Editor opened this panel whatever the table took: the settings and the trace into a file are here.
        expect(await screen.findByTestId('trace-start')).toBeInTheDocument()
        expect(launch).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('trace-start'))

        await waitFor(() => expect(launch).toHaveBeenCalledTimes(1))
        expect(launchRequest()).toMatchObject({ projectId: 'real-p1' })
        // A table that declares nothing is traced with an empty set of parameters, which is what it takes.
        expect(JSON.parse(launchRequest()['inputJson'] as string)).toEqual({ params: {} })
    })

    it('keeps the launcher open with the reason when the trace cannot start', async () => {
        inputRead.mockResolvedValue(ruleTable)
        launch.mockRejectedValueOnce(new Error('compilation in progress'))
        render(<TraceLaunchHost />)

        await open()
        await userEvent.click(await screen.findByTestId('trace-start'))

        expect(await screen.findByTestId('launch-error')).toHaveTextContent('compilation in progress')
        expect(screen.getByTestId('trace-start')).toBeInTheDocument()
    })
})
